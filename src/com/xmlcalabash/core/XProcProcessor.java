package com.xmlcalabash.core;

import com.xmlcalabash.config.XProcConfigurer;
import com.xmlcalabash.util.DefaultXProcConfigurer;
import com.xmlcalabash.util.DefaultXProcMessageListener;
import com.xmlcalabash.util.StepErrorListener;
import com.xmlcalabash.util.URIUtils;
import com.xmlcalabash.util.XProcURIResolver;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.xml.sax.EntityResolver;

import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: ndw
 * Date: 2/12/13
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class XProcProcessor {
    protected Logger logger = Logger.getLogger(this.getClass().getName());
    private XProcConfigurer configurer = null;
    private XProcConfiguration config = null;
    private Processor processor = null;
    private XProcURIResolver uriResolver = null;
    private XProcMessageListener msgListener = null;
    private URI staticBaseURI = null;

    public XProcProcessor(XProcConfiguration config) {
        this.config = config;
        processor = config.getProcessor();

        staticBaseURI = URIUtils.cwdAsURI();

        uriResolver = new XProcURIResolver(this);
        try {
            if (config.uriResolver != null) {
                uriResolver.setUnderlyingURIResolver((URIResolver) Class.forName(config.uriResolver).newInstance());
            }
            if (config.entityResolver != null) {
                uriResolver.setUnderlyingEntityResolver((EntityResolver) Class.forName(config.entityResolver).newInstance());
            }

            if (config.errorListener != null) {
                msgListener = (XProcMessageListener) Class.forName(config.errorListener).newInstance();
            } else {
                msgListener = new DefaultXProcMessageListener();
            }
        } catch (Exception e) {
            throw new XProcException(e);
        }

        if (config.xprocConfigurer != null) {
            try {
                String className = config.xprocConfigurer;
                Constructor constructor = Class.forName(className).getConstructor(XProcProcessor.class);
                configurer = (XProcConfigurer) constructor.newInstance(this);
            } catch (Exception e) {
                throw new XProcException(e);
            }
        } else {
            configurer = new DefaultXProcConfigurer(this);
        }
        configurer.getXMLCalabashConfigurer().configProcessor(this);

        Configuration saxonConfig = processor.getUnderlyingConfiguration();
        saxonConfig.setURIResolver(uriResolver);
        saxonConfig.setErrorListener(new FatalStepErrorListener());

        for (String className : config.extensionFunctions) {
            try {
                Object def = Class.forName(className).newInstance();
                finer("Instantiated: " + className);
                if (def instanceof ExtensionFunctionDefinition)
                    processor.registerExtensionFunction((ExtensionFunctionDefinition) def);
                else if (def instanceof ExtensionFunction)
                    processor.registerExtensionFunction((ExtensionFunction) def);
                else
                    finer("Failed to instantiate extension function " + className + " because that class implements neither ExtensionFunction nor ExtensionFunctionDefinition.");
            } catch (NoClassDefFoundError ncdfe) {
                finer("Failed to instantiate extension function: " + className);
            } catch (Exception e) {
                finer("Failed to instantiate extension function: " + className);
            }
        }
    }

    public XProcConfiguration getConfiguration() {
        return config;
    }

    public Processor getProcessor() {
        return processor;
    }

    public XProcConfigurer getConfigurer() {
        return configurer;
    }

    public void setConfigurer(XProcConfigurer configurer) {
        this.configurer = configurer;
        configurer.getXMLCalabashConfigurer().configProcessor(this);
    }

    public XProcMessageListener getMessageListener() {
        return msgListener;
    }

    public void setMessageListener(XProcMessageListener listener) {
        msgListener = listener;
    }

    public XProcURIResolver getURIResolver() {
        return uriResolver;
    }

    public void setURIResolver(XProcURIResolver resolver) {
        uriResolver = resolver;
    }

    public XProcRuntime load(String uri) throws SaxonApiException {
        XProcRuntime runtime = new XProcRuntime(this);
        runtime.load(uri);
        return runtime;
    }

    public XProcRuntime use(XdmNode pipeline) throws SaxonApiException {
        XProcRuntime runtime = new XProcRuntime(this);
        runtime.use(pipeline);
        return runtime;
    }

    public boolean getSafeMode() {
        return config.safeMode;
    }

    public URI getStaticBaseURI() {
        return staticBaseURI;
    }

    public String getSendmailHost() {
        return config.mailHost;
    }

    public String getSendmailPort() {
        return config.mailPort;
    }

    public String getSendmailUsername() {
        return config.mailUser;
    }

    public String getSendmailPassword() {
        return config.mailPass;
    }

    public boolean getPSVISupported() {
        return config.schemaAware;
    }

    public boolean isStepAvailable(QName type) {
        return config.isStepAvailable(type);
    }

    public String implementation(QName type) {
        return config.implementation(type);
    }

    public XdmNode parse(String href, String base, boolean validate) {
        return uriResolver.parse(href, base, validate);
    }
    // ===========================================================
    // This logging stuff is still accessed through XProcRuntime
    // so that messages can be formatted in a common way and so
    // that errors can be trapped.

    public void error(Throwable error) {
        msgListener.error(error);
    }

    public void warning(String message) {
        msgListener.warning(null, null, message);
    }

    public void warning(Throwable error) {
        msgListener.warning(error);
    }

    public void info(String message) {
        msgListener.info(null, null, message);
    }

    public void fine(String message) {
        msgListener.fine(null, null, message);
    }

    public void finer(String message) {
        msgListener.finer(null, null, message);
    }

    public void finest(String message) {
        msgListener.finest(null, null, message);
    }

    // ===========================================================

    public boolean getAllowGeneralExpressions() {
        return config.extensionValues;
    }

    public boolean getAllowXPointerOnText() {
        return config.xpointerOnText;
    }

    public boolean transparentJSON() {
        return config.transparentJSON;
    }

    public String jsonFlavor() {
        return config.jsonFlavor;
    }

    public String htmlParser() {
        return config.htmlParser;
    }

    public boolean getUseXslt10Processor() {
        return config.useXslt10;
    }

    private class FatalStepErrorListener extends StepErrorListener {
        public FatalStepErrorListener() {
            // doesn't matter, it just goes bang
        }

        @Override
        public void warning(TransformerException e) throws TransformerException {
            throw new RuntimeException("Processor Error Listener Called");
        }

        @Override
        public void error(TransformerException e) throws TransformerException {
            throw new RuntimeException("Processor Error Listener Called");
        }

        @Override
        public void fatalError(TransformerException e) throws TransformerException {
            throw new RuntimeException("Processor Error Listener Called");
        }
    }
}
