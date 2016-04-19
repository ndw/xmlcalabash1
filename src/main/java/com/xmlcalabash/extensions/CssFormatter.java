package com.xmlcalabash.extensions;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;

import com.xmlcalabash.core.XMLCalabash;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.config.CssProcessor;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataWriter;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 21, 2008
 * Time: 6:59:07 PM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "cx:css-formatter",
        type = "{http://xmlcalabash.com/ns/extensions}css-formatter")

public class CssFormatter extends DefaultStep {
    private static final QName _href = new QName("","href");
    private static final QName _css = new QName("","css");
    private static final QName _content_type = new QName("","content-type");
    private ReadablePipe source = null;
    private ReadablePipe css = null;
    private WritablePipe result = null;
    private Properties options = new Properties();

    /* Creates a new instance of Unzip */
    public CssFormatter(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            css = pipe;
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        if (!"".equals(name.getNamespaceURI())) {
            throw new XProcException(step.getNode(), "The cx:css-formatter parameters are in no namespace: " + name + " (" + name.getNamespaceURI() + ")");
        }
        options.setProperty(name.getLocalName(), value.getString());
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        String cssClass = runtime.getConfiguration().cssProcessor;
        if (cssClass == null) {
            throw new XProcException("No CSS processor class defined");
        }

        final CssProcessor provider;
        try {
            provider = (CssProcessor) Class.forName(cssClass).newInstance();
            provider.initialize(runtime,step,options);
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            throw new XProcException(step.getNode(), "Failed to instantiate CSS provider");
        }

        while (css != null && css.moreDocuments()) {
            XdmNode style = css.read();
            provider.addStylesheet(style);
        }

        final String contentType;
        if (getOption(_content_type) != null) {
            contentType = getOption(_content_type).getString();
        } else {
            contentType = "application/pdf";
        }

        if (getOption(_css) != null) {
            String s = getOption(_css).getString();
            for (String css : s.split("\\s+")) {
                provider.addStylesheet(css);
            }
       }

        String href = getOption(_href).getString();
        String base = getOption(_href).getBaseURI().toASCIIString();

        try {
            DataStore store = runtime.getDataStore();
            URI id = store.writeEntry(href, base, contentType, new DataWriter() {
                public void store(OutputStream out) throws IOException {
                    try {
                        provider.format(source.read(),out,contentType);
                    } catch(SaxonApiException e) {
                        throw new IOException(e);
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
            if (e.getCause() instanceof SaxonApiException) {
                throw (SaxonApiException) e.getCause();
            }
            throw new XProcException(step.getNode(), "Failed to style with CSS document", e);
        }
    }
}
