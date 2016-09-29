/*
 * XInclude.java
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

import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;
import java.util.HashSet;
import java.net.URI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.util.*;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataReader;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:xinclude",
        type = "{http://www.w3.org/ns/xproc}xinclude")

public class XInclude extends DefaultStep implements ProcessMatchingNodes {
    private static final String localAttrNS = "http://www.w3.org/2001/XInclude/local-attributes";
    private static final String xiNS = "http://www.w3.org/2001/XInclude";

    private static final QName xi_include = new QName(xiNS,"include");
    private static final QName xi_fallback = new QName(xiNS,"fallback");
    private static final QName _fixup_xml_base = new QName("", "fixup-xml-base");
    private static final QName _fixup_xml_lang = new QName("", "fixup-xml-lang");
    private static final QName _set_xml_id = new QName("", "set-xml-id");
    private static final QName _accept = new QName("", "accept");
    private static final QName _accept_language = new QName("", "accept-language");
    private static final QName cx_trim = new QName("cx", XProcConstants.NS_CALABASH_EX, "trim");
    private static final QName cx_read_limit = new QName("cx", XProcConstants.NS_CALABASH_EX, "read-limit");
    private static final QName _encoding = new QName("", "encoding");
    private static final QName _href = new QName("", "href");
    private static final QName _parse = new QName("", "parse");
    private static final QName _fragid = new QName("", "fragid");
    private static final QName _xpointer = new QName("", "xpointer");
    private static final Pattern linesXptrRE = Pattern.compile("\\s*lines\\s*\\(\\s*(\\d+)\\s*-\\s*(\\d+)\\s*\\)\\s*");

    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Stack<ProcessMatch> matcherStack = new Stack<ProcessMatch> ();
    private Stack<String> inside = new Stack<String> ();
    private Stack<String> setXmlId = new Stack<String> ();
    private boolean fixupBase = false;
    private boolean fixupLang = false;
    private boolean copyAttributes = false;
    private boolean defaultTrimText = false;
    private boolean trimText = false;
    private int readLimit = 1024 * 1000 * 100;
    private Exception mostRecentException = null;

    /*
     * Creates a new instance of XInclude
     */
    public XInclude(XProcRuntime runtime, XAtomicStep step) {
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

        fixupBase = getOption(_fixup_xml_base, false);
        fixupLang = getOption(_fixup_xml_lang, false);
        copyAttributes = true; // XInclude 1.1

        String trim = getStep().getExtensionAttribute(cx_trim);
        if (trim == null || "false".equals(trim)) {
            // nop
        } else if ("true".equals(trim)) {
            defaultTrimText = true;
            trimText = true;
        } else {
            throw new XProcException("XInclude cx:trim must be 'true' or 'false'.");
        }

        trim = getStep().getExtensionAttribute(cx_read_limit);
        if (trim != null) {
            try {
                readLimit = Integer.parseInt(trim);
            } catch (NumberFormatException nfe) {
                throw new XProcException(nfe);
            }
        }

        XdmNode doc = source.read();
        XdmNode xdoc = expandXIncludes(doc);

        result.write(xdoc);
    }

    private XdmNode expandXIncludes(XdmNode doc) {
        logger.trace(MessageFormatter.nodeMessage(doc, "Starting expandXIncludes"));
        ProcessMatch matcher = new ProcessMatch(runtime, this);
        matcherStack.push(matcher);
        matcher.match(doc, new RuntimeValue("/|*", step.getNode()));
        XdmNode result = matcher.getResult();
        matcher = matcherStack.pop();
        return result;
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        //finest(node, "Start document " + matcherStack.size());
        matcherStack.peek().startDocument(node.getBaseURI());
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        //finest(node, "End document " + matcherStack.size());
        matcherStack.peek().endDocument();
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        //finest(node, "Start element " + node.getNodeName());
        ProcessMatch matcher = matcherStack.peek();
        if (xi_include.equals(node.getNodeName())) {
            String href = node.getAttributeValue(_href);
            String parse = node.getAttributeValue(_parse);
            String xptr = node.getAttributeValue(_xpointer);
            String fragid = node.getAttributeValue(_fragid);
            String setId = node.getAttributeValue(_set_xml_id);
            String accept = node.getAttributeValue(_accept);
            String accept_lang = node.getAttributeValue(_accept_language);

            if (href == null) {
                href = "";
            }

            if (accept != null && accept.matches(".*[^\u0020-\u007e].*")) {
                throw new XProcException("Invalid characters in accept value");
            }

            if (accept_lang != null && accept_lang.matches(".*[^\u0020-\u007e].*")) {
                throw new XProcException("Invalid characters in accept value");
            }

            // FIXME: Take accept and accept_language into consideration when retrieving resources

            XdmNode fallback = null;
            for (XdmNode child : new AxisNodes(node, Axis.CHILD, AxisNodes.SIGNIFICANT)) {
                if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                    if (xi_fallback.equals(child.getNodeName())) {
                        if (fallback != null) {
                            throw new XProcException(step.getNode(), "XInclude element must contain at most one xi:fallback element.");
                        }
                        fallback = child;
                    } else if (xiNS.equals(child.getNodeName().getNamespaceURI())) {
                        throw new XProcException(step.getNode(), "Element not allowed as child of XInclude: " + child.getNodeKind());
                    }
                }
            }

            boolean forceFallback = false;
            XPointer xpointer = null;
            XdmNode subdoc = null;

            if (parse == null) {
                parse = "xml";
            }

            if (parse.contains(";")) {
                parse = parse.substring(0, parse.indexOf(";")).trim();
            }

            if ("xml".equals(parse) || "application/xml".equals(parse) || ("text/xml".equals(parse) || parse.endsWith("+xml"))) {
                parse = "xml";
            } else if ("text".equals(parse) || parse.startsWith("text/")) {
                parse = "text";
            } else {
                logger.info("Unrecognized parse value on XInclude: " + parse);
                xptr = null;
                fragid = null;
                forceFallback = true;
            }

            if (xptr != null && fragid != null) {
                if (!xptr.equals(fragid)) {
                    if ("xml".equals(parse)) {
                        logger.info("XInclude specifies different xpointer/fragid, using xpointer for xml: " + xptr);
                    } else {
                        xptr = fragid;
                        logger.info("XInclude specifies different xpointer/fragid, using fragid for " + parse + ": " + xptr);
                    }
                }
            }

            if (xptr == null && fragid != null) {
                xptr = fragid;
            }

            trimText = defaultTrimText;
            String trim = node.getAttributeValue(cx_trim);
            if (trim == null) {
                // nop
            } else if ("true".equals(trim) || "false".equals(trim)) {
                trimText = "true".equals(trim);
            } else {
                throw new XProcException("XInclude cx:trim must be 'true' or 'false'.");
            }

            if (xptr != null) {
                /* HACK */
                if ("text".equals(parse)) {
                    String xtrim = xptr.trim();
                    // What about spaces around the "=" !
                    if (xtrim.startsWith("line=") || xtrim.startsWith("char=")) {
                        xptr = "text(" + xptr + ")";
                    } else if (xtrim.startsWith("search=")) {
                        xptr = "search(" + xptr + ")";
                    }
               }
                xpointer = new XPointer(runtime, xptr, readLimit);
            }

            if (forceFallback) {
                logger.trace(MessageFormatter.nodeMessage(node, "XInclude fallback forced"));
                fallback(node, href);
                return false;
            } else if ("text".equals(parse)) {
                readText(href, node, node.getBaseURI().toASCIIString(), xpointer, matcher);
                return false;
            } else {
                setXmlId.push(setId);

                subdoc = readXML(node, href, node.getBaseURI().toASCIIString());

                String iuri = null;

                if (subdoc == null) {
                    logger.trace(MessageFormatter.nodeMessage(node, "XInclude parse failed: " + href));
                    fallback(node, href);
                    setXmlId.pop();
                    return false;
                } else {
                    iuri = subdoc.getBaseURI().toASCIIString();
                    if (xptr != null) {
                        iuri += "#" + xptr;
                    }

                    if (inside.contains(iuri)) {
                        throw XProcException.stepError(29,"XInclude document includes itself: " + href);
                    }

                    logger.trace(MessageFormatter.nodeMessage(node, "XInclude parse: " + href));
                }

                Vector<XdmNode> nodes = null;
                if (xpointer == null) {
                    nodes = new Vector<XdmNode> ();

                    // Put all the children of the document in there, so that we can add xml:base to the root(s)...
                    XdmSequenceIterator iter = subdoc.axisIterator(Axis.CHILD);
                    while (iter.hasNext()) {
                        XdmNode child = (XdmNode) iter.next();
                        nodes.add(child);
                    }
                } else {
                    Hashtable<String,String> nsBindings = xpointer.xpathNamespaces();
                    nodes = xpointer.selectNodes(runtime,subdoc);
                    if (nodes == null) {
                        logger.trace(MessageFormatter.nodeMessage(node, "XInclude parse failed: " + href));
                        fallback(node, href);
                        setXmlId.pop();
                        return false;
                    }
                }

                for (XdmNode snode : nodes) {
                    if ((fixupBase || fixupLang || copyAttributes) && snode.getNodeKind() == XdmNodeKind.ELEMENT) {
                        Fixup fixup = new Fixup(runtime,node);
                        snode = fixup.fixup(snode);
                    }

                    if (snode.getNodeKind() == XdmNodeKind.ELEMENT || snode.getNodeKind() == XdmNodeKind.DOCUMENT) {
                        inside.push(iuri);
                        XdmNode ex = expandXIncludes(snode);
                        matcher.addSubtree(ex);
                        inside.pop();
                    } else {
                        matcher.addSubtree(snode);
                    }
                }

                setXmlId.pop();

                return false;
            }
        } else if (xi_fallback.equals(node.getNodeName())) {
            throw new XProcException("Invalid placement for xi:fallback element");
        } else {
            matcher.addStartElement(node);
            matcher.addAttributes(node);
            matcher.startContent();
            return true;
        }
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("processAttribute can't happen in XInclude");
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        if (xi_include.equals(node.getNodeName())) {
            // Do nothing, we've already output the subtree that replaced xi:include
        } else {
            //finest(node, "End element " + node.getNodeName());
            matcherStack.peek().addEndElement();
        }
    }

    public void processText(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("processText can't happen in XInclude");
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("processComment can't happen in XInclude");
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("processPI can't happen in XInclude");
    }

    private void readText(final String href, final XdmNode node,
            String base, final XPointer xpointer, final TreeWriter matcher) {
        logger.trace("XInclude read text: " + href + " (" + base + ")");

        DataStore store = runtime.getDataStore();
        try {
            store.readEntry(href, base, "text/plain, text/*, */*", null, new DataReader() {
                public void load(URI id, String media, InputStream content,
                        long len) throws IOException {
                    String text = readText(node, xpointer, media, content, len);
                    if (text == null) {
                        logger.trace(MessageFormatter.nodeMessage(node, "XInclude text parse failed: " + href));
                        fallback(node, href);
                    } else {
                        logger.trace(MessageFormatter.nodeMessage(node, "XInclude text parse: " + href));
                        matcher.addText(text);
                    }
                }
            });
        } catch (Exception e) {
            logger.debug("XInclude read text failed");
            mostRecentException = e;
            fallback(node, href);
        }
    }

    String readText(final XdmNode node, final XPointer xpointer, String media,
            InputStream content, long len) throws IOException {
        String charset = HttpUtils.getCharset(media);

        if (charset == null && node.getAttributeValue(_encoding) != null) {
            charset = node.getAttributeValue(_encoding);
        }

        if (charset == null) {
            charset = "utf-8";
        }

        // Get the response
        BufferedReader rd = new BufferedReader(new InputStreamReader(content, charset));

        String data = "";
        if (xpointer != null) {
            data = xpointer.selectText(rd, (int) len);
        } else {
            String line;
            while ((line = rd.readLine()) != null) {
                data += line + "\n";
            }
        }

        rd.close();

        if (trimText) {
            return data.trim();
        } else {
            return data;
        }
    }

    public XdmNode readXML(XdmNode node, String href, String base) {
        logger.trace("XInclude read XML: " + href + " (" + base + ")");

        if (href == null || "".equals(href)) {
            XdmNode ptr = node;
            while (ptr.getParent() != null) {
                ptr = ptr.getParent();
            }
            return ptr;
        } else {
            try {
                XdmNode doc = runtime.parse(href, base);
                return doc;
            } catch (Exception e) {
                logger.debug("XInclude read XML failed");
                mostRecentException = e;
                return null;
            }
        }
    }

    public void fallback(XdmNode node, String href) {
        logger.trace(MessageFormatter.nodeMessage(node, "fallback: " + node.getNodeName()));

        // N.B. We've already tested for at most one xi:fallback element
        XdmNode fallback = null;
        for (XdmNode child : new AxisNodes(node, Axis.CHILD, AxisNodes.SIGNIFICANT)) {
            if (child.getNodeKind() == XdmNodeKind.ELEMENT && xi_fallback.equals(child.getNodeName())) {
                fallback = child;
            }
        }

        if (fallback == null) {
            if (mostRecentException != null) {
                throw new XProcException(step.getNode(), "XInclude resource error (" + href + ") and no fallback provided.", mostRecentException);
            } else {
                throw new XProcException(step.getNode(), "XInclude resource error (" + href + ") and no fallback provided.");
            }
        }

        XdmSequenceIterator iter = fallback.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode fbc = (XdmNode) iter.next();
            if (fbc.getNodeKind() == XdmNodeKind.ELEMENT) {
                fbc = expandXIncludes(fbc);
            }
            matcherStack.peek().addSubtree(fbc);
        }
    }

    private class Fixup implements ProcessMatchingNodes {
        private XProcRuntime runtime = null;
        private ProcessMatch matcher = null;
        private boolean root = true;
        private XdmNode xinclude = null;

        public Fixup(XProcRuntime runtime, XdmNode node) {
            this.runtime = runtime;
            xinclude = node;
        }

        public XdmNode fixup(XdmNode node) {
            matcher = new ProcessMatch(runtime, this);
            matcher.match(node, new RuntimeValue("*", step.getNode()));
            XdmNode fixed = matcher.getResult();
            return fixed;
        }

        public boolean processStartDocument(XdmNode node) throws SaxonApiException {
            matcher.startDocument(node.getBaseURI());
            return true;
        }

        public void processEndDocument(XdmNode node) throws SaxonApiException {
            matcher.endDocument();
        }

        public boolean processStartElement(XdmNode node) throws SaxonApiException {
            HashSet<QName> copied = new HashSet<QName> ();
            matcher.addStartElement(node);

            if (root) {
                root = false;

                if (copyAttributes) {
                    // Handle set-xml-id; it suppresses copying the xml:id attribute and optionally
                    // provides a value for it. (The value "" removes the xml:id.)
                    String setId = setXmlId.peek();
                    if (setId != null) {
                        copied.add(XProcConstants.xml_id);
                        if (!"".equals(setId)) {
                            matcher.addAttribute(XProcConstants.xml_id, setId);
                        }
                    }

                    XdmSequenceIterator iter = xinclude.axisIterator(Axis.ATTRIBUTE);
                    while (iter.hasNext()) {
                        XdmNode child = (XdmNode) iter.next();

                        // Attribute must be in a namespace
                        boolean copy = !"".equals(child.getNodeName().getNamespaceURI());

                        // But not in the XML namespace
                        copy = copy && !XProcConstants.NS_XML.equals(child.getNodeName().getNamespaceURI());

                        if (copy) {
                            QName aname = child.getNodeName();
                            if (localAttrNS.equals(aname.getNamespaceURI())) {
                                aname = new QName("", aname.getLocalName());
                            }

                            copied.add(aname);
                            matcher.addAttribute(aname, child.getStringValue());
                        }
                    }
                }

                XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
                while (iter.hasNext()) {
                    XdmNode child = (XdmNode) iter.next();
                    if ((XProcConstants.xml_base.equals(child.getNodeName()) && fixupBase)
                        || (XProcConstants.xml_lang.equals(child.getNodeName()) && fixupLang)) {
                        // nop;
                    } else {
                        if (!copied.contains(child.getNodeName())) {
                            copied.add(child.getNodeName());
                            matcher.addAttribute(child);
                        }
                    }
                }
                if (fixupBase) {
                    copied.add(XProcConstants.xml_base);
                    matcher.addAttribute(XProcConstants.xml_base, node.getBaseURI().toASCIIString());
                }
                String lang = getLang(node);
                if (fixupLang && lang != null) {
                    copied.add(XProcConstants.xml_lang);
                    matcher.addAttribute(XProcConstants.xml_lang, lang);
                }
            } else {
                // Careful. Don't copy ones you've already copied...
                XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
                while (iter.hasNext()) {
                    XdmNode child = (XdmNode) iter.next();
                    if (!copied.contains(child.getNodeName())) {
                        matcher.addAttribute(child);
                    }
                }
            }

            matcher.startContent();
            return true;
        }

        public void processAttribute(XdmNode node) throws SaxonApiException {
            throw new XProcException(node, "This can't happen!?");
        }

        public void processEndElement(XdmNode node) throws SaxonApiException {
            matcher.addEndElement();
        }

        public void processText(XdmNode node) throws SaxonApiException {
            throw new XProcException(node, "This can't happen!?");
        }

        public void processComment(XdmNode node) throws SaxonApiException {
            throw new XProcException(node, "This can't happen!?");
        }

        public void processPI(XdmNode node) throws SaxonApiException {
            throw new XProcException(node, "This can't happen!?");
        }

        private String getLang(XdmNode node) {
            String lang = null;
            while (lang == null && node.getNodeKind() == XdmNodeKind.ELEMENT) {
                lang = node.getAttributeValue(XProcConstants.xml_lang);
                node = node.getParent();
            }
            return lang;
        }
    }

}


