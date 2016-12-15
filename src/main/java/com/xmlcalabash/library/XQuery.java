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

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.XProcCollectionFinder;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.CollectionFinder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:xquery",
        type = "{http://www.w3.org/ns/xproc}xquery")

public class XQuery extends DefaultStep {
    private static final QName _content_type = new QName("content-type");
    private static final QName cx_decode = new QName("cx", XProcConstants.NS_CALABASH_EX, "decode");

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

        CollectionFinder collectionFinder = config.getCollectionFinder();

        config.setDefaultCollection(XProcCollectionFinder.DEFAULT);
        config.setCollectionFinder(new XProcCollectionFinder(runtime, defaultCollection, collectionFinder));

        try {
            Processor qtproc = runtime.getProcessor();
            XQueryCompiler xqcomp = qtproc.newXQueryCompiler();
            xqcomp.setBaseURI(root.getBaseURI());

            if (xqcomp.getModuleURIResolver() == null) {
                xqcomp.setModuleURIResolver(runtime.getResolver());
            }

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
                    xqeval.setExternalVariable(name, v.getUntypedAtomic(runtime));
                }

            }

            Iterator<XdmItem> iter = xqeval.iterator();
            while (iter.hasNext()) {
                XdmItem item = iter.next();
                XdmNode node = null;

                if (item.isAtomicValue()) {
                    if (runtime.getAllowTextResults()) {
                        TreeWriter tree = new TreeWriter(runtime);
                        tree.startDocument(step.getNode().getBaseURI());
                        tree.addStartElement(XProcConstants.c_result);
                        tree.addAttribute(_content_type, "text/plain");
                        tree.addAttribute(cx_decode,"true");
                        tree.startContent();
                        tree.addText(item.getStringValue());
                        tree.addEndElement();
                        tree.endDocument();
                        node = tree.getResult();
                    } else {
                        throw new XProcException(step.getNode(), "p:xquery returned atomic value");
                    }
                } else {
                    node = (XdmNode) item;

                    // If the document isn't well-formed XML, encode it as text
                    try {
                        S9apiUtils.assertDocument(node);
                    } catch (XProcException e) {
                        // If the document isn't well-formed XML, encode it as text
                        if (runtime.getAllowTextResults()) {
                            // Document is apparently not well-formed XML.
                            TreeWriter tree = new TreeWriter(runtime);
                            tree.startDocument(step.getNode().getBaseURI());
                            tree.addStartElement(XProcConstants.c_result);
                            tree.addAttribute(_content_type, "text/plain");
                            tree.addAttribute(cx_decode,"true");
                            tree.startContent();
                            tree.addText(node.toString());
                            tree.addEndElement();
                            tree.endDocument();
                            node = tree.getResult();
                        } else {
                            throw new XProcException(step.getStep(), "p:xquery returned non-XML result", e.getCause());
                        }
                    }
                }

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
            config.setCollectionFinder(collectionFinder);
        }
    }
}
