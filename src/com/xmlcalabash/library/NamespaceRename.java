/*
 * NamespaceRename.java
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

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.*;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NamePool;

import javax.xml.XMLConstants;

import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */
public class NamespaceRename extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _from = new QName("from");
    private static final QName _to = new QName("to");
    private static final QName _apply_to = new QName("apply-to");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private String from = null;
    private String to = null;
    private String applyTo = null;

    /** Creates a new instance of NamespaceRename */
    public NamespaceRename(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        if (getOption(_from) != null) {
            from = getOption(_from).getString();
        } else {
            from = "";
        }

        if (getOption(_to) != null) {
            to = getOption(_to).getString();
        } else {
            to = "";
        }

        applyTo = getOption(_apply_to, "all");

        if (XMLConstants.XML_NS_URI.equals(from) || XMLConstants.XML_NS_URI.equals(to)
                || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(from) || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(to)) {
            throw XProcException.stepError(14);
        }

        if (from.equals(to)) {
            result.write(source.read());
        } else {
            matcher = new ProcessMatch(runtime, this);
            matcher.match(source.read(), new RuntimeValue("*", step.getNode()));
            result.write(matcher.getResult());
        }

        if (source.moreDocuments()) {
            throw XProcException.dynamicError(6);
        }
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        NodeInfo inode = node.getUnderlyingNode();
        NamePool pool = inode.getNamePool();
        int inscopeNS[] = inode.getDeclaredNamespaces(null);
        int newNS[] = null;
        int nameCode = inode.getNameCode();
        int typeCode = inode.getTypeAnnotation() & NamePool.FP_MASK;

        if ("attributes".equals(applyTo)) {
            matcher.addStartElement(nameCode, typeCode, inscopeNS);
        } else {
            if (inscopeNS.length > 0) {
                newNS = new int[inscopeNS.length];
                for (int pos = 0; pos < inscopeNS.length; pos++) {
                    int ns = inscopeNS[pos];
                    String pfx = pool.getPrefixFromNamespaceCode(ns);
                    String uri = pool.getURIFromNamespaceCode(ns);
                    if (from.equals(uri)) {
                        int newns = pool.getNamespaceCode(pfx,to);
                        if (newns < 0) {
                            newns = pool.allocateNamespaceCode(pfx,to);
                        }
                        newNS[pos] = newns;
                    } else {
                        newNS[pos] = ns;
                    }
                }
            }

            // Careful, we're messing with the namespace bindings
            // Make sure the nameCode is right...
            String pfx = pool.getPrefix(nameCode);
            String uri = pool.getURI(nameCode);

            if (from.equals(uri)) {
                nameCode = pool.allocate(pfx,to,node.getNodeName().getLocalName());
            }

            matcher.addStartElement(nameCode, typeCode, newNS);
        }

        if (!"elements".equals(applyTo)) {
            XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
            while (iter.hasNext()) {
                XdmNode attr = (XdmNode) iter.next();
                inode = attr.getUnderlyingNode();
                pool = inode.getNamePool();
                nameCode = inode.getNameCode();
                typeCode = inode.getTypeAnnotation() & NamePool.FP_MASK;
                String pfx = pool.getPrefix(nameCode);
                String uri = pool.getURI(nameCode);

                if (from.equals(uri)) {
                    if ("".equals(pfx)) {
                        pfx = "_1";
                    }
                    nameCode = pool.allocate(pfx,to,attr.getNodeName().getLocalName());
                }
                matcher.addAttribute(nameCode, typeCode, attr.getStringValue());
            }
        } else {
            matcher.addAttributes(node);
        }

        return true;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
    }

    public void processText(XdmNode node) throws SaxonApiException {
        matcher.addText(node.getStringValue());
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        matcher.addComment(node.getStringValue());
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        matcher.addPI(node.getNodeName().getLocalName(), node.getStringValue());
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("processAttribute can't be called in NamespaceRename--but it was!?");
    }
}

