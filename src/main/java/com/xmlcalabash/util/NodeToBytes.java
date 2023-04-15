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

package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

import java.io.*;
import java.net.URI;
import java.net.URLConnection;

/**
 *
 * @author ndw
 */
public class NodeToBytes {
    private static final QName _encoding = new QName("encoding");
    private static final QName _content_type = new QName("content-type");
    private static final QName c_encoding = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "encoding");
    private static final QName c_body = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "body");
    private static final QName c_json = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "json");
    private static final QName cx_decode = XProcConstants.qNameFor(XProcConstants.NS_CALABASH_EX, "decode");

    private NodeToBytes() {
        // you aren't allowed to do this
    }

    public static byte[] convert(XProcRuntime runtime, XdmNode doc, boolean decode) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        XdmNode root = S9apiUtils.getDocumentElement(doc);
        assert root != null;

        if (decode
             && ((XProcConstants.NS_XPROC_STEP == root.getNodeName().getNamespaceUri())
                  && "base64".equals(root.getAttributeValue(_encoding)))
                 || (root.getNodeName().getNamespaceUri() == NamespaceUri.NULL)
                     && "base64".equals(root.getAttributeValue(c_encoding))) {
            storeBinary(doc, stream);
        } else if (runtime.transparentJSON()
                   && (((c_body.equals(root.getNodeName())
                        && ("application/json".equals(root.getAttributeValue(_content_type))
                            || "text/json".equals(root.getAttributeValue(_content_type))))
                       || c_json.equals(root.getNodeName()))
                       || JSONtoXML.JSONX_NS == root.getNodeName().getNamespaceUri()
                       || JSONtoXML.JXML_NS == root.getNodeName().getNamespaceUri()
                       || JSONtoXML.MLJS_NS == root.getNodeName().getNamespaceUri())) {
            storeJSON(doc, stream);
        } else {
            storeXML(runtime, doc, stream);
        }

        return stream.toByteArray();
    }

    private static void storeXML(XProcRuntime runtime, XdmNode doc, OutputStream stream) {
        try {
            try {
                Serializer serializer = runtime.getProcessor().newSerializer();
                serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8");
                serializer.setOutputProperty(Serializer.Property.INDENT, "no");
                serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
                serializer.setOutputProperty(Serializer.Property.METHOD, "xml");

                serializer.setOutputStream(stream);
                S9apiUtils.serialize(runtime, doc, serializer);
            } finally {
                stream.close();
            }
        } catch (IOException ioe) {
            throw new XProcException("Failed to serialize as XML: " + doc, ioe);
        } catch (SaxonApiException sae) {
            throw new XProcException("Failed to serialize as XML: " + doc, sae);
        } 
    }

    private static void storeBinary(XdmNode doc, OutputStream stream) {
        try {
            try {
                byte[] decoded = Base64.decode(doc.getStringValue());
                stream.write(decoded);
            } finally {
                stream.close();
            }
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }

    private static void storeJSON(XdmNode doc, OutputStream stream) {
        try {
            try {
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
                String json = XMLtoJSON.convert(doc);
                writer.print(json);
            } finally {
                // Closing both might not be safe, depending on the concrete
                // implementation of OutputStream. Omitting the call to 
                // PrintWriter.close is safe, however.
//                            writer.close();
                stream.close();
            }
        } catch (IOException ioe) {
            throw XProcException.stepError(50, ioe);
        }
    }
}

