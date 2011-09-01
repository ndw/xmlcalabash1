package com.xmlcalabash.util;

import com.renderx.xep.FOTarget;
import com.renderx.xep.FormatterImpl;
import com.renderx.xep.lib.ConfigurationException;
import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import org.xml.sax.InputSource;
import javax.xml.transform.sax.SAXSource;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 6:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class FoXEP implements FoProcessor {
    FormatterImpl xep = null;
    XStep step = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        this.step = step;
        try {
            xep = new FormatterImpl(options);
        } catch (ConfigurationException ce) {
            throw new XProcException("Failed to initialize XEP", ce);
        }
    }

    public void format(InputSource fodoc, OutputStream out, String contentType) {
        String outputFormat = null;
        if (contentType == null || "application/pdf".equals(contentType)) {
            outputFormat = "PDF";
        } else if ("application/PostScript".equals(contentType)) {
            outputFormat = "PostScript";
        } else if ("application/afp".equals(contentType)) {
            outputFormat = "AFP";
        } else {
            throw new XProcException(step.getNode(), "Unsupported content-type on p:xsl-formatter: " + contentType);
        }

        try {
            SAXSource source = new SAXSource(fodoc);
            xep.render(source, new FOTarget(out, outputFormat));
        } catch (Exception e) {
            throw new XProcException(step.getNode(), "Failed to process FO document with XEP", e);
        } finally {
            xep.cleanup();
        }
    }
}
