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

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.ProcessMatchingNodes;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Untyped;

import javax.xml.XMLConstants;
import java.util.ArrayList;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:namespace-rename",
        type = "{http://www.w3.org/ns/xproc}namespace-rename")

public class NamespaceRename extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _from = new QName("from");
    private static final QName _to = new QName("to");
    private static final QName _apply_to = new QName("apply-to");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private NamespaceUri from = null;
    private NamespaceUri to = null;
    private String applyTo = null;

    /* Creates a new instance of NamespaceRename */
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
            from = NamespaceUri.of(getOption(_from).getString());
        } else {
            from = NamespaceUri.NULL;
        }

        if (getOption(_to) != null) {
            to = NamespaceUri.of(getOption(_to).getString());
        } else {
            to = NamespaceUri.NULL;
        }

        applyTo = getOption(_apply_to, "all");

        if (from == XProcConstants.NS_XML || to == XProcConstants.NS_XML
            || from == XProcConstants.NS_XML_ATTR || to == XProcConstants.NS_XML_ATTR) {
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
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }
    }

    public boolean processStartDocument(XdmNode node) {
        return true;
    }

    public void processEndDocument(XdmNode node) {
        matcher.addEndElement();
    }

    @Override
    public AttributeMap processAttributes(XdmNode node, AttributeMap matchingAttributes, AttributeMap nonMatchingAttributes) {
        throw new UnsupportedOperationException("processAttribute can't be called in NamespaceRename--but it was!?");
    }

    @Override
    public boolean processStartElement(XdmNode node, AttributeMap attributes) {
        NodeInfo inode = node.getUnderlyingNode();
        NamespaceMap nsmap = inode.getAllNamespaces();
        NamespaceMap newNS = inode.getAllNamespaces();

        // This code had to be competely rewritten for the Saxon 10 API

        NodeName startName = NameOfNode.makeName(inode);
        SchemaType startType = inode.getSchemaType();
        AttributeMap startAttr = inode.attributes();

        if (!"attributes".equals(applyTo)) {
            if (from == node.getNodeName().getNamespaceUri()) {
                String prefix = node.getNodeName().getPrefix();
                startName = new FingerprintedQName(prefix, to, node.getNodeName().getLocalName());
                startType = Untyped.INSTANCE;
                newNS = newNS.remove(prefix);
                newNS = newNS.put(prefix, to);
            }
        }

        if ("all".equals(applyTo)) {
            for (AttributeInfo attr : inode.attributes()) {
                NodeName nameCode = attr.getNodeName();
                SimpleType atype = attr.getType();
                NamespaceUri uri = NamespaceUri.of(nameCode.getURI());

                if (from == uri) {
                    startAttr = startAttr.remove(nameCode);

                    String pfx = nameCode.getPrefix();
                    newNS = newNS.remove(pfx);

                    nameCode = new FingerprintedQName(pfx, to, nameCode.getLocalPart());
                    atype = BuiltInAtomicType.UNTYPED_ATOMIC;
                    newNS = newNS.put(pfx, to);
                }

                startAttr = startAttr.put(new AttributeInfo(nameCode, atype, attr.getValue(), attr.getLocation(), ReceiverOption.NONE));
            }
        }

        if ("elements".equals(applyTo)) {
            for (AttributeInfo attr : inode.attributes()) {
                NodeName nameCode = attr.getNodeName();
                SimpleType atype = attr.getType();

                NamespaceUri uri = NamespaceUri.of(nameCode.getURI());

                if (from == uri) {
                    startAttr = startAttr.remove(nameCode);
                    String pfx = prefixFor(newNS, from);
                    nameCode = new FingerprintedQName(pfx, from, nameCode.getLocalPart());
                    atype = BuiltInAtomicType.UNTYPED_ATOMIC;
                    newNS = newNS.put(pfx, from);
                }

                startAttr = startAttr.put(new AttributeInfo(nameCode, atype, attr.getValue(), attr.getLocation(), ReceiverOption.NONE));
            }
        }

        if ("attributes".equals(applyTo)) {
            for (AttributeInfo attr : inode.attributes()) {
                NodeName nameCode = attr.getNodeName();
                startAttr = startAttr.remove(nameCode);

                String pfx = nameCode.getPrefix();
                NamespaceUri uri = NamespaceUri.of(nameCode.getURI());

                if (from == uri) {
                    pfx = prefixFor(newNS, to);
                    nameCode = new FingerprintedQName(pfx,to,nameCode.getLocalPart());
                    newNS = newNS.put(pfx, to);
                }

                startAttr = startAttr.put(new AttributeInfo(nameCode, BuiltInAtomicType.UNTYPED_ATOMIC, attr.getValue(), attr.getLocation(), ReceiverOption.NONE));
            }
        }

        /*
        System.err.println("{"+startName.getURI()+"}"+startName.getPrefix()+":"+startName.getLocalPart());
        for (NamespaceBinding b : newNS) {
            System.err.println(b.getPrefix()+"="+b.getURI());
        }
        System.err.println("---");
        for (AttributeInfo a : startAttr) {
            System.err.println("{"+a.getNodeName().getURI()+"}"+a.getNodeName().getPrefix()+":"+a.getNodeName().getLocalPart());
        }
         */

        matcher.addStartElement(startName, startAttr, startType, newNS);
        return true;
    }

    public void processEndElement(XdmNode node) {
        matcher.addEndElement();
    }

    public void processText(XdmNode node) {
        matcher.addText(node.getStringValue());
    }

    public void processComment(XdmNode node) {
        matcher.addComment(node.getStringValue());
    }

    public void processPI(XdmNode node) {
        matcher.addPI(node.getNodeName().getLocalName(), node.getStringValue());
    }
}

