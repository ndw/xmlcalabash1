/*
 * LabelElements.java
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

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.ProcessMatchingNodes;
import net.sf.saxon.om.InscopeNamespaceResolver;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author ndw
 */
public class Template extends DefaultStep implements ProcessMatchingNodes {
    private ReadablePipe source = null;
    private ReadablePipe template = null;
    private WritablePipe result = null;
    private Hashtable<QName,RuntimeValue> params = new Hashtable<QName, RuntimeValue> ();
    private ProcessMatch matcher = null;
    private XdmNode context = null;

    private static final int START = 0;
    private static final int XPATHMODE = 1;
    private static final int SQUOTEMODE = 2;
    private static final int DQUOTEMODE = 3;
    private static final int END = 4;

    /** Creates a new instance of LabelElements */
    public Template(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else if ("template".equals(port)) {
            template = pipe;
        } else {
            throw new UnsupportedOperationException("WTF?");
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value);
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
        template.resetReader();
    }

    public void run() throws SaxonApiException {
        if (step.getNode().getNodeName().equals(XProcConstants.p_document_template)) {
            runtime.finer(this, step.getNode(), "The template step should be named p:template, the name p:document-template is deprecated.");
        }

        super.run();

        if (source.documentCount() > 1) {
            throw XProcException.stepError(68);
        }

        context = source.read();

        matcher = new ProcessMatch(runtime, this);
        matcher.match(template.read(), new RuntimeValue("node()", step.getNode()));

        result.write(matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        matcher.startDocument(node.getBaseURI());
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        matcher.endDocument();
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        matcher.addStartElement(node);

        XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
        while (iter.hasNext()) {
            XdmNode attr = (XdmNode) iter.next();
            String value = attr.getStringValue();
            if (value.contains("{") || value.contains("}")) {
                Vector<XdmItem> items = parse(attr, value);
                String newvalue = "";
                for (XdmItem item : items) {
                    newvalue += item.getStringValue();
                }
                matcher.addAttribute(attr, newvalue);
            } else {
                matcher.addAttribute(attr);
            }
        }

        return true;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
    }

    public void processText(XdmNode node) throws SaxonApiException {
        String value = node.getStringValue();
        if (value.contains("{") || value.contains("}")) {
            Vector<XdmItem> items = parse(node, value);
            for (XdmItem item : items) {
                if (item.isAtomicValue()) {
                    matcher.addText(item.getStringValue());
                } else {
                    XdmNode nitem = (XdmNode) item;
                    switch (nitem.getNodeKind()) {
                        case ELEMENT:
                            matcher.addSubtree(nitem);
                            break;
                        case ATTRIBUTE:
                            matcher.addAttribute(nitem);
                            break;
                        case PROCESSING_INSTRUCTION:
                            matcher.addSubtree(nitem);
                            break;
                        case COMMENT:
                            matcher.addComment(nitem.getStringValue());
                            break;
                        default:
                            matcher.addText(nitem.getStringValue());
                    }
                }
            }
        } else {
            matcher.addText(value);
        }
    }

    private Vector<XdmItem> parse(XdmNode node, String value) {
        Vector<XdmItem> items = new Vector<XdmItem> ();
        int state = START;
        String ptext = "";
        String ch = "";

        Hashtable<String,String> nsbindings = new Hashtable<String,String> ();

        // FIXME: Surely there's a better way to do this?
        XdmNode parent = node;
        while (parent != null
                && parent.getNodeKind() != XdmNodeKind.ELEMENT
                && parent.getNodeKind() != XdmNodeKind.DOCUMENT) {
            parent = parent.getParent();
        }

        if (parent.getNodeKind() == XdmNodeKind.ELEMENT) {
            NodeInfo inode = parent.getUnderlyingNode();
            InscopeNamespaceResolver inscopeNS = new InscopeNamespaceResolver(inode);
            Iterator<?> prefixes = inscopeNS.iteratePrefixes();
            while (prefixes.hasNext()) {
                String nspfx = (String)prefixes.next();
                String nsuri = inscopeNS.getURIForPrefix(nspfx, true);
                nsbindings.put(nspfx,nsuri);
            }
        }

        String peek = "";
        int pos = 0;
        while (pos < value.length()) {
            ch = value.substring(pos,pos+1);

            switch (state) {
                case START:
                    if (pos+1 < value.length()) {
                        peek = value.substring(pos+1,pos+2);
                    } else {
                        peek = "";
                    }

                    if ("{".equals(ch)) {
                        if ("{".equals(peek)) {
                            ptext += "{";
                            pos++;
                        } else {
                            if (!"".equals(ptext)) {
                                items.add(new XdmAtomicValue(ptext));
                                ptext = "";
                            }
                            state = XPATHMODE;
                        }
                    } else if ("}".equals(ch)) {
                        if ("}".equals(peek)) {
                            ptext += "}";
                            pos++;
                        } else {
                            throw XProcException.stepError(67);
                        }
                    } else {
                        ptext += ch;
                    }
                    break;
                case XPATHMODE:
                    if ("{".equals(ch)) {
                        throw XProcException.stepError(67);
                    } else if ("'".equals(ch)) {
                        ptext += "'";
                        state = SQUOTEMODE;
                    } else if ("\"".equals(ch)) {
                        ptext += "\"";
                        state = DQUOTEMODE;
                    } else if ("}".equals(ch)) {
                        items.addAll(evaluateXPath(context, nsbindings, ptext, params));
                        ptext = "";
                        state = START;
                    } else {
                        ptext += ch;
                    }
                    break;
                case SQUOTEMODE:
                    if ("'".equals(ch)) {
                        ptext += "'";
                        state = XPATHMODE;
                    } else {
                        ptext += ch;
                    }
                    break;
                case DQUOTEMODE:
                    if (("\"").equals(ch)) {
                        ptext += "\"";
                        state = XPATHMODE;
                    } else {
                        ptext += ch;
                    }
                    break;
            }

            pos++;
        }

        if (state != START) {
            throw XProcException.stepError(67);
        }

        if (!"".equals(ptext)) {
            items.add(new XdmAtomicValue(ptext));
        }

        return items;
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        String value = node.getStringValue();
        if (value.contains("{") || value.contains("}")) {
            Vector<XdmItem> items = parse(node, value);
            String newvalue = "";
            for (XdmItem item : items) {
                newvalue += item.getStringValue();
            }
            matcher.addComment(newvalue);
        } else {
            matcher.addComment(value);
        }
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        String value = node.getStringValue();
        if (value.contains("{") || value.contains("}")) {
            Vector<XdmItem> items = parse(node, value);
            String newvalue = "";
            for (XdmItem item : items) {
                newvalue += item.getStringValue();
            }
            matcher.addPI(node.getNodeName().getLocalName(), newvalue);
        } else {
            matcher.addPI(node.getNodeName().getLocalName(), value);
        }
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("This can't happen.");
    }

}

