package com.xmlcalabash.library;

import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.model.RuntimeValue;

import java.io.IOException;
import java.util.Properties;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.Vector;

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

        Vector<String> foClasses = new Vector<String> ();
        if (runtime.getConfiguration().foProcessor != null) {
            foClasses.add(runtime.getConfiguration().foProcessor);
        }
        foClasses.add("com.xmlcalabash.util.FoFOP");

        FoProcessor provider = null;
        Throwable pexcept = null;
        for (String className : foClasses) {
            if (provider == null) {
                try {
                    provider = (FoProcessor) Class.forName(className).newInstance();
                    provider.initialize(runtime,step,options);
                } catch (NoClassDefFoundError ncdfe) {
                    pexcept = ncdfe;
                    provider = null;
                } catch (Exception e) {
                    pexcept = e;
                    provider = null;
                }
            }
        }

        if (provider == null) {
            throw new XProcException(step.getNode(), "Failed to instantiate FO provider", pexcept);
        }

        String contentType = null;
        if (getOption(_content_type) != null) {
            contentType = getOption(_content_type).getString();
        }

        String href = getOption(_href).getBaseURI().resolve(getOption(_href).getString()).toASCIIString();
        String output = null;
        if (href.startsWith("file:/")) {
            output = URIUtils.getFile(href).getPath();
        } else {
            throw new XProcException(step.getNode(), "Don't know how to write p:xsl-formatter output to " + href);
        }

        OutputStream out = null;
        try {
            try {
                out = new BufferedOutputStream(new FileOutputStream(output));
                provider.format(source.read(),out,contentType);
            } catch (XProcException e) {
                throw e;
            } catch (Exception e) {
                throw new XProcException(step.getNode(), "Failed to process FO document", e);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            // Oh, nevermind if we couldn't close the file
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
