package com.xmlcalabash.library;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;
import java.util.Vector;

import com.xmlcalabash.core.XMLCalabash;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataWriter;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 21, 2008
 * Time: 6:59:07 PM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "p:xsl-formatter",
        type = "{http://www.w3.org/ns/xproc}xsl-formatter")

public class XSLFormatter extends DefaultStep {
    private static final QName _href = new QName("","href");
    private static final QName _content_type = new QName("","content-type");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Properties options = new Properties();

    /* Creates a new instance of Unzip */
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
                    break;
                } catch (NoClassDefFoundError ncdfe) {
                    logger.debug("No FO processor class available: " + className);
                    if (runtime.getDebug()) {
                        ncdfe.printStackTrace();
                    }
                    pexcept = ncdfe;
                    provider = null;
                } catch (Exception e) {
                    logger.debug("Failed to instantiate FO processor class: " + className);
                    pexcept = e;
                    provider = null;
                }
            }
        }

        if (provider == null) {
            throw new XProcException(step.getNode(), "Failed to instantiate FO provider", pexcept);
        }

        final String contentType;
        if (getOption(_content_type) != null) {
            contentType = getOption(_content_type).getString();
        } else {
            contentType = "application/octet-stream";
        }

        String base = getOption(_href).getBaseURI().toASCIIString();
        String href = getOption(_href).getString();

        try {
            final FoProcessor processor = provider;
            DataStore store = runtime.getDataStore();
            URI id = store.writeEntry(href, base, contentType, new DataWriter() {
                public void store(OutputStream content) throws IOException {
                    OutputStream out = new BufferedOutputStream(content);
                    try {
                        processor.format(source.read(),out,contentType);
                    } catch (SaxonApiException e) {
                        throw new XProcException(step.getNode(), "Failed to process FO document", e);
                    } finally {
                        out.close();
                    }
                }
            });

            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(XProcConstants.c_result);
            tree.startContent();
            tree.addText(id.toASCIIString());
            tree.addEndElement();
            tree.endDocument();
            result.write(tree.getResult());
        } catch (XProcException e) {
            throw e;
        } catch (Exception e) {
            throw new XProcException(step.getNode(), "Failed to process FO document", e);
        }
    }
}
