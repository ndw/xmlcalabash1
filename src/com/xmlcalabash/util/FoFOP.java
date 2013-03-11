package com.xmlcalabash.util;

import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import net.sf.saxon.s9api.XdmNode;
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
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 6:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class FoFOP implements FoProcessor {
    XProcRuntime runtime = null;
    FopFactory fopFactory = null;
    Properties options = null;
    XStep step = null;
    URIResolver resolver = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        this.runtime = runtime;
        this.step = step;
        this.options = options;

        fopFactory = FopFactory.newInstance();
        resolver = runtime.getResolver();

        if (resolver != null) {
            fopFactory.setURIResolver(resolver);
        }

        try {
            String s = getStringProp("BaseURL");
            if (s != null) {
                fopFactory.setBaseURL(s);
            }

            Boolean b = getBooleanProp("BreakIndentInheritanceOnReferenceAreaBoundary");
            if (b != null) {
                fopFactory.setBreakIndentInheritanceOnReferenceAreaBoundary(b);
            }

            s = getStringProp("FontBaseURL");
            if (s != null) {
                fopFactory.getFontManager().setFontBaseURL(s);
            }

            b = getBooleanProp("Base14KerningEnabled");
            if (b != null) {
                fopFactory.getFontManager().setBase14KerningEnabled(b);
            }

            s = getStringProp("HyphenBaseURL");
            if (s != null) {
                fopFactory.setHyphenBaseURL(s);
            }

            s = getStringProp("PageHeight");
            if (s != null) {
                fopFactory.setPageHeight(s);
            }

            s = getStringProp("PageWidth");
            if (s != null) {
                fopFactory.setPageWidth(s);
            }

            Float f = getFloatProp("SourceResolution");
            if (f != null) {
                fopFactory.setSourceResolution(f);
            }

            f = getFloatProp("TargetResolution");
            if (f != null) {
                fopFactory.setTargetResolution(f);
            }

            b = getBooleanProp("StrictUserConfigValidation");
            if (b != null) {
                fopFactory.setStrictUserConfigValidation(b);
            }

            b = getBooleanProp("StrictValidation");
            if (b != null) {
                fopFactory.setStrictUserConfigValidation(b);
            }

            b = getBooleanProp("UseCache");
            if (b != null) {
                fopFactory.getFontManager().setUseCache(b);
            }

            s = getStringProp("UserConfig");
            if (s != null) {
                fopFactory.setUserConfig(s);
            }
          } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    public void format(XdmNode doc, OutputStream out, String contentType) {
        String outputFormat = null;
        if (contentType == null || "application/pdf".equalsIgnoreCase(contentType)) {
            outputFormat = "application/pdf"; // "PDF";
        } else if ("application/PostScript".equalsIgnoreCase(contentType)) {
            outputFormat = "application/postscript"; //"PostScript";
        } else if ("application/afp".equalsIgnoreCase(contentType)) {
            outputFormat =  "application/x-afp";  //"AFP";
        } else if ("application/rtf".equalsIgnoreCase(contentType)) {
            outputFormat = "application/rtf";
        } else if ("text/plain".equalsIgnoreCase(contentType)) {
           outputFormat = "text/plain";
        } else {
            throw new XProcException(step.getNode(), "Unsupported content-type on p:xsl-formatter: " + contentType);
        }

        try {
            InputSource fodoc = S9apiUtils.xdmToInputSource(runtime, doc);
            SAXSource source = new SAXSource(fodoc);
            Fop fop = fopFactory.newFop(outputFormat, out);
            FOUserAgent userAgent = fop.getUserAgent();

            Boolean b = getBooleanProp("Accessibility");
            if (b != null) {
                userAgent.setAccessibility(b);
            }

            String s = getStringProp("Author");
            if (s != null) {
                userAgent.setAuthor(s);
            }

            userAgent.setBaseURL(step.getNode().getBaseURI().toString());
            s = getStringProp("BaseURL");
            if (s != null) {
                userAgent.setBaseURL(s);
            }

            b = getBooleanProp("ConserveMemoryPolicy");
            if (b != null) {
                userAgent.setConserveMemoryPolicy(b);
            }

            s = getStringProp("CreationDate");
            if (s != null) {
                DateFormat df = DateFormat.getDateInstance();
                Date d = df.parse(s);
                userAgent.setCreationDate(d);
            }

            s = getStringProp("Creator");
            if (s != null) {
                userAgent.setCreator(s);
            }

            s = getStringProp("Keywords");
            if (s != null) {
                userAgent.setKeywords(s);
            }

            b = getBooleanProp("LocatorEnabled");
            if (b != null) {
                userAgent.setLocatorEnabled(b);
            }

            s = getStringProp("Producer");
            if (s != null) {
                userAgent.setProducer(s);
            }

            s = getStringProp("Subject");
            if (s != null) {
                userAgent.setSubject(s);
            }

            Float f = getFloatProp("TargetResolution");
            if (f != null) {
                userAgent.setTargetResolution(f);
            }

            s = getStringProp("Title");
            if (s != null) {
                userAgent.setTitle(s);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, new SAXResult(fop.getDefaultHandler()));
        } catch (Exception e) {
            throw new XProcException(step.getNode(), "Failed to process FO document with FOP", e);
        }
    }

    private String getStringProp(String name) {
        return options.getProperty(name);
    }

    private Float getFloatProp(String name) {
        String s = getStringProp(name);
        if (s != null) {
            try {
                float f = Float.parseFloat(s);
                return new Float(f);
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
}
