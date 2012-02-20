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
import java.util.Iterator;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.util.HttpUtils;
import com.xmlcalabash.util.JSONtoXML;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.tree.util.NamespaceIterator;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.dom.HtmlDocumentBuilder;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.ccil.cowan.tagsoup.Parser;

import javax.xml.transform.dom.DOMSource;
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

        XdmSequenceIterator iter = doc.axisIterator(Axis.CHILD);
        XdmNode child = (XdmNode) iter.next();
        while (child.getNodeKind() != XdmNodeKind.ELEMENT) {
            tree.addSubtree(child);
            child = (XdmNode) iter.next();
        }
        tree.addStartElement(child);
        tree.addAttributes(child);
        tree.startContent();

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
            int nscount = 0;
            Iterator<NamespaceBinding> nsiter = NamespaceIterator.iterateNamespaces(inode);
            while (nsiter.hasNext()) {
                nscount++;
                nsiter.next();
            }
            
            boolean replaced = false;
            NamespaceBinding newNS[] = null;
            if (nscount > 0) {
                NamespaceBinding inscopeNS[] = new NamespaceBinding[nscount];
                newNS = new NamespaceBinding[nscount+1];
                for (int pos = 0; pos < inscopeNS.length; pos++) {
                    NamespaceBinding ns = inscopeNS[pos];
                    String pfx = ns.getPrefix();

                    if ("".equals(pfx)) {
                        NamespaceBinding newns = new NamespaceBinding(pfx, namespace);
                        newNS[pos] = newns;
                        replaced = true;
                    } else {
                        newNS[pos] = ns;
                    }
                }
                if (!replaced) {
                    NamespaceBinding newns = new NamespaceBinding("",namespace);
                    newNS[newNS.length-1] = newns;
                }
            }

            // Careful, we're messing with the namespace bindings
            // Make sure the nameCode is right...
            /* Not sure what to do here in 9.4. Nothing?
            int nameCode = inode.getNameCode();
            int typeCode = inode.getTypeAnnotation() & NamePool.FP_MASK;
            String pfx = pool.getPrefix(nameCode);
            String uri = pool.getURI(nameCode);

            if ("".equals(pfx) && !namespace.equals(uri)) {
                nameCode = pool.allocate(pfx,namespace,unescnode.getNodeName().getLocalName());
            }
            */

            FingerprintedQName newName = new FingerprintedQName("", namespace, inode.getLocalPart());
            tree.addStartElement(newName, inode.getSchemaType(), newNS);

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
        parser.setEntityResolver(runtime.getResolver());
        SAXSource saxSource = new SAXSource(parser, source);
        DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
        try {
            XdmNode doc = builder.build(saxSource);
            return doc;
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
            XdmNode doc = builder.build(new DOMSource(html));
            return doc;
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }
}

