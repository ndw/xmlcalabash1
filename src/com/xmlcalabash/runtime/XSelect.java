package com.xmlcalabash.runtime;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.DocumentSequence;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.model.NamespaceBinding;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.RuntimeValue;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.saxon.s9api.*;
import net.sf.saxon.sxpath.IndependentContext;

/**
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
 * https://runtime.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 *
 * Ideally, I'd like this code to perform the selections in a lazy fashion, but that's
 * hard because it has to be possible to answer questions about how many documents
 * will be returned. So for now, I'm just doing it all up front.
 *
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 10, 2008
 * Time: 10:13:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class XSelect implements ReadablePipe {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private ReadablePipe source = null;
    private String select = null;
    private XdmNode context = null;
    private DocumentSequence documents = null;
    private XPathSelector selector = null;
    private XProcRuntime runtime = null;
    private boolean initialized = false;
    private int docindex = 0;
    private Step reader = null;
    private XStep forStep = null;

    /** Creates a new instance of Select */
    public XSelect(XProcRuntime runtime, XStep forStep, ReadablePipe readFrom, String xpathExpr, XdmNode context) {
        source = readFrom;
        select = xpathExpr;
        this.runtime = runtime;
        this.context = context;
        documents = new DocumentSequence(runtime);
        this.forStep = forStep;
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

            IndependentContext icontext = (IndependentContext) xcomp.getUnderlyingStaticContext();

            Hashtable<QName, RuntimeValue> inScopeOptions = new Hashtable<QName, RuntimeValue> ();
            try {
                inScopeOptions = ((XCompoundStep) forStep).getInScopeOptions();
            } catch (ClassCastException cce) {
                // FIXME: Surely there's a better way to do this!!!
            }
            
            Hashtable<QName, RuntimeValue> boundOpts = new Hashtable<QName, RuntimeValue> ();
            for (QName name : inScopeOptions.keySet()) {
                RuntimeValue v = inScopeOptions.get(name);
                if (v.initialized()) {
                    boundOpts.put(name, v);
                }
            }

            for (QName varname : boundOpts.keySet()) {
                xcomp.declareVariable(varname);
            }

            for (String prefix : bindings.getNamespaceBindings().keySet()) {
                xcomp.declareNamespace(prefix, bindings.getNamespaceBindings().get(prefix));
            }

            XPathExecutable xexec = xcomp.compile(select);
            selector = xexec.load();

            for (QName varname : boundOpts.keySet()) {
                XdmAtomicValue avalue = boundOpts.get(varname).getUntypedAtomic(runtime);
                selector.setVariable(varname,avalue);
            }

        } catch (SaxonApiException sae) {
            if (S9apiUtils.xpathSyntaxError(sae)) {
                throw XProcException.dynamicError(23, context, "Invalid XPath expression: '" + select + "'.");
            } else {
                throw new XProcException(sae);
            }
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
                        if ((node.getNodeKind() != XdmNodeKind.ELEMENT)
                            && (node.getNodeKind() != XdmNodeKind.DOCUMENT)) {
                            throw XProcException.dynamicError(16);
                        }
                    } catch (ClassCastException cce) {
                        throw XProcException.dynamicError(16);
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
        source.resetReader();
        documents.reset();
        initialized = false;
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

    public String toString() {
        return "xselect " + documents;
    }
}
