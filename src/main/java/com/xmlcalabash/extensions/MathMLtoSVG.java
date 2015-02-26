package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sourceforge.jeuclid.MathMLParserSupport;
import net.sourceforge.jeuclid.MutableLayoutContext;
import net.sourceforge.jeuclid.context.LayoutContextImpl;
import net.sourceforge.jeuclid.context.Parameter;
import net.sourceforge.jeuclid.converter.Converter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "cx:mathml-to-svg",
        type = "{http://xmlcalabash.com/ns/extensions}mathml-to-svg")

public class MathMLtoSVG extends DefaultStep {
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Properties options = new Properties();
    private MutableLayoutContext params = null;

    /**
     * Creates a new instance of MathMLtoSVG
     */
    public MathMLtoSVG(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        if (!"".equals(name.getNamespaceURI())) {
            throw new XProcException(step.getNode(), "The cx:mathml-to-svg parameters are in no namespace: " + name + " (" + name.getNamespaceURI() + ")");
        }
        options.setProperty(name.getLocalName(), value.getString());
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode doc = source.read();

        if (doc == null || source.moreDocuments()) {
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }

        Converter conv = Converter.getInstance();
        params = new LayoutContextImpl(LayoutContextImpl.getDefaultLayoutContext());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        setJParameter(Parameter.ANTIALIAS,                "antialias");
        setJParameter(Parameter.ANTIALIAS_MINSIZE,        "antialias-minsize");
        setJParameter(Parameter.DEBUG,                    "debug");
        setJParameter(Parameter.DISPLAY,                  "display");
        setJParameter(Parameter.FONTS_DOUBLESTRUCK,       "fonts-doublestruck");
        setJParameter(Parameter.FONTS_FRAKTUR,            "fonts-fraktur");
        setJParameter(Parameter.FONTS_MONOSPACED,         "fonts-monospaced");
        setJParameter(Parameter.FONTS_SANSSERIF,          "fonts-sansserif");
        setJParameter(Parameter.FONTS_SCRIPT,             "fonts-script");
        setJParameter(Parameter.FONTS_SERIF,              "fonts-serif");
        setJParameter(Parameter.MATHBACKGROUND,           "mathbackground");
        setJParameter(Parameter.MATHCOLOR,                "mathcolor");
        setJParameter(Parameter.MATHSIZE,                 "mathsize");
        setJParameter(Parameter.MFRAC_KEEP_SCRIPTLEVEL,   "mfrac-keep-scriptlevel");
        setJParameter(Parameter.SCRIPTLEVEL,              "scriptlevel");
        setJParameter(Parameter.SCRIPTMINSIZE,            "scriptminsize");
        setJParameter(Parameter.SCRIPTSIZEMULTIPLIER,     "scriptsizemultiplier");

        try {
            // This is a bit of a hack. Trying to use the DOM wrapper from NodeInfo didn't work.
            String mathML = doc.toString();
            Document jdoc = MathMLParserSupport.parseString(mathML);
            conv.convert(jdoc, bos, Converter.TYPE_SVG, params);
        } catch (SAXException e) {
            throw new XProcException(e);
        } catch (ParserConfigurationException e) {
            throw new XProcException(e);
        } catch (IOException e) {
            throw new XProcException(e);
        }

        try {
            result.write(parseSVG(bos.toString("utf-8")));
        } catch (UnsupportedEncodingException e) {
            // this can't happen
        }
    }

    private void setJParameter(Parameter p, String key) {
        String s = options.getProperty(key);
        if (s != null) {
            params.setParameter(p, p.fromString(s));
        }
    }

    private XdmNode parseSVG(String svg) throws SaxonApiException {
        DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
        builder.setLineNumbering(true);
        builder.setDTDValidation(false);
        builder.setBaseURI(step.getNode().getBaseURI());
        StringReader reader = new StringReader(svg);
        Source src = new SAXSource(new InputSource(reader));
        return builder.build(src);
    }
}
