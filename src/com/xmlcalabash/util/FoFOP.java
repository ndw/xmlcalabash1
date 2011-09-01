package com.xmlcalabash.util;

import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.InputSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 6:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class FoFOP implements FoProcessor {
    FopFactory fopFactory = null;
    XStep step = null;
    URIResolver resolver = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        fopFactory = FopFactory.newInstance();
        this.step = step;
        resolver = runtime.getResolver();
    }
    public void format(InputSource fodoc, OutputStream out, String contentType) {
        String outputFormat = null;
        if (contentType == null || "application/pdf".equalsIgnoreCase(contentType)) {
            outputFormat = MimeConstants.MIME_PDF; // "PDF";
        } else if ("application/PostScript".equalsIgnoreCase(contentType)) {
            outputFormat = MimeConstants.MIME_POSTSCRIPT; //"PostScript";
        } else if ("application/afp".equalsIgnoreCase(contentType)) {
            outputFormat =  MimeConstants.MIME_AFP;  //"AFP";
        } else if ("application/rtf".equalsIgnoreCase(contentType)) {
            outputFormat = MimeConstants.MIME_RTF;
        } else if ("text/plain".equalsIgnoreCase(contentType)) {
           outputFormat = MimeConstants.MIME_PLAIN_TEXT;
        } else {
            throw new XProcException(step.getNode(), "Unsupported content-type on p:xsl-formatter: " + contentType);
        }

        try {
            SAXSource source = new SAXSource(fodoc);
            if (resolver != null) {
                fopFactory.setURIResolver(resolver);
            }
            Fop fop = fopFactory.newFop(outputFormat, out);
            FOUserAgent userAgent = fop.getUserAgent();
            userAgent.setBaseURL(step.getNode().getBaseURI().toString());
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, new SAXResult(fop.getDefaultHandler()));
        } catch (Exception e) {
            throw new XProcException(step.getNode(), "Failed to process FO document with FOP", e);
        }
    }
}
