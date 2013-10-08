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
import java.net.URL;
import java.net.URLConnection;
import java.net.URISyntaxException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.util.*;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.model.*;
import net.sf.saxon.s9api.*;

import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */
public class XInclude extends DefaultStep implements ProcessMatchingNodes {
    private static final QName xi_include = new QName("http://www.w3.org/2001/XInclude","include");
    private static final QName xi_fallback = new QName("http://www.w3.org/2001/XInclude","fallback");
    private static final QName _fixup_xml_base = new QName("", "fixup-xml-base");
    private static final QName _fixup_xml_lang = new QName("", "fixup-xml-lang");
    private static final QName cx_mark_roots = new QName("cx",XProcConstants.NS_CALABASH_EX,"mark-roots");
    private static final QName cx_copy_attributes = new QName("cx",XProcConstants.NS_CALABASH_EX,"copy-attributes");
    private static final QName cx_root = new QName("cx",XProcConstants.NS_CALABASH_EX,"root");
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
    private boolean fixupBase = false;
    private boolean fixupLang = false;
    private boolean markRoots = false;
    private boolean copyAttributes = false;
    private Exception mostRecentException = null;

    /**
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

        String mark = getStep().getExtensionAttribute(cx_copy_attributes);
        if (mark == null || "false".equals(mark)) {
            // nop
        } else if ("true".equals(mark)) {
            copyAttributes = true;
        } else {
            throw new XProcException("On p:xinclude, cx:copy-attributes must be 'true' or 'false'.");
        }

        mark = getStep().getExtensionAttribute(cx_mark_roots);
        if (mark == null || "false".equals(mark)) {
            // nop
        } else if ("true".equals(mark)) {
            markRoots = true;
        } else {
            throw new XProcException("On p:xinclude, cx:mark-roots must be 'true' or 'false'.");
        }

        XdmNode doc = source.read();
        XdmNode xdoc = expandXIncludes(doc);

        result.write(xdoc);
    }

    private XdmNode expandXIncludes(XdmNode doc) {
        finest(doc, "Starting expandXIncludes");
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
            XPointer xpointer = null;
            XdmNode subdoc = null;
            boolean textfragok = runtime.getAllowXPointerOnText();

            /* HACK */
            if ("text".equals(parse) && node.getAttributeValue(_fragid) != null) {
                xptr = node.getAttributeValue(_fragid);
                // FIXME: This is a total hack
                if (!xptr.startsWith("text(")) {
                    xptr = "text(" + xptr + ")";
                }
                textfragok = true;
            }

            if (xptr != null) {
                xpointer = new XPointer(xptr);
            }

            if ("text".equals(parse)) {
                if (!textfragok && xpointer != null) {
                    throw XProcException.stepError(1, "XPointer is not allowed on XInclude when parse='text'");
                }

                String text = readText(href, node, node.getBaseURI().toASCIIString(), xpointer);
                if (text == null) {
                    finest(node, "XInclude text parse failed: " + href);
                    fallback(node, href);
                    return false;
                } else {
                    finest(node, "XInclude text parse: " + href);
                }
                matcher.addText(text);
                return false;
            } else {
                subdoc = readXML(href, node.getBaseURI().toASCIIString());

                String iuri = null;

                if (subdoc == null) {
                    finest(node, "XInclude parse failed: " + href);
                    fallback(node, href);
                    return false;
                } else {
                    iuri = subdoc.getBaseURI().toASCIIString();
                    if (xptr != null) {
                        iuri += "#" + xptr;
                    }

                    if (inside.contains(iuri)) {
                        throw XProcException.stepError(29,"XInclude document includes itself: " + href);
                    }

                    finest(node, "XInclude parse: " + href);
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
                }

                for (XdmNode snode : nodes) {
                    if ((fixupBase || fixupLang || markRoots || copyAttributes) && snode.getNodeKind() == XdmNodeKind.ELEMENT) {
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

                return false;
            }
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

    public String readText(String href, XdmNode node, String base, XPointer xpointer) {
        finest(null, "XInclude read text: " + href + " (" + base + ")");

        URI baseURI = null;
        try {
            baseURI = new URI(base);
        } catch (URISyntaxException use) {
            throw new XProcException(use);
        }

        URI hrefURI = baseURI.resolve(href);

        String data = "";

        try {
            URL url = hrefURI.toURL();
            URLConnection conn = url.openConnection();
            String contentType = conn.getContentType();
            int contentLength = conn.getContentLength();
            String charset = HttpUtils.getCharset(contentType);

            if (charset == null && node.getAttributeValue(_encoding) != null) {
                charset = node.getAttributeValue(_encoding);
            }

            if (charset == null) {
                charset = "utf-8";
            }

            // Get the response
            InputStreamReader stream = null;
            BufferedReader rd = null;

            if (charset == null) {
                stream = new InputStreamReader(conn.getInputStream());
            } else {
                stream = new InputStreamReader(conn.getInputStream(), charset);
            }

            if (xpointer != null) {
                data = xpointer.selectText(stream, contentLength);
            } else {
                rd = new BufferedReader(stream);
                String line;
                while ((line = rd.readLine()) != null) {
                    data += line + "\n";
                }
                rd.close();
            }
            stream.close();
        } catch (Exception e) {
            finest(null, "XInclude read text failed");
            mostRecentException = e;
            return null;
        }

        return data;
    }

    public XdmNode readXML(String href, String base) {
        finest(null, "XInclude read XML: " + href + " (" + base + ")");

        try {
            XdmNode doc = runtime.parse(href, base);
            return doc;
        } catch (Exception e) {
            finest(null, "XInclude read XML failed");
            mostRecentException = e;
            return null;
        }
    }

    public void fallback(XdmNode node, String href) {
        finest(node, "fallback: " + node.getNodeName());
        boolean valid = true;
        XdmNode fallback = null;
        for (XdmNode child : new RelevantNodes(runtime, node, Axis.CHILD)) {
            if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                valid = valid && xi_fallback.equals(child.getNodeName()) && (fallback == null);
                fallback = child;
            } else {
                valid = false;
            }
        }

        if (!valid) {
            throw new XProcException(step.getNode(), "XInclude element must contain exactly one xi:fallback element.");
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
                    XdmSequenceIterator iter = xinclude.axisIterator(Axis.ATTRIBUTE);
                    while (iter.hasNext()) {
                        XdmNode child = (XdmNode) iter.next();

                        boolean copy = !"".equals(child.getNodeName().getNamespaceURI()); // must be in a ns
                        copy = copy && !(XProcConstants.xml_base.equals(child.getNodeName()) && fixupBase);
                        copy = copy && !(XProcConstants.xml_lang.equals(child.getNodeName()) && fixupLang);
                        copy = copy && !(cx_mark_roots.equals(child.getNodeName()) && markRoots);

                        if (copy) {
                            copied.add(child.getNodeName());
                            matcher.addAttribute(child);
                        }
                    }
                }

                XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
                while (iter.hasNext()) {
                    XdmNode child = (XdmNode) iter.next();
                    if ((XProcConstants.xml_base.equals(child.getNodeName()) && fixupBase)
                        || (XProcConstants.xml_lang.equals(child.getNodeName()) && fixupLang)
                        || (cx_mark_roots.equals(child.getNodeName()) && markRoots)) {
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
                if (markRoots) {
                    copied.add(cx_root);
                    matcher.addAttribute(cx_root, "true");
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


