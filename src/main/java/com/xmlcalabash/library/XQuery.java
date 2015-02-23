/*
 * XQuery.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * Portions Copyright 2007 Sun Microsystems, Inc.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://xproc.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */

package com.xmlcalabash.library;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.s9api.*;
import net.sf.saxon.Configuration;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.CollectionResolver;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;

/**
 *
 * @author ndw
 */
public class XQuery extends DefaultStep {
    private static final QName _content_type = new QName("content-type");

    private ReadablePipe source = null;
    private Hashtable<QName,RuntimeValue> params = new Hashtable<QName,RuntimeValue> ();
    private ReadablePipe query = null;
    private WritablePipe result = null;
    
    public XQuery(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else if ("query".equals(port)) {
            query = pipe;
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value);
    }

    public void reset() {
        query.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        // FIXME: Deal with (doubly) escaped markup correctly...

        Vector<XdmNode> defaultCollection = new Vector<XdmNode> ();
        while (source.moreDocuments()) {
            defaultCollection.add(source.read());
        }

        XdmNode document = null;
        if (defaultCollection.size() > 0) {
            document = defaultCollection.firstElement();
        }

        XdmNode root = S9apiUtils.getDocumentElement(query.read());
        String queryString = null;

        if ((XProcConstants.c_data.equals(root.getNodeName())
             && "application/octet-stream".equals(root.getAttributeValue(_content_type)))
            || "base64".equals(root.getAttributeValue(_encoding))) {
            byte[] decoded = Base64.decode(root.getStringValue());
            queryString = new String(decoded);
        } else {
            queryString = root.getStringValue();
        }

        Configuration config = runtime.getProcessor().getUnderlyingConfiguration();

        runtime.getConfigurer().getSaxonConfigurer().configXQuery(config);

        CollectionURIResolver collectionResolver = config.getCollectionURIResolver();

        config.setCollectionURIResolver(new CollectionResolver(runtime, defaultCollection, collectionResolver));

        try {
            Processor qtproc = runtime.getProcessor();
            XQueryCompiler xqcomp = qtproc.newXQueryCompiler();
            xqcomp.setBaseURI(root.getBaseURI());
            xqcomp.setModuleURIResolver(runtime.getResolver());
            XQueryExecutable xqexec = xqcomp.compile(queryString);
            XQueryEvaluator xqeval = xqexec.load();
            if (document != null) {
                xqeval.setContextItem(document);
            }

            for (QName name : params.keySet()) {
                RuntimeValue v = params.get(name);
                if (runtime.getAllowGeneralExpressions()) {
                    xqeval.setExternalVariable(name, v.getValue());
                } else {
                    xqeval.setExternalVariable(name, new XdmAtomicValue(v.getString()));
                }

            }

            Iterator<XdmItem> iter = xqeval.iterator();
            while (iter.hasNext()) {
                XdmItem item = iter.next();
                if (item.isAtomicValue()) {
                    throw new XProcException(step.getNode(), "Not expecting atomic values back from XQuery!");
                }
                XdmNode node = (XdmNode) item;

                if (node.getNodeKind() != XdmNodeKind.DOCUMENT) {
                    // Make a document for this node...is this the right thing to do?
                    TreeWriter treeWriter = new TreeWriter(runtime);
                    treeWriter.startDocument(step.getNode().getBaseURI());
                    treeWriter.addSubtree(node);
                    treeWriter.endDocument();
                    node = treeWriter.getResult();
                }

                result.write(node);
            }
        } finally {
            config.setCollectionURIResolver(collectionResolver);
        }
    }
}
