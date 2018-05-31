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

package com.xmlcalabash.extensions;

import java.util.Hashtable;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.*;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

import java.io.*;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "cx:hash-document",
        type = "{http://xmlcalabash.com/ns/extensions}hash-document")

public class HashDocument extends DefaultStep {
    private static final QName _content_type = new QName("content-type");
    private static final QName c_encoding = new QName("c", XProcConstants.NS_XPROC_STEP, "encoding");
    private static final QName c_body = new QName("c", XProcConstants.NS_XPROC_STEP, "body");
    private static final QName c_json = new QName("c", XProcConstants.NS_XPROC_STEP, "json");
    private static final QName cx_decode = new QName("cx", XProcConstants.NS_CALABASH_EX, "decode");
    private static final QName _algorithm = new QName("", "algorithm");
    private static final QName _hash_version = new QName("", "hash-version");
    private static final QName _crc = new QName("", "crc");
    private static final QName _md = new QName("", "md");
    private static final QName _sha = new QName("", "sha");
    private static final QName _hmac = new QName("cx", XProcConstants.NS_CALABASH_EX, "hmac");
    private static final QName _accessKey = new QName("cx", XProcConstants.NS_CALABASH_EX, "accessKey");
    private Hashtable<QName,String> params = new Hashtable<QName, String> ();


    private ReadablePipe source = null;
    private WritablePipe result = null;

    public HashDocument(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value.getString());
    }

    public void reset() {
        params.clear();
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        QName algorithm = getOption(_algorithm).getQName();

        String hashVersion = null;
        if (getOption(_hash_version) != null) {
            hashVersion = getOption(_hash_version).getString();
        }

        XdmNode doc = source.read();

        if (doc == null || source.moreDocuments()) {
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }

        XdmNode root = S9apiUtils.getDocumentElement(doc);

        String decode = step.getExtensionAttribute(cx_decode);
        if (decode == null) {
            decode = root.getAttributeValue(cx_decode);
        }

        String contentType = root.getAttributeValue(_content_type);
        byte[] bytes = null;
        if (("true".equals(decode) || "1".equals(decode))
             && ((XProcConstants.NS_XPROC_STEP.equals(root.getNodeName().getNamespaceURI())
                  && "base64".equals(root.getAttributeValue(_encoding)))
                 || ("".equals(root.getNodeName().getNamespaceURI())
                     && "base64".equals(root.getAttributeValue(c_encoding))))) {
            bytes = binaryContent(doc);
        } else if (("true".equals(decode) || "1".equals(decode))
            && XProcConstants.c_result.equals(root.getNodeName())
                && root.getAttributeValue(_content_type) != null
                    && root.getAttributeValue(_content_type).startsWith("text/")) {
            bytes = textContent(doc);
        } else if (runtime.transparentJSON()
                   && (((c_body.equals(root.getNodeName())
                        && ("application/json".equals(contentType)
                            || "text/json".equals(contentType)))
                       || c_json.equals(root.getNodeName()))
                       || JSONtoXML.JSONX_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.JXML_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.MLJS_NS.equals(root.getNodeName().getNamespaceURI()))) {
            bytes = jsonContent(doc);
        } else {
            bytes = XmlContent(doc);
        }

        String hash = "";
        if (_crc.equals(algorithm)) {
            hash = HashUtils.crc(bytes, hashVersion);
        } else if (_md.equals(algorithm)) {
            hash = HashUtils.md(bytes, hashVersion);
        } else if (_sha.equals(algorithm)) {
            hash = HashUtils.sha(bytes, hashVersion);
        } else if (_hmac.equals(algorithm)) {
            hash = HashUtils.hmac(bytes, params.get(_accessKey));
        } else {
            throw XProcException.dynamicError(36);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText(hash);
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }

    private byte[] XmlContent(final XdmNode doc) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            try {
                Serializer serializer = makeSerializer();
                serializer.setOutputStream(baos);
                S9apiUtils.serialize(runtime, doc, serializer);
            } catch (SaxonApiException e) {
                throw new XProcException(e);
            } finally {
                baos.close();
            }
        } catch (IOException e) {
            throw new XProcException(e);
        }

        return baos.toByteArray();
    }

    private byte[] binaryContent(final XdmNode doc) {
        return Base64.decode(doc.getStringValue());
    }

    private byte[] textContent(final XdmNode doc) {
        Serializer serializer = makeSerializer();
        serializer.setOutputProperty(Serializer.Property.METHOD, "text");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.setOutputStream(baos);
        try {
            S9apiUtils.serialize(runtime, doc, serializer);
        } catch (SaxonApiException e) {
            throw new XProcException(e);
        }

        return baos.toByteArray();
    }

    private byte[] jsonContent(final XdmNode doc) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            try {
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, "UTF-8"));
                String json = XMLtoJSON.convert(doc);
                writer.print(json);
            } catch (UnsupportedEncodingException e) {
                // nop; this is never going to happen
            } finally {
                baos.close();
            }
        } catch (IOException e) {
            throw new XProcException(e);
        }

        return baos.toByteArray();
    }
}

