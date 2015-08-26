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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.zip.GZIPOutputStream;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.util.*;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataWriter;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:store",
        type = "{http://www.w3.org/ns/xproc}store")

public class Store extends DefaultStep {
    private static final QName _href = new QName("href");
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
        RuntimeValue hrefOpt = getOption(_href);
        super.run(": href=" + hrefOpt.getValue().toString());

        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        XdmNode doc = source.read();

        if (doc == null || source.moreDocuments()) {
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }

        String href = null, base = null;
        if (hrefOpt != null) {
            href = hrefOpt.getString();
            base = hrefOpt.getBaseURI().toASCIIString();
        }

        if (method == CompressionMethod.GZIP) {
            logger.trace(MessageFormatter.nodeMessage(hrefOpt == null ? null : hrefOpt.getNode(),
                    "Gzipping" + (href == null ? "" : " to \"" + href + "\".")));
        } else {
            logger.trace(MessageFormatter.nodeMessage(hrefOpt.getNode(), "Storing to \"" + href + "\"."));
        }

        XdmNode root = S9apiUtils.getDocumentElement(doc);

        String decode = step.getExtensionAttribute(cx_decode);
        if (decode == null) {
            decode = root.getAttributeValue(cx_decode);
        }

        String contentType = root.getAttributeValue(_content_type);
        URI contentId;
        if (("true".equals(decode) || "1".equals(decode) || method != CompressionMethod.NONE)
             && ((XProcConstants.NS_XPROC_STEP.equals(root.getNodeName().getNamespaceURI())
                  && "base64".equals(root.getAttributeValue(_encoding)))
                 || ("".equals(root.getNodeName().getNamespaceURI())
                     && "base64".equals(root.getAttributeValue(c_encoding))))) {
            contentId = storeBinary(doc, href, base, contentType);
        } else if (("true".equals(decode) || "1".equals(decode))
            && XProcConstants.c_result.equals(root.getNodeName())
                && root.getAttributeValue(_content_type) != null
                    && root.getAttributeValue(_content_type).startsWith("text/")) {
            contentId = storeText(doc, href, base, contentType);
        } else if (runtime.transparentJSON()
                   && (((c_body.equals(root.getNodeName())
                        && ("application/json".equals(contentType)
                            || "text/json".equals(contentType)))
                       || c_json.equals(root.getNodeName()))
                       || JSONtoXML.JSONX_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.JXML_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.MLJS_NS.equals(root.getNodeName().getNamespaceURI()))) {
            contentId = storeJSON(doc, href, base, contentType);
        } else {
            contentId = storeXML(doc, href, base, contentType);
        }

        if (contentId != null) {
            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(XProcConstants.c_result);
            tree.startContent();
            tree.addText(contentId.toASCIIString());
            tree.addEndElement();
            tree.endDocument();
            result.write(tree.getResult());
        }
    }

    private URI storeXML(final XdmNode doc, String href, String base,
            String media) throws SaxonApiException {
        final Serializer serializer = makeSerializer();

        if (media == null) {
            media = "application/xml";
        }

        try {
            if (href == null) {
                OutputStream outstr;
                ByteArrayOutputStream baos;
                baos = new ByteArrayOutputStream();
                outstr = baos;
                try {
                    if (method == CompressionMethod.GZIP) {
                        GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                        outstr = gzout;
                    }

                    serializer.setOutputStream(outstr);
                    S9apiUtils.serialize(runtime, doc, serializer);
                } finally {
                    outstr.close();
                }
                returnData(baos);
                return null;
            } else {
                DataStore store = runtime.getDataStore();
                return store.writeEntry(href, base, media, new DataWriter() {
                    public void store(OutputStream outstr)
                            throws IOException {
                        if (method == CompressionMethod.GZIP) {
                            GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                            outstr = gzout;
                        }

                        serializer.setOutputStream(outstr);
                        try {
                            S9apiUtils.serialize(runtime, doc, serializer);
                        } catch (SaxonApiException e) {
                            throw new IOException(e);
                        }
                    }
                });
            }
        } catch (FileNotFoundException e) {
            throw XProcException.stepError(50);
        } catch (IOException ioe) {
            if (ioe.getCause() instanceof SaxonApiException) {
                throw (SaxonApiException) ioe.getCause();
            }
            throw XProcException.stepError(50, ioe);
        }
    }

    private URI storeBinary(XdmNode doc, String href, String base, String media) {
        if (media == null) {
            media = "application/octet-stream";
        }

        try {
            final byte[] decoded = Base64.decode(doc.getStringValue());

            if (href == null) {
                OutputStream outstr = null;
                ByteArrayOutputStream baos = null;
                baos = new ByteArrayOutputStream();
                outstr = baos;
                try {
                    if (method == CompressionMethod.GZIP) {
                        GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                        outstr = gzout;
                    }

                    outstr.write(decoded);
                } finally {
                    outstr.close();
                }
                returnData(baos);
                return null;
            } else {
                DataStore store = runtime.getDataStore();
                return store.writeEntry(href, base, media, new DataWriter() {
                    public void store(OutputStream outstr) throws IOException {
                        if (method == CompressionMethod.GZIP) {
                            GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                            outstr = gzout;
                        }

                        outstr.write(decoded);
                    }
                });
            }
        } catch (FileNotFoundException e) {
            throw XProcException.stepError(50);
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }

    private URI storeText(XdmNode doc, String href, String base, String media) {
        final Serializer serializer = makeSerializer();
        serializer.setOutputProperty(Serializer.Property.METHOD, "text");

        if (media == null) {
            media = "text/plain";
        }

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream outstr = baos;

            if (method == CompressionMethod.GZIP) {
                GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                outstr = gzout;
            }

            serializer.setOutputStream(outstr);
            try {
                S9apiUtils.serialize(runtime, doc, serializer);
            } catch (SaxonApiException e) {
                throw new IOException(e);
            }

            if (href == null) {
                returnData(baos);
                return null;
            } else {
                DataStore store = runtime.getDataStore();
                return store.writeEntry(href, base, media, new DataWriter() {
                    public void store(OutputStream outstr) throws IOException {
                        outstr.write(baos.toByteArray());
                    }
                });
            }
        } catch (FileNotFoundException e) {
            throw XProcException.stepError(50);
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }

    private URI storeJSON(final XdmNode doc, String href, String base,
            String media) throws SaxonApiException {
        if (media == null) {
            media = "application/json";
        }

        try {
            if (href == null) {
                OutputStream outstr = null;
                ByteArrayOutputStream baos = null;

                baos = new ByteArrayOutputStream();
                outstr = baos;
                try {
                    if (method == CompressionMethod.GZIP) {
                        GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                        outstr = gzout;
                    }

                    PrintWriter writer = new PrintWriter(outstr);
                    String json = XMLtoJSON.convert(doc);
                    writer.print(json);
                } finally {
                    // no need to close both 
                    // writer.close();
                    outstr.close();
                }
                returnData(baos);
                return null;
            } else {
                DataStore store = runtime.getDataStore();
                return store.writeEntry(href, base, media, new DataWriter() {
                    public void store(OutputStream outstr) throws IOException {
                        if (method == CompressionMethod.GZIP) {
                            GZIPOutputStream gzout = new GZIPOutputStream(outstr);
                            outstr = gzout;
                        }

                        PrintWriter writer = new PrintWriter(outstr);
                        String json = XMLtoJSON.convert(doc);
                        writer.print(json);
                        // No need to close writer here - the underlying 
                        // outstr gets closed by the DataStore implementation 
                        // writer.close();
                    }
                });
            }
        } catch (FileNotFoundException e) {
            throw XProcException.stepError(50);
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

