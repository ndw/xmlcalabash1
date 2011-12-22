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
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NameOfNode;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NamePool;
import javax.xml.XMLConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.type.SimpleType;

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
        NamespaceBinding inscopeNS[] = inode.getDeclaredNamespaces(null);
        NamespaceBinding newNS[] = null;

        if ("attributes".equals(applyTo)) {
            matcher.addStartElement(new NameOfNode(inode), inode.getSchemaType(), inscopeNS);
        } else {
            if (inscopeNS.length > 0) {
                int countNS = 0;

                for (int pos = 0; pos < inscopeNS.length; pos++) {
                    NamespaceBinding ns = inscopeNS[pos];
                    String uri = ns.getURI();
                    if (!from.equals(uri) || !"".equals(to)) {
                        countNS++;
                    }
                }

                newNS = new NamespaceBinding[countNS];
                int newPos = 0;
                for (int pos = 0; pos < inscopeNS.length; pos++) {
                    NamespaceBinding ns = inscopeNS[pos];
                    String pfx = ns.getPrefix();
                    String uri = ns.getURI();
                    if (from.equals(uri)) {
                        if ("".equals(to)) {
                            // Nevermind, we're throwing the namespace away
                        } else {
                            NamespaceBinding newns = new NamespaceBinding(pfx,to);
                            newNS[newPos++] = newns;
                        }
                    } else {
                        newNS[newPos++] = ns;
                    }
                }
            }

            // Careful, we're messing with the namespace bindings
            // Make sure the nameCode is right...
            NodeName nameCode = new NameOfNode(inode);
            String pfx = nameCode.getPrefix();
            String uri = nameCode.getURI();

            if (from.equals(uri)) {
                if ("".equals(to)) {
                    pfx = "";
                }

                nameCode = new FingerprintedQName(pfx,to,nameCode.getLocalPart());
            }

            matcher.addStartElement(nameCode, inode.getSchemaType(), newNS);
        }

        if (!"elements".equals(applyTo)) {
            XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
            while (iter.hasNext()) {
                XdmNode attr = (XdmNode) iter.next();
                inode = attr.getUnderlyingNode();
                NodeName nameCode = new NameOfNode(inode);
                String pfx = nameCode.getPrefix();
                String uri = nameCode.getURI();

                if (from.equals(uri)) {
                    if ("".equals(pfx)) {
                        pfx = "_1";
                    }
                    nameCode = new FingerprintedQName(pfx,to,nameCode.getLocalPart());
                }
                matcher.addAttribute(nameCode, (SimpleType) inode.getSchemaType(), attr.getStringValue());
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

