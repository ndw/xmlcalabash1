/*
 * Select.java
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

package com.xmlcalabash.io;

import net.sf.saxon.s9api.*;

import java.util.Iterator;
import java.util.logging.Logger;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.NamespaceBinding;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.util.S9apiUtils;

/**
 *
 * Ideally, I'd like this code to perform the selections in a lazy fashion, but that's
 * hard because it has to be possible to answer questions about how many documents
 * will be returned. So for now, I'm just doing it all up front.
 *
 * @author ndw
 */
public class Select implements ReadablePipe {
    private ReadablePipe source = null;
    private String select = null;
    private XdmNode context = null;
    private DocumentSequence documents = null;
    private XPathSelector selector = null;
    private XProcRuntime runtime = null;
    private boolean initialized = false;
    private int docindex = 0;
    private Step reader = null;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /** Creates a new instance of Select */
    public Select(XProcRuntime runtime, ReadablePipe readFrom, String xpathExpr, XdmNode xpathContext) {
        source = readFrom;
        select = xpathExpr;
        context = xpathContext;
        this.runtime = runtime;
        documents = new DocumentSequence(runtime);
    }

    public void canReadSequence(boolean sequence) {
        // nop; always true
    }

    public boolean readSequence() {
        return true;
    }
    
    private void readSource() {
        initialized = true;
        try {
            NamespaceBinding bindings = new NamespaceBinding(runtime,context);
            XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
            xcomp.setBaseURI(context.getBaseURI());
            for (String prefix : bindings.getNamespaceBindings().keySet()) {
                xcomp.declareNamespace(prefix, bindings.getNamespaceBindings().get(prefix));
            }

            XPathExecutable xexec = xcomp.compile(select);
            selector = xexec.load();
            // FIXME: Set getVariables
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        while (source.moreDocuments()) {
            // Ok, time to go looking for things to select from.
            try {
                XdmNode doc = source.read();

                if (reader != null) {
                    runtime.finest(null, reader.getNode(), reader.getName() + " select read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + source);
                }

                selector.setContextItem(doc);

                Iterator<XdmItem> iter = selector.iterator();
                while (iter.hasNext()) {
                    XdmItem item = iter.next();
                    XdmNode node = null;
                    try {
                        node = (XdmNode) item;
                    } catch (ClassCastException cce) {
                        throw new XProcException (context, "Select matched non-node!?");
                    }
                    XdmDestination dest = new XdmDestination();
                    S9apiUtils.writeXdmValue(runtime, node, dest, node.getBaseURI());

                    XdmNode sdoc = dest.getXdmNode();

                    if (reader != null) {
                        runtime.finest(null, reader.getNode(), reader.getName() + " select wrote '" + (sdoc == null ? "null" : sdoc.getBaseURI()) + "' to " + documents);
                    }

                    documents.add(sdoc);
                }
            } catch (SaxonApiException sae) {
                throw new XProcException(sae);
            }
        }
    }
    
    public void resetReader() {
        docindex = 0;
    }
    
    public boolean moreDocuments() {
        if (!initialized) {
            readSource();
        }
        return docindex < documents.size();
    }

    public boolean closed() {
        return true;
    }

    public int documentCount() {
        if (!initialized) {
            readSource();
        }
        return documents.size();
    }

    public DocumentSequence documents() {
        return documents;
    }

    public void setReader(Step step) {
        reader = step;
    }

    public XdmNode read () throws SaxonApiException {
        if (!initialized) {
            readSource();
        }

        XdmNode doc = null;
        if (moreDocuments()) {
            doc = documents.get(docindex++);
        }

        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }

        return doc;
    }
}
