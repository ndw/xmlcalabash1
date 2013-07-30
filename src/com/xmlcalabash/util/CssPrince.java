package com.xmlcalabash.util;

import com.princexml.Prince;
import com.princexml.PrinceEvents;
import com.xmlcalabash.config.CssProcessor;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class CssPrince implements CssProcessor {
    private static final QName _content_type = new QName("content-type");
    private static final QName _encoding = new QName("", "encoding");

    XProcRuntime runtime = null;
    Properties options = null;
    String primarySS = null;
    Vector<String> userSS = new Vector<String> ();

    XStep step = null;
    Prince prince = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        this.runtime = runtime;
        this.step = step;
        this.options = options;

        String exePath = getStringProp("exePath");
        if (exePath == null) {
            exePath = System.getProperty("com.xmlcalabash.css.prince.exepath");
        }
        if (exePath == null || "".equals(exePath)) {
            throw new XProcException("Attempt to use Prince as CSS formater but no Prince exePath specified");
        }

        prince = new Prince(exePath, new PrinceMessages());

        String s = getStringProp("baseURL");
        if (s != null) {
            prince.setBaseURL(s);
        }

        Boolean b = getBooleanProp("compress");
        if (b != null) {
            prince.setCompress(b);
        }

        b = getBooleanProp("debug");
        if (b != null) {
            prince.setDebug(b);
        }

        b = getBooleanProp("embedFonts");
        if (b != null) {
            prince.setEmbedFonts(b);
        }

        b = getBooleanProp("encrypt");
        if (b != null) {
            prince.setEncrypt(b);
        }

        Integer keyBits = getIntProp("keyBits");
        if (keyBits != null) {
            String up = getStringProp("userPassword");
            String op = getStringProp("ownerPassword");
            b = getBooleanProp("disallowPrint");
            boolean dp = b == null ? false : b;
            b = getBooleanProp("disallowModify");
            boolean dm = b == null ? false : b;
            b = getBooleanProp("disallowCopy");
            boolean dc = b == null ? false : b;
            b = getBooleanProp("disallowAnnotate");
            boolean da = b == null ? false : b;
            prince.setEncryptInfo(keyBits, up, op, dp, dm, dc, da);
        }

        s = getStringProp("fileRoot");
        if (s != null) {
            prince.setFileRoot(s);
        }


        b = getBooleanProp("html");
        if (b != null) {
            prince.setHTML(b);
        }

        s = getStringProp("httpPassword");
        if (s != null) {
            prince.setHttpPassword(s);
        }

        s = getStringProp("httpUsername");
        if (s != null) {
            prince.setHttpUsername(s);
        }

        s = getStringProp("httpProxy");
        if (s != null) {
            prince.setHttpProxy(s);
        }

        s = getStringProp("inputType");
        if (s != null) {
            prince.setInputType(s);
        }

        b = getBooleanProp("javascript");
        if (b != null) {
            prince.setJavaScript(b);
        }

        s = getStringProp("log");
        if (s != null) {
            prince.setLog(s);
        }

        b = getBooleanProp("network");
        if (b != null) {
            prince.setNetwork(b);
        }

        b = getBooleanProp("subsetFonts");
        if (b != null) {
            prince.setSubsetFonts(b);
        }

        b = getBooleanProp("verbose");
        if (b != null) {
            prince.setVerbose(b);
        }

        b = getBooleanProp("XInclude");
        if (b != null) {
            prince.setXInclude(b);
        }

        s = getStringProp("scripts");
        if (s != null) {
            for (String js : s.split("\\s+")) {
                prince.addScript(js);
            }
        }
    }

    public void addStylesheet(XdmNode doc) {
        doc = S9apiUtils.getDocumentElement(doc);

        String stylesheet = null;
        if ((XProcConstants.c_data.equals(doc.getNodeName())
             && "application/octet-stream".equals(doc.getAttributeValue(_content_type)))
            || "base64".equals(doc.getAttributeValue(_encoding))) {
            byte[] decoded = Base64.decode(doc.getStringValue());
            stylesheet = new String(decoded);
        } else {
            stylesheet = doc.getStringValue();
        }

        String prefix = "temp";
        String suffix = ".css";

        File temp;
        try {
            temp = File.createTempFile(prefix, suffix);
        } catch (IOException ioe) {
            throw new XProcException(step.getNode(), "Failed to create temporary file for CSS");
        }

        temp.deleteOnExit();

        try {
            PrintStream cssout = new PrintStream(temp);
            cssout.print(stylesheet);
            cssout.close();
        } catch (FileNotFoundException fnfe) {
            throw new XProcException(step.getNode(), "Failed to write to temporary CSS file");
        }

        if (primarySS == null) {
            primarySS = temp.toURI().toASCIIString();
        } else {
            userSS.add(temp.toURI().toASCIIString());
        }
    }

    public void format(XdmNode doc, OutputStream out, String contentType) {
        if (contentType != null && !"application/pdf".equals(contentType)) {
            throw new XProcException(step.getNode(), "Unsupported content-type on p:css-formatter: " + contentType);
        }

        try {
            if (primarySS != null) {
                prince.addStyleSheet(primarySS);
            }

            for (String uri : userSS) {
                prince.addStyleSheet(uri);
            }

            ByteArrayInputStream bis = new ByteArrayInputStream(doc.toString().getBytes("UTF-8"));
            prince.convert(bis, out);
        } catch (IOException e) {
            if (runtime.getDebug()) {
                e.printStackTrace();
            }
            throw new XProcException(e);
        }
    }

    private String getStringProp(String name) {
        return options.getProperty(name);
    }

    private Integer getIntProp(String name) {
        String s = getStringProp(name);
        if (s != null) {
            try {
                int i = Integer.parseInt(s);
                return new Integer(i);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    private Boolean getBooleanProp(String name) {
        String s = options.getProperty(name);
        if (s != null) {
            return "true".equals(s);
        }
        return null;
    }

    private class PrinceMessages implements PrinceEvents {
        @Override
        public void onMessage(String msgType, String msgLoc, String msgText) {
            if ("inf".equals(msgType)) {
                step.info(step.getNode(), msgText);
            } else if ("wrn".equals(msgType)) {
                step.warning(step.getNode(), msgText);
            } else {
                step.error(step.getNode(), msgText, new QName(XProcConstants.NS_XPROC_ERROR_EX, "prince"));
            }
        }
    }
}
