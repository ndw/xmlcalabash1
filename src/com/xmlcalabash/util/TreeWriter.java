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
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.s9api.*;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceIterator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.LocationMap;

import java.net.URI;

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
    protected boolean seenRoot = false;
    protected XProcLocationProvider xLocationProvider = null;

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

    public void startDocument(URI baseURI) {
        seenRoot = false;
        try {
            exec = new Executable(controller.getConfiguration());
            destination = new XdmDestination();
            receiver = destination.getReceiver(controller.getConfiguration());
            
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
        NodeInfo inode = node.getUnderlyingNode();
        int nameCode = inode.getNameCode();
        int typeCode = inode.getTypeAnnotation() & NamePool.FP_MASK;

        int inscopeNS[] = null;
        if (seenRoot) {
            inscopeNS = inode.getDeclaredNamespaces(null);
        } else {
            inscopeNS = NamespaceIterator.getInScopeNamespaceCodes(inode);
            seenRoot = true;
        }

        URI nodeBaseURI = node.getBaseURI();
        receiver.setSystemId(nodeBaseURI.toASCIIString());
        addStartElement(nameCode, typeCode, inscopeNS);
    }

    public void addStartElement(XdmNode node, QName newName) {
        NodeInfo inode = node.getUnderlyingNode();
        int nameCode = pool.allocate(newName.getPrefix(), newName.getNamespaceURI(), newName.getLocalName());
        int typeCode = inode.getTypeAnnotation() & NamePool.FP_MASK;

        int inscopeNS[] = null;
        if (seenRoot) {
            inscopeNS = inode.getDeclaredNamespaces(null);
        } else {
            inscopeNS = NamespaceIterator.getInScopeNamespaceCodes(inode);
            seenRoot = true;
        }

        URI nodeBaseURI = node.getBaseURI();
        receiver.setSystemId(nodeBaseURI.toASCIIString());
        addStartElement(nameCode, typeCode, inscopeNS);
    }

    public void addStartElement(QName newName) {
        //System.err.println("newName: " + newName.getPrefix() + ", " + newName.getNamespaceURI() + ", " + newName.getLocalName()); 
        int nameCode = pool.allocate(newName.getPrefix(), newName.getNamespaceURI(), newName.getLocalName());
        int typeCode = StandardNames.XS_UNTYPED;
        int inscopeNS[] = null;
        addStartElement(nameCode, typeCode, inscopeNS);
    }

    public void addStartElement(int nameCode, int typeCode, int nscodes[]) {
        int locId;
        String sysId = receiver.getSystemId();
        if (sysId == null) {
            locId = 0;
        } else {
            locId = xLocationProvider.allocateLocation(sysId);
        }

        try {
            receiver.startElement(nameCode, typeCode, locId, 0);
            if (nscodes != null) {
                for (int ns : nscodes) {
                    receiver.namespace(ns, 0);
                }
            }
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
        int nameCode = inode.getNameCode();
        int typeCode = inode.getTypeAnnotation() & NamePool.FP_MASK;

        try {
            receiver.attribute(nameCode, typeCode, newValue, 0, 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addAttribute(int nameCode, int typeCode, String newValue) {
        try {
            receiver.attribute(nameCode, typeCode, newValue, 0, 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    public void addAttribute(QName attrName, String newValue) {
        int nameCode = pool.allocate(attrName.getPrefix(),attrName.getNamespaceURI(),attrName.getLocalName());
        int typeCode = StandardNames.XS_UNTYPED_ATOMIC;
        try {
            receiver.attribute(nameCode, typeCode, newValue, 0, 0);
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

