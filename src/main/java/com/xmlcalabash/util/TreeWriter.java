/*
 * TreeWriter.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.Controller;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NameOfNode;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.NamespaceIterator;
import net.sf.saxon.type.BuiltInType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;

import java.net.URI;
import java.util.Iterator;

/**
 *
 * @author ndw
 */

/* N.B. There's a fundamental problem in here somewhere. In order to preserve base URIs correctly
   when, for example, @xml:base attributes have been deleted. The tree walker has to reset the
   systemIdentifier in the receiver several times. This must have something to do with getting
   the right base URIs on the constructed nodes.

   Conversely, in the case where, for example, the file is coming from a p:http-request, the URI
   of the document entity received over the net is supposed to be the base URI of the document.
   But this "resetting" that takes place undoes the value set on the document node. I'm not sure
   how.

   So there's a hacked compromise in here: if the "overrideBaseURI" is the empty string, we ignore
   it. That seems to cover both cases.

   But I am not very confident.
 */

public class TreeWriter {
    protected static final String logger = "com.xmlcalabash.util";
    protected Controller controller = null;
    protected XProcRuntime runtime = null;
    protected Executable exec = null;
    protected NamePool pool = null;
    protected XdmDestination destination = null;
    protected Receiver receiver = null;
    protected boolean seenRoot = false;
    protected boolean inDocument = false;

    /*
     * Creates a new instance of ProcessMatch
     */
    public TreeWriter(XProcRuntime xproc) {
        runtime = xproc;
        pool = xproc.getProcessor().getUnderlyingConfiguration().getNamePool();
        controller = new Controller(runtime.getProcessor().getUnderlyingConfiguration());
    }

    public TreeWriter(Processor proc) {
        pool = proc.getUnderlyingConfiguration().getNamePool();
        controller = new Controller(proc.getUnderlyingConfiguration());
    }

    public XdmNode getResult() {
        return destination.getXdmNode();
    }

    public boolean inDocument() {
        return inDocument;
    }

    public void startDocument(URI baseURI) {
        inDocument = true;
        seenRoot = false;
        try {
            exec = new Executable(controller.getConfiguration());
            destination = new XdmDestination();
            receiver = destination.getReceiver(controller.getConfiguration());

            PipelineConfiguration pipe = controller.makePipelineConfiguration();
            receiver.setPipelineConfiguration(pipe);

            if (baseURI != null) {
                receiver.setSystemId(baseURI.toASCIIString());
            } else {
                receiver.setSystemId("http://example.com/");
            }

            receiver.open();
            receiver.startDocument(0);
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    public void endDocument() {
        try {
            receiver.setSystemId("http://norman-was-here.com/");
            receiver.endDocument();
            receiver.close();
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addSubtree(XdmNode node) {
        if (node.getNodeKind() == XdmNodeKind.DOCUMENT) {
            writeChildren(node);
        } else if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
            addStartElement(node);
            XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
            while (iter.hasNext()) {
                XdmNode child = (XdmNode) iter.next();
                addAttribute(child, child.getStringValue());
            }
            try {
                receiver.startContent();
            } catch (XPathException xe) {
                throw new XProcException(xe);
            }
            writeChildren(node);
            addEndElement();
        } else if (node.getNodeKind() == XdmNodeKind.COMMENT) {
            addComment(node.getStringValue());
        } else if (node.getNodeKind() == XdmNodeKind.TEXT) {
            addText(node.getStringValue());
        } else if (node.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION) {
            addPI(node.getNodeName().getLocalName(), node.getStringValue());
        } else {
            throw new UnsupportedOperationException("Unexpected node type");
        }
    }

    protected void writeChildren(XdmNode node) {
        XdmSequenceIterator iter = node.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            addSubtree(child);
        }
    }

    public void addStartElement(XdmNode node) {
        addStartElement(node, node.getNodeName(), node.getBaseURI());
    }

    public void addStartElement(XdmNode node, URI overrideBaseURI) {
        addStartElement(node, node.getNodeName(), overrideBaseURI);
    }

    public void addStartElement(XdmNode node, QName newName) {
        addStartElement(node, newName, node.getBaseURI());
    }

    public void addStartElement(XdmNode node, QName newName, URI overrideBaseURI) {
        NodeInfo inode = node.getUnderlyingNode();

        NamespaceBinding inscopeNS[] = null;
        if (seenRoot) {
            inscopeNS = inode.getDeclaredNamespaces(null);
        } else {
            int count = 0;
            Iterator<NamespaceBinding> nsiter = NamespaceIterator.iterateNamespaces(inode);
            while (nsiter.hasNext()) {
                count++;
                nsiter.next();
            }
            inscopeNS = new NamespaceBinding[count];
            nsiter = NamespaceIterator.iterateNamespaces(inode);
            count = 0;
            while (nsiter.hasNext()) {
                inscopeNS[count] = nsiter.next();
                count++;
            }
            seenRoot = true;
        }

        // If the newName has no prefix, then make sure we don't pass along some other
        // binding for the default namespace...
        if ("".equals(newName.getPrefix())) {
            int newLen = 0;
            for (int pos = 0; pos < inscopeNS.length; pos++) {
                NamespaceBinding nscode = inscopeNS[pos];
                if (!"".equals(nscode.getPrefix())) {
                    newLen++;
                }
            }
            if (newLen != inscopeNS.length) {
                NamespaceBinding newCodes[] = new NamespaceBinding[newLen];
                int npos = 0;
                for (int pos = 0; pos < inscopeNS.length; pos++) {
                    NamespaceBinding nscode = inscopeNS[pos];
                    if (!"".equals(nscode.getPrefix())) {
                        newCodes[npos++] = nscode;
                    }
                }
                inscopeNS = newCodes;
            }
        }

        // Hack. See comment at top of file
        if (!"".equals(overrideBaseURI.toASCIIString())) {
            receiver.setSystemId(overrideBaseURI.toASCIIString());
        }

        FingerprintedQName newNameOfNode = new FingerprintedQName(newName.getPrefix(),newName.getNamespaceURI(),newName.getLocalName());
        addStartElement(newNameOfNode, inode.getSchemaType(), inscopeNS);
    }

    public void addStartElement(QName newName) {
        NodeName elemName = new FingerprintedQName(newName.getPrefix(), newName.getNamespaceURI(), newName.getLocalName());
        SchemaType typeCode = BuiltInType.getSchemaType(StandardNames.XS_UNTYPED);
        NamespaceBinding inscopeNS[] = null;
        addStartElement(elemName, typeCode, inscopeNS);
    }

    public void addStartElement(NodeName elemName, SchemaType typeCode, NamespaceBinding nscodes[]) {
        Location loc;
        String sysId = receiver.getSystemId();
        if (sysId == null) {
            loc = VoidLocation.instance();
        } else {
            loc = new SysIdLocation(sysId);
        }

        try {
            receiver.startElement(elemName, typeCode, loc, 0);
            if (nscodes != null) {
                for (NamespaceBinding ns : nscodes) {
                    receiver.namespace(ns, 0);
                }
            }
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addNamespace(String prefix, String uri) {
        NamespaceBinding nsbind = new NamespaceBinding(prefix, uri);
        try {
            receiver.namespace(nsbind, 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addAttributes(XdmNode element) {
        XdmSequenceIterator iter = element.axisIterator(Axis.ATTRIBUTE);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            addAttribute(child);
        }
    }

    public void addAttribute(XdmNode xdmattr) {
        addAttribute(xdmattr, xdmattr.getStringValue());
    }

    public void addAttribute(XdmNode xdmattr, String newValue) {
        NodeInfo inode = xdmattr.getUnderlyingNode();
        NodeName attrName = NameOfNode.makeName(inode);
        SimpleType typeCode = (SimpleType) inode.getSchemaType();

        try {
            receiver.attribute(attrName, typeCode, newValue, VoidLocation.instance(), 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addAttribute(NodeName elemName, SimpleType typeCode, String newValue) {
        try {
            receiver.attribute(elemName, typeCode, newValue, VoidLocation.instance(), 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addAttribute(QName attrName, String newValue) {
        NodeName elemName = new FingerprintedQName(attrName.getPrefix(),attrName.getNamespaceURI(),attrName.getLocalName());
        SimpleType typeCode = (SimpleType) BuiltInType.getSchemaType(StandardNames.XS_UNTYPED_ATOMIC);
        try {
            receiver.attribute(elemName, typeCode, newValue, VoidLocation.instance(), 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void startContent() {
        try {
            receiver.startContent();
        } catch (XPathException xe) {
            throw new XProcException(xe);
        }
    }
    
    public void addEndElement() {
        try {
            receiver.endElement();
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }


    public void addComment(String comment) {
        try {
            receiver.comment(comment, VoidLocation.instance(), 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addText(String text) {
        try {
            receiver.characters(text, VoidLocation.instance(), 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addPI(String target, String data) {
        try {
            receiver.processingInstruction(target, data, VoidLocation.instance(), 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }
}

