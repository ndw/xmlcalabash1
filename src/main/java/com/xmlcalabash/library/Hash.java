/*
 * Hash.java
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

/*
 * HST added support for RFC 2104 HMAC signature, name cx:hmac
 */

package com.xmlcalabash.library;

import java.util.zip.CRC32;
import java.util.Hashtable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:hash",
        type = "{http://www.w3.org/ns/xproc}hash")

public class Hash extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _value = new QName("", "value");
    private static final QName _algorithm = new QName("", "algorithm");
    private static final QName _version = new QName("", "version");
    private static final QName _match = new QName("", "match");
    private static final QName _crc = new QName("", "crc");
    private static final QName _md = new QName("", "md");
    private static final QName _sha = new QName("", "sha");
    private static final QName _hmac = new QName("cx", XProcConstants.NS_CALABASH_EX, "hmac");
    private static final QName _accessKey = new QName("cx", XProcConstants.NS_CALABASH_EX, "accessKey");
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private Hashtable<QName,String> params = new Hashtable<QName, String> ();
    protected static final String logger = "org.xproc.library.hash";
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private String hash = null;

    /**
     * Creates a new instance of Hash
     */
    public Hash(XProcRuntime runtime, XAtomicStep step) {
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
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        String value = getOption(_value).getString();
        QName algorithm = getOption(_algorithm).getQName();

        String version = null;
        if (getOption(_version) != null) {
            version = getOption(_version).getString();
        }

        if (_crc.equals(algorithm)) {
            hash = crc(value, version);
        } else if (_md.equals(algorithm)) {
            hash = md(value, version);
        } else if (_sha.equals(algorithm)) {
            hash = sha(value, version);
        } else if (_hmac.equals(algorithm)) {
            hash = hmac(value, params.get(_accessKey));
        } else {
            throw XProcException.dynamicError(36);
        }

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(), getOption(_match));

        if (source.moreDocuments()) {
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }

        result.write(matcher.getResult());

    }

    /*
    From the Java 1.5 docs:
    MD2: The MD2 message digest algorithm as defined in RFC 1319.
    MD5: The MD5 message digest algorithm as defined in RFC 1321.
    SHA-1: The Secure Hash Algorithm, as defined in Secure Hash Standard, NIST FIPS 180-1.
    SHA-256, SHA-384, and SHA-512: New hash algorithms for...
     */

    private String crc(String value, String version) {
        if (version == null) {
            version = "32";
        }

        if (!"32".equals(version)) {
            throw XProcException.dynamicError(36);
        }

        CRC32 crc = new CRC32();
        crc.update(value.getBytes());

        return Long.toHexString(crc.getValue());
    }

    private String md(String value, String version) {
        MessageDigest digest = null;
        if (version == null) {
            version = "5";
        }

        try {
            digest = MessageDigest.getInstance("MD" + version);
        } catch (NoSuchAlgorithmException nsae) {
            throw XProcException.dynamicError(36);
        }

        byte[] hash = digest.digest(value.getBytes());
        String result = "";

        for (byte b : hash) {
            String str = Integer.toHexString(b & 0xff);
            if (str.length() < 2) {
                str = "0" + str;
            }
            result = result + str;
        }

        return result;
    }

    private String sha(String value, String version) {
        MessageDigest digest = null;
        if (version == null) {
            version = "1";
        }

        try {
            digest = MessageDigest.getInstance("SHA-" + version);
        } catch (NoSuchAlgorithmException nsae) {
            throw XProcException.dynamicError(36);
        }

        byte[] hash = digest.digest(value.getBytes());
        String result = "";

        for (byte b : hash) {
            String str = Integer.toHexString(b & 0xff);
            if (str.length() < 2) {
                str = "0" + str;
            }
            result = result + str;
        }

        return result;
    }

    /**
     * Computes RFC 2104-compliant HMAC signature.
     * Copied/modified slightly from amazon.webservices.common.Signature
     * Contributed by Henry Thompson, used with permission
     * 
     * @param data
     *     The data to be signed.
     * @param key
     *     The signing key.
     * @return
     *     The Base64-encoded RFC 2104-compliant HMAC signature.
     * @throws
     *     XProcException exception when signature generation fails
     */
    private static String hmac(String data, String key) {
        String result = "";
        try {
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), 
                                                         HMAC_SHA1_ALGORITHM);
            
            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            
            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());
            
            // base64-encode the hmac
	        result = Base64.encodeBytes(rawHmac);
	        //System.err.println("k: "+data+" + "+key+" = "+rawHmac+" = "+result);
        } catch (Exception e) {
    	    throw XProcException.dynamicError(36,"Failed to generate HMAC : " + e.getMessage());
        }

        return result;
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        matcher.addText(hash);
        return false;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public void processText(XdmNode node) throws SaxonApiException {
        matcher.addText(hash);
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        matcher.addComment(hash);
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        matcher.addPI(node.getNodeName().getLocalName(),hash);
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        matcher.addAttribute(node.getNodeName(), hash);
    }
}

