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
import net.sf.saxon.event.ComplexContentOutputter;
import net.sf.saxon.event.NamespaceReducer;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.*;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.BuiltInType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Untyped;
import org.w3c.dom.Attr;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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
    protected static final AttributeMap emptyAttributeMap = EmptyAttributeMap.getInstance();
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
            PipelineConfiguration pipe = controller.makePipelineConfiguration();
            if (runtime == null) {
                receiver = destination.getReceiver(pipe, new SerializationProperties());
            } else {
                receiver = destination.getReceiver(pipe, runtime.getDefaultSerializationProperties());
            }

            receiver.setPipelineConfiguration(pipe);

            if (baseURI != null) {
                receiver.setSystemId(baseURI.toASCIIString());
            } else {
                receiver.setSystemId("http://example.com/");
            }

            receiver = new ComplexContentOutputter(new NamespaceReducer(receiver));

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
        XdmSequenceIterator<XdmNode> iter = node.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            addSubtree(child);
        }
    }

    public void addStartElement(QName nodeName) {
        addStartElement(nodeName, EmptyAttributeMap.getInstance());
    }

    public void addStartElement(XdmNode node) {
        try {
            addStartElement(node, node.getNodeName(), node.getBaseURI());
        } catch (IllegalStateException e) {
            if (runtime != null && runtime.getIgnoreInvalidXmlBase()) {
                addStartElement(node, node.getNodeName(), null);
            } else {
                throw e;
            }
        }
    }

    public void addStartElement(XdmNode node, URI overrideBaseURI) {
        addStartElement(node, node.getNodeName(), overrideBaseURI);
    }

    public void addStartElement(XdmNode node, QName newName) {
        try {
            addStartElement(node, newName, node.getBaseURI());
        } catch (IllegalStateException e) {
            if (runtime != null && runtime.getIgnoreInvalidXmlBase()) {
                addStartElement(node, node.getNodeName(), null);
            } else {
                throw e;
            }
        }
    }

    public void addStartElement(XdmNode node, QName newName, URI overrideBaseURI) {
        AttributeMap attrs = node.getUnderlyingNode().attributes();
        addStartElement(node, newName, overrideBaseURI, attrs);
    }

    public void addStartElement(XdmNode node, AttributeMap attrs) {
        NodeInfo inode = node.getUnderlyingNode();
        addStartElement(NameOfNode.makeName(inode), attrs, inode.getSchemaType(), inode.getAllNamespaces(), node.getBaseURI());
    }

    public void addStartElement(XdmNode node, QName newName, URI overrideBaseURI, AttributeMap attrs) {
        NodeInfo inode = node.getUnderlyingNode();

        NamespaceMap inscopeNS = null;
        if (seenRoot) {
            ArrayList<NamespaceBinding> nslist = new ArrayList<>();
            Collections.addAll(nslist, inode.getDeclaredNamespaces(null));
            inscopeNS = new NamespaceMap(nslist);
        } else {
            inscopeNS = inode.getAllNamespaces();
        }

        // If the newName has no prefix, then make sure we don't pass along some other
        // binding for the default namespace...
        if ("".equals(newName.getPrefix()) && !"".equals(inscopeNS.getDefaultNamespace())) {
            inscopeNS.remove("");
        }

        // Hack. See comment at top of file
        if (overrideBaseURI != null && !"".equals(overrideBaseURI.toASCIIString())) {
            receiver.setSystemId(overrideBaseURI.toASCIIString());
        }

        FingerprintedQName newNameOfNode = new FingerprintedQName(newName.getPrefix(),newName.getNamespaceURI(),newName.getLocalName());
        addStartElement(newNameOfNode, attrs, inode.getSchemaType(), inscopeNS);
    }

    public void addStartElement(QName newName, AttributeMap attrs) {
        addStartElement(newName, attrs, NamespaceMap.emptyMap());
    }

    public void addStartElement(QName newName, AttributeMap attrs, NamespaceMap nsmap) {
        NodeName elemName = new FingerprintedQName(newName.getPrefix(), newName.getNamespaceURI(), newName.getLocalName());
        addStartElement(elemName, attrs, Untyped.INSTANCE, nsmap);
    }

    public void addStartElement(NodeName elemName, SchemaType typeCode) {
        addStartElement(elemName, emptyAttributeMap, typeCode, NamespaceMap.emptyMap());
    }

    public void addStartElement(NodeName elemName, SchemaType typeCode, NamespaceMap nsmap) {
        addStartElement(elemName, emptyAttributeMap, typeCode, nsmap);
    }

    public void addStartElement(NodeName elemName, AttributeMap attrs, SchemaType typeCode, NamespaceMap nsmap, URI overrideBaseURI) {
        // Hack. See comment at top of file
        if (overrideBaseURI != null && !"".equals(overrideBaseURI.toASCIIString())) {
            receiver.setSystemId(overrideBaseURI.toASCIIString());
        }
        addStartElement(elemName, attrs, typeCode, nsmap);
    }

    public void addStartElement(NodeName elemName, AttributeMap attrs, SchemaType typeCode, NamespaceMap nsmap) {
        // Sort out the namespaces...
        nsmap = updateMap(nsmap, elemName.getPrefix(), elemName.getURI());
        for (AttributeInfo attr : attrs) {
            if (attr.getNodeName().getURI() != null && !"".equals(attr.getNodeName().getURI())) {
                nsmap = updateMap(nsmap, attr.getNodeName().getPrefix(), attr.getNodeName().getURI());
            }
        }

        Location loc;
        String sysId = receiver.getSystemId();
        if (sysId == null) {
            loc = VoidLocation.instance();
        } else {
            loc = new SysIdLocation(sysId);
        }

        try {
            receiver.startElement(elemName, typeCode, attrs, nsmap, loc, 0);
        } catch (XPathException e) {
            throw new XProcException(e);
        }
    }

    private NamespaceMap updateMap(NamespaceMap nsmap, String prefix, String uri) {
        if (uri == null || "".equals(uri)) {
            return nsmap;
        }

        if (prefix == null || "".equals(prefix)) {
            if (!uri.equals(nsmap.getDefaultNamespace())) {
                return nsmap.put("", uri);
            }
        }

        String curNS = nsmap.getURI(prefix);
        if (curNS == null) {
            return nsmap.put(prefix, uri);
        } else if (curNS.equals(uri)) {
            return nsmap;
        }

        throw new XProcException("Cannot add " + prefix + " to namespace map with URI " + uri);
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

