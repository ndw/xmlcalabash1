package com.xmlcalabash.library;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.model.RuntimeValue;
import com.renderx.xep.FormatterImpl;
import com.renderx.xep.FOTarget;

import javax.xml.transform.sax.SAXSource;
import java.util.Properties;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import com.xmlcalabash.util.URIUtils;
import org.xml.sax.InputSource;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 21, 2008
 * Time: 6:59:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class XSLFormatter extends DefaultStep {
    private static final QName _href = new QName("","href");
    private static final QName _content_type = new QName("","content-type");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Properties options = new Properties();

    /** Creates a new instance of Unzip */
    public XSLFormatter(XProcRuntime runtime, XAtomicStep step) {
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
            throw new XProcException(step.getNode(), "The p:xsl-formatter parameters are in no namespace: " + name + " (" + name.getNamespaceURI() + ")");
        }
        options.setProperty(name.getLocalName(), value.getString());
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        String contentType = getOption(_content_type).getString();
        String href = getOption(_href).getBaseURI().resolve(getOption(_href).getString()).toASCIIString();
        String output = null;

        if (href.startsWith("file:/")) {
            output = URIUtils.getFile(href).getPath();
        } else {
            throw new XProcException(step.getNode(), "Don't know how to write p:xsl-formatter output to " + href);
        }

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

        FormatterImpl xep = null;
        try {
            xep = new FormatterImpl(options);
        }
        catch (Exception e) {
            throw new XProcException(e);
        }

        try {
            InputSource doc = S9apiUtils.xdmToInputSource(runtime, source.read());
            SAXSource saxdoc = new SAXSource(doc);

            OutputStream out = null;
            out = new BufferedOutputStream(new FileOutputStream(output));

            try {
                try {
                    xep.render(saxdoc, new FOTarget(out, outputFormat));
                } catch (Exception e) {
                    throw new XProcException(e);
                }
                finally {
                    out.close();
                }
            }
            catch (Exception e) {
                throw new XProcException(e);
            }
        } catch (Exception e) {
            throw new XProcException(e);
        } finally {
            xep.cleanup();
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText(href);
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }
}
