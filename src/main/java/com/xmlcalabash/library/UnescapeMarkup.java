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

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.*;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NamespaceMap;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.*;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.dom.HtmlDocumentBuilder;
import org.ccil.cowan.tagsoup.Parser;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:unescape-markup",
        type = "{http://www.w3.org/ns/xproc}unescape-markup")

public class UnescapeMarkup extends DefaultStep {
    private static final QName _namespace = new QName("namespace");
    private static final QName _content_type = new QName("content-type");
    private static final QName _encoding = new QName("encoding");
    private static final QName _charset = new QName("charset");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private String namespace = null;

    /*
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

        String contentType = getOption(_content_type, "application/xml");
        String defCharset = HttpUtils.getCharset(contentType);
        contentType = HttpUtils.baseContentType(contentType);

        if (getOption(_namespace) != null) {
            namespace = getOption(_namespace).getString();
        }

        String encoding = null;
        if (getOption(_encoding) != null) {
            encoding = getOption(_encoding).getString();
        }

        String charset = null;
        if (getOption(_charset) == null) {
            charset = defCharset;
        } else {
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
            throw new XProcException(step.getNode(), "Unexpected encoding: " + encoding);
        } else {
            escapedContent = extractText(doc);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(doc.getBaseURI());

        XdmSequenceIterator<XdmNode> iter = doc.axisIterator(Axis.CHILD);
        XdmNode child = null;
        boolean foundElement = false;
        while (!foundElement && iter.hasNext()) {
            child = iter.next();
            foundElement = (child.getNodeKind() == XdmNodeKind.ELEMENT);
            if (!foundElement) {
                System.err.println("CHILD:" + child);
                tree.addSubtree(child);
            }
        }
        tree.addStartElement(child);

        if ("text/html".equals(contentType)) {
            XdmNode tagDoc = null;
            if ("tagsoup".equals(runtime.htmlParser())) {
                tagDoc = tagSoup(escapedContent);
            } else {
                tagDoc = parseHTML(escapedContent);
            }
            if (namespace == null) {
                tree.addSubtree(tagDoc);
            } else {
                remapDefaultNamespace(tree, tagDoc);
            }
        } else if ("application/json".equals(contentType) || "text/json".equals(contentType)) {
            JSONTokener jt = new JSONTokener(escapedContent);
            XdmNode jsonDoc = JSONtoXML.convert(runtime.getProcessor(), jt, runtime.jsonFlavor());
            tree.addSubtree(jsonDoc);
        } else if (!"application/xml".equals(contentType)) {
            throw XProcException.stepError(51);
        } else {
            // Put a wrapper around it so that it doesn't have to have a single root...
            escapedContent = "<wrapper>" + escapedContent + "</wrapper>";

            StringReader sr = new StringReader(escapedContent);

            // Make sure the nodes in the unescapedContent get the right base URI
            InputSource is = new InputSource(sr);
            is.setSystemId(doc.getBaseURI().toASCIIString());

            XdmNode unesc = runtime.parse(is);

            // Now ignore the wrapper that we added...
            XdmNode dummyWrapper = S9apiUtils.getDocumentElement(unesc);
            assert dummyWrapper != null;
            XdmSequenceIterator<XdmNode> realNodes = dummyWrapper.axisIterator(Axis.CHILD);
            while (realNodes.hasNext()) {
                unesc = realNodes.next();
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
            NamespaceMap nsmap = inode.getAllNamespaces();
            if (!"".equals(nsmap.getDefaultNamespace())) {
                nsmap = nsmap.put("", namespace);
            }

            FingerprintedQName newName = new FingerprintedQName("", namespace, inode.getLocalPart());
            tree.addStartElement(newName, inode.attributes(), inode.getSchemaType(), nsmap);

            XdmSequenceIterator<XdmNode> childNodes = unescnode.axisIterator(Axis.CHILD);
            while (childNodes.hasNext()) {
                XdmNode child = childNodes.next();
                remapDefaultNamespace(tree, child);
            }
            
            tree.addEndElement();
        } else {
            tree.addSubtree(unescnode);
        }
    }

    private String extractText(XdmNode doc) {
        StringBuilder content = new StringBuilder();
        XdmSequenceIterator<XdmNode> iter = doc.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = iter.next();
            if (child.getNodeKind() == XdmNodeKind.ELEMENT || child.getNodeKind() == XdmNodeKind.TEXT) {
                content.append(child.getStringValue());
            }
        }
        return content.toString();
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
        parser.setEntityResolver(runtime.getResolver());
        SAXSource saxSource = new SAXSource(parser, source);
        DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
        try {
            return builder.build(saxSource);
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    private XdmNode parseHTML(String text) {
        HtmlDocumentBuilder htmlBuilder = new HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET);
        htmlBuilder.setEntityResolver(runtime.getResolver());
        try {
            InputSource src = new InputSource(new StringReader(text));
            Document html = htmlBuilder.parse(src);
            DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
            return builder.build(new DOMSource(html));
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }
}

