package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

public class HashUtils {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /*
    From the Java 1.5 docs:
    MD2: The MD2 message digest algorithm as defined in RFC 1319.
    MD5: The MD5 message digest algorithm as defined in RFC 1321.
    SHA-1: The Secure Hash Algorithm, as defined in Secure Hash Standard, NIST FIPS 180-1.
    SHA-256, SHA-384, and SHA-512: New hash algorithms for...
     */

    public static String crc(byte[] bytes, String version) {
        if ((version != null) && !"32".equals(version)) {
            throw XProcException.dynamicError(36);
        }

        CRC32 crc = new CRC32();
        crc.update(bytes);
        return Long.toHexString(crc.getValue());
    }

    public static String md(byte[] bytes, String version) {
        version = version == null ? "MD5" : "MD" + version;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(version);
        } catch (NoSuchAlgorithmException nsae) {
            throw XProcException.dynamicError(36);
        }

        byte[] hash = digest.digest(bytes);
        return byteString(hash);
    }

    public static String sha(byte[] bytes, String version) {
        version = version == null ? "SHA-1" : "SHA-" + version;
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance(version);
        } catch (NoSuchAlgorithmException nsae) {
            throw XProcException.dynamicError(36);
        }

        byte[] hash = digest.digest(bytes);
        return byteString(hash);
   }

    /**
     * Computes RFC 2104-compliant HMAC signature.
     * Copied/modified slightly from amazon.webservices.common.Signature
     * Contributed by Henry Thompson, used with permission
     *
     * @param bytes
     *     The data to be signed.
     * @param key
     *     The signing key.
     * @return
     *     The Base64-encoded RFC 2104-compliant HMAC signature.
     * @throws
     *     XProcException exception when signature generation fails
     */
    public static String hmac(byte[] bytes, String key) {
        String result = "";
        try {
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(bytes);

            // base64-encode the hmac
            result = Base64.encodeBytes(rawHmac);
        } catch (Exception e) {
            throw XProcException.dynamicError(36,"Failed to generate HMAC : " + e.getMessage());
        }

        return result;
    }

    private static String byteString(byte[] hash) {
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



}
