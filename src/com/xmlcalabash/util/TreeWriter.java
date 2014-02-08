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

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import net.sf.saxon.Controller;
import net.sf.saxon.event.NamespaceReducer;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.*;
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
import java.util.Vector;

/**
 *
 * @author ndw
 */
public class TreeWriter {
    protected static final String logger = "com.xmlcalabash.util";
    protected Controller controller = null;
    protected XProcRuntime runtime = null;
    protected Executable exec = null;
    protected NamePool pool = null;
    protected XdmDestination destination = null;
    protected Receiver receiver = null;
    protected XProcLocationProvider xLocationProvider = null;
    protected boolean seenRoot = false;
    protected boolean inDocument = false;

    /**
     * Creates a new instance of ProcessMatch
     */
    public TreeWriter(XProcRuntime xproc) {
        runtime = xproc;
        controller = new Controller(runtime.getProcessor().getUnderlyingConfiguration());
        pool = controller.getNamePool();
        xLocationProvider = new XProcLocationProvider();
    }

    public TreeWriter(Processor proc) {
        controller = new Controller(proc.getUnderlyingConfiguration());
        pool = controller.getNamePool();
        xLocationProvider = new XProcLocationProvider();

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
            receiver = new NamespaceReducer(receiver);
            
            PipelineConfiguration pipe = controller.makePipelineConfiguration();
            pipe.setLocationProvider(xLocationProvider);
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
        
        receiver.setSystemId(overrideBaseURI.toASCIIString());
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
        int locId;
        String sysId = receiver.getSystemId();
        if (sysId == null) {
            locId = 0;
        } else {
            locId = xLocationProvider.allocateLocation(sysId);
        }

        try {
            receiver.startElement(elemName, typeCode, locId, 0);
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
        NodeName attrName = new NameOfNode(inode);
        SimpleType typeCode = (SimpleType) inode.getSchemaType();

        try {
            receiver.attribute(attrName, typeCode, newValue, 0, 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addAttribute(NodeName elemName, SimpleType typeCode, String newValue) {
        try {
            receiver.attribute(elemName, typeCode, newValue, 0, 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addAttribute(QName attrName, String newValue) {
        NodeName elemName = new FingerprintedQName(attrName.getPrefix(),attrName.getNamespaceURI(),attrName.getLocalName());
        SimpleType typeCode = (SimpleType) BuiltInType.getSchemaType(StandardNames.XS_UNTYPED_ATOMIC);
        try {
            receiver.attribute(elemName, typeCode, newValue, 0, 0);
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
            receiver.comment(comment, 0, 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addText(String text) {
        try {
            receiver.characters(text, 0, 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addPI(String target, String data) {
        try {
            receiver.processingInstruction(target, data, 0, 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }
}

