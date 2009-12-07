/*
 * UnescapeMarkup.java
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

import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import org.xml.sax.InputSource;
import org.ccil.cowan.tagsoup.Parser;
import net.sf.saxon.s9api.*;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceIterator;

import javax.xml.transform.sax.SAXSource;

import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */
public class UnescapeMarkup extends DefaultStep {
    private static final QName _namespace = new QName("namespace");
    private static final QName _content_type = new QName("content-type");
    private static final QName _encoding = new QName("encoding");
    private static final QName _charset = new QName("charset");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private String namespace = null;

    /**
     * Creates a new instance of UnescapeMarkup
     */
    public UnescapeMarkup(XProcRuntime runtime, XAtomicStep step) {
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

        if (getOption(_namespace) != null) {
            namespace = getOption(_namespace).getString();
        }

        String encoding = null;
        if (getOption(_encoding) != null) {
            encoding = getOption(_encoding).getString();
        }

        String charset = null;
        if (getOption(_charset) != null) {
            charset = getOption(_charset).getString();
        }

        XdmNode doc = source.read();

        String escapedContent = null;
        if ("base64".equals(encoding)) {
            if (charset == null) {
                throw XProcException.stepError(10);
            }
            escapedContent = decodeBase64(doc, charset);
        } else if (encoding != null) {
            throw new XProcException("Unexpected encoding: " + encoding);
        } else {
            escapedContent = extractText(doc);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(doc.getBaseURI());

        XdmSequenceIterator iter = doc.axisIterator(Axis.CHILD);
        XdmNode child = (XdmNode) iter.next();
        while (child.getNodeKind() != XdmNodeKind.ELEMENT) {
            tree.addSubtree(child);
            child = (XdmNode) iter.next();
        }
        tree.addStartElement(child);
        tree.addAttributes(child);
        tree.startContent();

        String contentType = getOption(_content_type, "application/xml");
        if ("text/html".equals(contentType)) {
            XdmNode tagDoc = tagSoup(escapedContent);
            if (namespace == null) {
                tree.addSubtree(tagDoc);
            } else {
                remapDefaultNamespace(tree, tagDoc);
            }
        } else if (!"application/xml".equals(contentType)) {
            throw XProcException.stepError(51);
        } else {
            // Put a wrapper around it so that it doesn't have to have a single root...
            escapedContent = "<wrapper>" + escapedContent + "</wrapper>";

            StringReader sr = new StringReader(escapedContent);
            InputSource isource = new InputSource(sr);
            SAXSource source = new SAXSource(isource);
            DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
            builder.setDTDValidation(false);

            XdmNode unesc = builder.build(source);
            /*
            if (namespace == null) {
                XdmSequenceIterator unesciter = unesc.axisIterator(Axis.CHILD);
                while (unesciter.hasNext()) {
                    XdmNode unescnode = (XdmNode) unesciter.next();
                    tree.addSubtree(unescnode);
                }
            } else {
                remapDefaultNamespace(tree, unesc);
            }
            */

            // Now ignore the wrapper that we added...
            XdmNode dummyWrapper = S9apiUtils.getDocumentElement(unesc);
            XdmSequenceIterator realNodes = dummyWrapper.axisIterator(Axis.CHILD);
            while (realNodes.hasNext()) {
                unesc = (XdmNode) realNodes.next();
                if (namespace == null) {
                    tree.addSubtree(unesc);
                } else {
                    remapDefaultNamespace(tree, unesc);
                }
            }
        }

        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }

    private void remapDefaultNamespace(TreeWriter tree, XdmNode unescnode) {
        if (unescnode.getNodeKind() == XdmNodeKind.ELEMENT) {
            NodeInfo inode = unescnode.getUnderlyingNode();
            NamePool pool = inode.getNamePool();
            int inscopeNS[] = NamespaceIterator.getInScopeNamespaceCodes(inode);

            boolean replaced = false;
            int newNS[] = null;
            if (inscopeNS.length > 0) {
                newNS = new int[inscopeNS.length+1];
                for (int pos = 0; pos < inscopeNS.length; pos++) {
                    int ns = inscopeNS[pos];
                    String pfx = pool.getPrefixFromNamespaceCode(ns);
                    //String uri = pool.getURIFromNamespaceCode(ns);

                    if ("".equals(pfx)) {
                        int newns = pool.getNamespaceCode(pfx,namespace);
                        if (newns < 0) {
                            newns = pool.allocateNamespaceCode(pfx,namespace);
                        }
                        newNS[pos] = newns;
                        replaced = true;
                    } else {
                        newNS[pos] = ns;
                    }
                }
                if (!replaced) {
                    int newns = pool.getNamespaceCode("",namespace);
                    if (newns < 0) {
                        newns = pool.allocateNamespaceCode("",namespace);
                    }
                    newNS[newNS.length-1] = newns;
                }
            }

            // Careful, we're messing with the namespace bindings
            // Make sure the nameCode is right...
            int nameCode = inode.getNameCode();
            int typeCode = inode.getTypeAnnotation() & NamePool.FP_MASK;
            String pfx = pool.getPrefix(nameCode);
            String uri = pool.getURI(nameCode);

            if ("".equals(pfx) && !namespace.equals(uri)) {
                nameCode = pool.allocate(pfx,namespace,unescnode.getNodeName().getLocalName());
            }

            tree.addStartElement(nameCode, typeCode, newNS);

            XdmSequenceIterator iter = unescnode.axisIterator(Axis.ATTRIBUTE);
            while (iter.hasNext()) {
                XdmNode child = (XdmNode) iter.next();
                tree.addAttribute(child);
            }

            XdmSequenceIterator childNodes = unescnode.axisIterator(Axis.CHILD);
            while (childNodes.hasNext()) {
                XdmNode child = (XdmNode) childNodes.next();
                remapDefaultNamespace(tree, child);
            }
            
            tree.addEndElement();
        } else {
            tree.addSubtree(unescnode);
        }
    }

    private String extractText(XdmNode doc) {
        String content = "";
        XdmSequenceIterator iter = doc.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            if (child.getNodeKind() == XdmNodeKind.ELEMENT || child.getNodeKind() == XdmNodeKind.TEXT) {
                content += child.getStringValue();
            }
        }
        return content;
    }

    private String decodeBase64(XdmNode doc, String charset) {
        String content = extractText(doc);
        byte[] decoded = Base64.decode(content);
        try {
            return new String(decoded, charset);
        } catch (UnsupportedEncodingException uee) {
            throw XProcException.stepError(10, uee);
        }
    }

    private XdmNode tagSoup(String text) {
        StringReader inputStream = new StringReader(text);
        InputSource source = new InputSource(inputStream);
        Parser parser = new Parser();
        SAXSource saxSource = new SAXSource(parser, source);
        DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
        try {
            XdmNode doc = builder.build(saxSource);
            return doc;
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }
}

