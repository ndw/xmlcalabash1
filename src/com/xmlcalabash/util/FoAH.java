package com.xmlcalabash.util;

import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import jp.co.antenna.XfoJavaCtl.XfoException;
import jp.co.antenna.XfoJavaCtl.XfoObj;
import net.sf.saxon.s9api.XdmNode;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class FoAH implements FoProcessor {
    XProcRuntime runtime = null;
    XStep step = null;
    XfoObj ah = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        this.runtime = runtime;
        this.step = step;
        try {
            ah = new XfoObj();
        } catch (XfoException xfoe) {
            throw new XProcException(xfoe);
        }
    }
    public void format(XdmNode doc, OutputStream out, String contentType) {
        String outputFormat = null;
        if (contentType == null || "application/pdf".equals(contentType)) {
            outputFormat = "@PDF";
        } else if ("application/PostScript".equals(contentType)) {
            outputFormat = "@PS";
        } else if ("image/svg+xml".equals(contentType)) {
            outputFormat = "@SVG";
        } else if ("application/vnd.inx".equals(contentType)) {
            outputFormat = "@INX";
        } else if ("application/vnd.mif".equals(contentType)) {
            outputFormat = "@MIF";
        } else if ("text/plain".equals(contentType)) {
            outputFormat = "@TXT";
        } else {
            throw new XProcException(step.getNode(), "Unsupported content-type on p:xsl-formatter: " + contentType);
        }

        try {
            // Holy hack on a cracker!
            StringReader reader = new StringReader(doc.toString());
            ah.render(reader, out, outputFormat);
        } catch (XfoException xfoe) {
            throw new XProcException(xfoe);
        }
    }
}
