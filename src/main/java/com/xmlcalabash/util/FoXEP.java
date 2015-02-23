package com.xmlcalabash.util;

import com.renderx.xep.FOTarget;
import com.renderx.xep.FormatterImpl;
import com.renderx.xep.lib.ConfigurationException;
import com.renderx.xep.lib.Logger;
import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import net.sf.saxon.s9api.XdmNode;
import org.slf4j.LoggerFactory;
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
    private org.slf4j.Logger logger = LoggerFactory.getLogger(FoXEP.class);
    XProcRuntime runtime = null;
    FormatterImpl xep = null;
    XStep step = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        this.runtime = runtime;
        this.step = step;
        try {
            xep = new FormatterImpl(options, new FoLogger());
        } catch (ConfigurationException ce) {
            throw new XProcException("Failed to initialize XEP", ce);
        }
    }

    public void format(XdmNode doc, OutputStream out, String contentType) {
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
            InputSource fodoc = S9apiUtils.xdmToInputSource(runtime, doc);
            SAXSource source = new SAXSource(fodoc);
            xep.render(source, new FOTarget(out, outputFormat));
        } catch (Exception e) {
            throw new XProcException(step.getNode(), "Failed to process FO document with XEP", e);
        } finally {
            xep.cleanup();
        }
    }

    private class FoLogger implements Logger {

        public void openDocument() {
            step.info(step.getNode(), "p:xsl-formatter document processing starts");
        }

        public void closeDocument() {
            step.info(step.getNode(), "p:xsl-formatter document processing ends");
        }

        public void event(String name, String message) {
            logger.trace(MessageFormatter.nodeMessage(step.getNode(),
                    "p:xsl-formatter processing " + name + ": " + message));
        }

        public void openState(String state) {
            logger.trace(MessageFormatter.nodeMessage(step.getNode(), "p:xsl-formatter process start: " + state));
        }

        public void closeState(String state) {
            logger.trace(MessageFormatter.nodeMessage(step.getNode(), "p:xsl-formatter process end: " + state));
        }

        public void info(String message) {
            step.info(step.getNode(), message);
        }

        public void warning(String message) {
            step.warning(step.getNode(), message);
        }

        public void error(String message) {
            step.error(step.getNode(), message, XProcConstants.stepError(1)); // FIXME: 1?
        }

        public void exception(String message, Exception exception) {
            throw new XProcException(message, exception);
        }
    }

}
