/*
 * Store.java
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

import java.io.*;
import java.net.URI;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.JSONtoXML;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.core.XProcRuntime;
import java.net.URLConnection;

import com.xmlcalabash.util.XMLtoJSON;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.Serializer;

import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;

import java.net.URL;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author ndw
 */
public class Store extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _encoding = new QName("encoding");
    private static final QName _content_type = new QName("content-type");
    private static final QName c_encoding = new QName("c", XProcConstants.NS_XPROC_STEP, "encoding");
    private static final QName c_body = new QName("c", XProcConstants.NS_XPROC_STEP, "body");
    private static final QName c_json = new QName("c", XProcConstants.NS_XPROC_STEP, "json");
    private static final QName cx_decode = new QName("cx", XProcConstants.NS_CALABASH_EX, "decode");

    private ReadablePipe source = null;
    private WritablePipe result = null;

    // Store also implements the cx:gzip step.
    // If gzip is true, then href may be null
    protected CompressionMethod method = CompressionMethod.NONE;
    private URI href = null;

    /**
     * Creates a new instance of Store
     */
    public Store(XProcRuntime runtime, XAtomicStep step) {
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

        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        RuntimeValue hrefOpt = getOption(_href);

        XdmNode doc = source.read();

        if (doc == null || source.moreDocuments()) {
            throw XProcException.dynamicError(6);
        }

        if (hrefOpt != null) {
            href = hrefOpt.getBaseURI().resolve(hrefOpt.getString());
        }

        if (method == CompressionMethod.GZIP) {
            finer(hrefOpt == null ? null : hrefOpt.getNode(), "Gzipping" + (href == null ? "" : " to \"" + href + "\"."));
        } else {
            finer(hrefOpt.getNode(), "Storing to \"" + href + "\".");
        }

        String decode = step.getExtensionAttribute(cx_decode);
        XdmNode root = S9apiUtils.getDocumentElement(doc);
        if (("true".equals(decode) || "1".equals(decode) || method != CompressionMethod.NONE)
             && ((XProcConstants.NS_XPROC_STEP.equals(root.getNodeName().getNamespaceURI())
                  && "base64".equals(root.getAttributeValue(_encoding)))
                 || ("".equals(root.getNodeName().getNamespaceURI())
                     && "base64".equals(root.getAttributeValue(c_encoding))))) {
            storeBinary(doc, href);
        } else if (runtime.transparentJSON()
                   && (((c_body.equals(root.getNodeName())
                        && ("application/json".equals(root.getAttributeValue(_content_type))
                            || "text/json".equals(root.getAttributeValue(_content_type))))
                       || c_json.equals(root.getNodeName()))
                       || JSONtoXML.JSONX_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.JXML_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.MLJS_NS.equals(root.getNodeName().getNamespaceURI()))) {
            storeJSON(doc, href);
        } else {
            storeXML(doc, href);
        }

        if (href != null) {
            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(XProcConstants.c_result);
            tree.startContent();
            tree.addText(href.toString());
            tree.addEndElement();
            tree.endDocument();
            result.write(tree.getResult());
        }
    }

    private void storeXML(XdmNode doc, URI href) throws SaxonApiException {
        Serializer serializer = makeSerializer();

        try {
            OutputStream outstr = null;
            ByteArrayOutputStream baos = null;

            if (href == null) {
                baos = new ByteArrayOutputStream();
                outstr = baos;
            } else if (href.getScheme().equals("file")) {
                File output = new File(href);

                File path = new File(output.getParent());
                if (!path.isDirectory()) {
                    if (!path.mkdirs()) {
                        throw XProcException.stepError(50);
                    }
                }
                outstr = new FileOutputStream(output);
            } else {
                final URLConnection conn = href.toURL().openConnection();
                conn.setDoOutput(true);
                outstr = conn.getOutputStream();
            }

            if (method == CompressionMethod.GZIP) {
                GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                outstr = gzout;
            }

            serializer.setOutputStream(outstr);
            S9apiUtils.serialize(runtime, doc, serializer);
            outstr.close();

            if (href == null) {
                returnData(baos);
            }
        } catch (IOException ioe) {
            throw XProcException.stepError(50, ioe);
        }
    }

    private void storeBinary(XdmNode doc, URI href) {
        try {
            byte[] decoded = Base64.decode(doc.getStringValue());
            OutputStream outstr = null;
            ByteArrayOutputStream baos = null;

            if (href == null) {
                baos = new ByteArrayOutputStream();
                outstr = baos;
            } else {
                File output = new File(href);

                File path = new File(output.getParent());
                if (!path.isDirectory()) {
                    if (!path.mkdirs()) {
                        throw XProcException.stepError(50);
                    }
                }

                outstr = new FileOutputStream(output);
            }

            if (method == CompressionMethod.GZIP) {
                GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                outstr = gzout;
            }

            outstr.write(decoded);
            outstr.close();

            if (href == null) {
                returnData(baos);
            }
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }

    private void storeJSON(XdmNode doc, URI href) throws SaxonApiException {
        try {
            OutputStream outstr = null;
            ByteArrayOutputStream baos = null;

            if (href == null) {
                baos = new ByteArrayOutputStream();
                outstr = baos;
            } else if (href.getScheme().equals("file")) {
                File output = new File(href);

                File path = new File(output.getParent());
                if (!path.isDirectory()) {
                    if (!path.mkdirs()) {
                        throw XProcException.stepError(50);
                    }
                }

                outstr = new FileOutputStream(output);
            } else {
                final URLConnection conn = href.toURL().openConnection();
                conn.setDoOutput(true);
                outstr = conn.getOutputStream();
            }

            if (method == CompressionMethod.GZIP) {
                GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                outstr = gzout;
            }

            PrintWriter writer = new PrintWriter(outstr);
            String json = XMLtoJSON.convert(doc);
            writer.print(json);
            writer.close();
            outstr.close();

            if (href == null) {
                returnData(baos);
            }
        } catch (IOException ioe) {
            throw XProcException.stepError(50, ioe);
        }
    }

    public void returnData(ByteArrayOutputStream baos) {
        // We're only called if the output is compressed

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_data);
        tree.addAttribute(_encoding, "base64");
        tree.addAttribute(_content_type, "application/x-gzip");
        tree.startContent();
        tree.addText(Base64.encodeBytes(baos.toByteArray()));
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());

    }

    protected enum CompressionMethod { NONE, GZIP };
}

