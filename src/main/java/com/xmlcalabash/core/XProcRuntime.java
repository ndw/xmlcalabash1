/*
 * XProcRuntime.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.xmlcalabash.core;

import com.nwalsh.annotations.SaxonExtensionFunction;
import com.xmlcalabash.config.XProcConfigurer;
import com.xmlcalabash.functions.BaseURI;
import com.xmlcalabash.functions.Cwd;
import com.xmlcalabash.functions.IterationPosition;
import com.xmlcalabash.functions.IterationSize;
import com.xmlcalabash.functions.ResolveURI;
import com.xmlcalabash.functions.StepAvailable;
import com.xmlcalabash.functions.SystemProperty;
import com.xmlcalabash.functions.ValueAvailable;
import com.xmlcalabash.functions.VersionAvailable;
import com.xmlcalabash.functions.XPathVersionAvailable;
import com.xmlcalabash.functions.XProcExtensionFunctionDefinition;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.FallbackDataStore;
import com.xmlcalabash.io.FileDataStore;
import com.xmlcalabash.io.HttpClientDataStore;
import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.URLDataStore;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.DeclarationScope;
import com.xmlcalabash.model.Parser;
import com.xmlcalabash.model.PipelineLibrary;
import com.xmlcalabash.runtime.XLibrary;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.runtime.XRootStep;
import com.xmlcalabash.runtime.XStep;
import com.xmlcalabash.util.DefaultXProcConfigurer;
import com.xmlcalabash.util.DefaultXProcMessageListener;
import com.xmlcalabash.util.Input;
import com.xmlcalabash.util.JSONtoXML;
import com.xmlcalabash.util.Output;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.TypeUtils;
import com.xmlcalabash.util.URIUtils;
import com.xmlcalabash.util.XProcSystemPropertySet;
import com.xmlcalabash.util.XProcURIResolver;
import com.xmlcalabash.util.XProcURIResolverX;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.type.Untyped;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import static java.lang.String.format;

/**
 *
 * @author ndw
 */
public class XProcRuntime implements DeclarationScope {
    protected Logger logger = LoggerFactory.getLogger(XProcRuntime.class);
    private Processor processor = null;
    private Parser parser = null;
    private XProcURIResolver uriResolver = null;
    private XProcConfiguration config = null;
    private QName errorCode = null;
    private XdmNode errorNode = null;
    private String errorMessage = null;
    private HashMap<QName, DeclareStep> declaredSteps = new HashMap<>();
    private DeclareStep pipeline = null;
    private XPipeline xpipeline = null;
    private static String episode = null;
    private HashMap<String,Vector<XdmNode>> collections = null;
    private URI staticBaseURI = null;
    private URI baseURI = null;
    private boolean allowGeneralExpressions = true;
    private boolean allowXPointerOnText = true;
    private boolean allowTextResults = true;
    private boolean transparentJSON = false;
    private boolean ignoreInvalidXmlBase = false;
    private String jsonFlavor = JSONtoXML.MARKLOGIC;
    private boolean useXslt10 = false;
    private boolean htmlSerializer = false;
    private XProcData xprocData = null;
    private XProcMessageListener msgListener = null;
    private PipelineLibrary standardLibrary = null;
    private HttpClient httpClient;
    private Map<String, CookieStore> cookieStores;
    private DataStore dataStore;
    private XProcConfigurer configurer = null;
    private String htmlParser = null;
    private Vector<XProcExtensionFunctionDefinition> exFuncs = new Vector<>();
    private final Vector<XProcSystemPropertySet> systemPropertySets = new Vector<>();
    private SerializationProperties defaultSerializationProperties = new SerializationProperties();

    private Output profile = null;
    private HashMap<XStep,Calendar> profileHash = null;
    private TreeWriter profileWriter = null;
    private QName profileProfile = new QName("http://xmlcalabash.com/ns/profile", "profile");
    private QName profileType = new QName("", "type");
    private QName profileName = new QName("", "name");
    private QName profileHref = new QName("", "href");
    private QName profileLine = new QName("", "line");
    private QName profileTime = new QName("http://xmlcalabash.com/ns/profile", "time");
    private String p_declare_step_clark = XProcConstants.p_declare_step.getClarkName();
    private String p_pipeline_clark = XProcConstants.p_pipeline.getClarkName();

    public XProcRuntime(XProcConfiguration config) {
        this.config = config;
        processor = config.getProcessor();
        logger.debug(getProductName() + " version " + getProductVersion());

        if (processor.getSaxonProductVersion().startsWith("9.9.0")
            || "9.9.1.1".equals(processor.getSaxonProductVersion())) {
            logger.warn(getProductName() + " is not compatible with Saxon version " + processor.getSaxonProductVersion());
        }

        if (config.xprocConfigurer != null) {
            try {
                String className = config.xprocConfigurer;
                Constructor<? extends XProcConfigurer> constructor = Class.forName(className).asSubclass(XProcConfigurer.class).getConstructor(XProcRuntime.class);
                configurer = constructor.newInstance(this);
            } catch (Exception e) {
                throw new XProcException(e);
            }
        } else {
            configurer = new DefaultXProcConfigurer(this);
        }

        xprocData = new XProcData(this);

        exFuncs.add(new Cwd(this));
        exFuncs.add(new BaseURI(this));
        exFuncs.add(new ResolveURI(this));
        exFuncs.add(new SystemProperty(this));
        exFuncs.add(new StepAvailable(this));
        exFuncs.add(new IterationSize(this));
        exFuncs.add(new IterationPosition(this));
        exFuncs.add(new ValueAvailable(this));
        exFuncs.add(new VersionAvailable(this));
        exFuncs.add(new XPathVersionAvailable(this));

        for (XProcExtensionFunctionDefinition xf : exFuncs) {
            processor.registerExtensionFunction(xf);
        }

        Configuration saxonConfig = processor.getUnderlyingConfiguration();
        uriResolver = new XProcURIResolver(this);

        // Make sure that the Saxon processor uses *our* resolver for everything.
        // Unless the user has already provided a class of their own, of course.
        XProcURIResolverX saxonFakeStaticResolver = new XProcURIResolverX();
        String saxonFakeClassName = saxonFakeStaticResolver.getClass().getName();
        saxonFakeStaticResolver.setRealResolver(uriResolver);
        if (!config.setSaxonProperties.contains(FeatureKeys.ENTITY_RESOLVER_CLASS)) {
            saxonConfig.setConfigurationProperty(FeatureKeys.ENTITY_RESOLVER_CLASS, saxonFakeClassName);
        }
        if (!config.setSaxonProperties.contains(FeatureKeys.URI_RESOLVER_CLASS)) {
            saxonConfig.setConfigurationProperty(FeatureKeys.URI_RESOLVER_CLASS, saxonFakeClassName);
        }

        saxonConfig.setURIResolver(uriResolver);
        staticBaseURI = URIUtils.cwdAsURI();

        try {
            if (config.uriResolver != null) {
                uriResolver.setUnderlyingURIResolver(Class.forName(config.uriResolver).asSubclass(URIResolver.class).getDeclaredConstructor().newInstance());
            }
            if (config.entityResolver != null) {
                uriResolver.setUnderlyingEntityResolver(Class.forName(config.entityResolver).asSubclass(EntityResolver.class).getDeclaredConstructor().newInstance());
            }

            if (config.errorListener != null) {
                msgListener = Class.forName(config.errorListener).asSubclass(XProcMessageListener.class).getDeclaredConstructor().newInstance();
            } else {
                msgListener = new DefaultXProcMessageListener();
            }
        } catch (Exception e) {
            throw new XProcException(e);
        }

        if (config.catalogs.size() > 0) {
            uriResolver.addCatalogs(config.catalogs);
        }

        // FIXME: s10
        // StepErrorListener errListener = new StepErrorListener(this);
        // saxonConfig.setErrorListener(errListener);

        allowGeneralExpressions = config.extensionValues;
        allowXPointerOnText = config.xpointerOnText;
        allowTextResults = config.allowTextResults;
        transparentJSON = config.transparentJSON;
        ignoreInvalidXmlBase = config.ignoreInvalidXmlBase;
        jsonFlavor = config.jsonFlavor;
        useXslt10 = config.useXslt10;
        htmlSerializer = config.htmlSerializer;

        if (config.profile != null) {
            profile = config.profile;
            profileHash = new HashMap<XStep, Calendar> ();
            profileWriter = new TreeWriter(this);
            profileWriter.startDocument(URI.create("http://xmlcalabash.com/output/profile.xml"));
        }

        String warnLevel = "INFO";
        for (String className : config.extensionFunctions.keySet()) {
            try {
                SaxonExtensionFunction annotation = config.extensionFunctions.get(className);
                if (annotation != null) {
                    warnLevel = annotation.warnLevel().toUpperCase();
                }

                Object def = null;

                try {
                    def = Class.forName(className).getDeclaredConstructor().newInstance();
                } catch (Throwable e) {
                    logger.trace("Attempting to instantiate " + className + " with processor context");
                    Class<?> cl = Class.forName(className);
                    Constructor<?> cons = cl.getConstructor(Processor.class);
                    def = cons.newInstance(processor);
                }

                logger.trace("Instantiated: " + className);
                if (def instanceof ExtensionFunctionDefinition) {
                    processor.registerExtensionFunction((ExtensionFunctionDefinition) def);
                } else if (def instanceof ExtensionFunction)
                    processor.registerExtensionFunction((ExtensionFunction) def);
                else
                    logger.info("Failed to instantiate extension function " + className + " because that class implements neither ExtensionFunction nor ExtensionFunctionDefinition.");
            } catch (Throwable e) {
                if ("INFO".equals(warnLevel)) {
                    logger.info("Failed to instantiate extension function: " + className);
                } else if ("DEBUG".equals(warnLevel)) {
                    logger.debug("Failed to instantiate extension function: " + className);
                } else if ("TRACE".equals(warnLevel)) {
                    logger.trace("Failed to instantiate extension function: " + className);
                } else {
                    logger.error("Failed to instantiate extension function: " + className);
                }
            }
        }

        htmlParser = config.htmlParser;
        addSystemPropertySet(XProcSystemPropertySet.BUILTIN);

        reset();

        initializeSteps();
    }

    public XProcRuntime(XProcRuntime runtime) {
        processor = runtime.processor;
        uriResolver = runtime.uriResolver;
        config = runtime.config;
        staticBaseURI = runtime.staticBaseURI;
        useXslt10 = runtime.useXslt10;
        htmlSerializer = runtime.htmlSerializer;
        msgListener = runtime.msgListener;
        standardLibrary = runtime.standardLibrary;
        httpClient = runtime.httpClient;
        cookieStores = runtime.cookieStores;
        configurer = runtime.configurer;
        allowGeneralExpressions = runtime.allowGeneralExpressions;
        allowXPointerOnText = runtime.allowXPointerOnText;
        transparentJSON = runtime.transparentJSON;
        ignoreInvalidXmlBase = runtime.ignoreInvalidXmlBase;
        jsonFlavor = runtime.jsonFlavor;
        profile = runtime.profile;

        exFuncs.add(new Cwd(this));
        exFuncs.add(new BaseURI(this));
        exFuncs.add(new ResolveURI(this));
        exFuncs.add(new SystemProperty(this));
        exFuncs.add(new StepAvailable(this));
        exFuncs.add(new IterationSize(this));
        exFuncs.add(new IterationPosition(this));
        exFuncs.add(new ValueAvailable(this));
        exFuncs.add(new VersionAvailable(this));
        exFuncs.add(new XPathVersionAvailable(this));

        for (XProcExtensionFunctionDefinition xf : exFuncs) {
            processor.registerExtensionFunction(xf);
        }

        reset();

        initializeSteps();
    }

    public void resetExtensionFunctions() {
        for (XProcExtensionFunctionDefinition xf : exFuncs) {
            processor.registerExtensionFunction(xf);
        }
    }

    private void initializeSteps() {
        for (Class<?> klass : config.implementations.values()) {
            try {
                Method config = klass.getMethod("configureStep", XProcRuntime.class);
                config.invoke(null, this);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // nevermind
            } catch (Exception e) {
                System.err.println("Caught: " + e);
            }
        }
    }

    public void close() {
        HttpClientUtils.closeQuietly(httpClient);
        httpClient = null;

        if (exFuncs != null) {
            for (XProcExtensionFunctionDefinition xf : exFuncs) {
                xf.close();
            }
        }
        exFuncs = null;
    }

    public SerializationProperties getDefaultSerializationProperties() { return defaultSerializationProperties; }

    public XProcConfigurer getConfigurer() {
        return configurer;
    }

    public void setConfigurer(XProcConfigurer configurer) {
        this.configurer = configurer;
    }

    public XProcData getXProcData() {
        return xprocData;
    }

    public boolean getDebug() {
        return config.debug;
    }

    public boolean getShowMessages() {
        return config.showMessages;
    }

    public Output getProfile() {
        return profile;
    }

    public void setProfile(Output profile) {
        this.profile = profile;
    }

    public URI getStaticBaseURI() {
        return staticBaseURI;
    }

    public void setStaticBaseURI(URI staticBaseURI) {
        this.staticBaseURI = staticBaseURI;
    }

    public URI getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(URI baseURI) {
        this.baseURI = baseURI;
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

    public void setURIResolver(URIResolver resolver) {
        uriResolver.setUnderlyingURIResolver(resolver);
    }

    public void setEntityResolver(EntityResolver resolver) {
        uriResolver.setUnderlyingEntityResolver(resolver);
    }

    public synchronized DataStore getDataStore() {
        if (dataStore == null) {
            DataStore fallback = new URLDataStore(new FallbackDataStore());
            if (!getSafeMode()) {
                fallback = new FileDataStore(fallback);
            }
            dataStore = new HttpClientDataStore(getHttpClient(), fallback);
        }
        return dataStore;
    }

    public synchronized void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public XProcURIResolver getResolver() {
        return uriResolver;
    }

    public XProcMessageListener getMessageListener() {
      return msgListener;
    }

    public void setMessageListener(XProcMessageListener listener) {
      msgListener = listener;
    }

    public void setCollection(URI href, Vector<XdmNode> docs) {
        if (collections == null) {
            collections = new HashMap<String,Vector<XdmNode>> ();
        }
        collections.put(href.toASCIIString(), docs);
    }

    public Vector<XdmNode> getCollection(URI href) {
        if (collections == null) {
            return null;
        }
        if (collections.containsKey(href.toASCIIString())) {
            return collections.get(href.toASCIIString());
        }
        return null;
    }

    public boolean getSafeMode() {
        return config.safeMode;
    }

    public boolean getAllowGeneralExpressions() {
        return allowGeneralExpressions;
    }

    public boolean getAllowXPointerOnText() {
        return allowXPointerOnText;
    }

    public boolean getAllowTextResults() {
        return allowTextResults;
    }

    public boolean getIgnoreInvalidXmlBase() {
        return ignoreInvalidXmlBase;
    }

    public boolean transparentJSON() {
        return transparentJSON;
    }

    public String jsonFlavor() {
        return jsonFlavor;
    }

    public String htmlParser() {
        return htmlParser;
    }

    public boolean getUseXslt10Processor() {
        return useXslt10;
    }

    public boolean getHtmlSerializer() {
        return htmlSerializer;
    }

    public void cache(XdmNode doc, URI baseURI) {
        uriResolver.cache(doc, baseURI);
    }

    public XProcConfiguration getConfiguration() {
        return config;
    }

    public Parser getParser() {
        return parser;
    }

    public String getEpisode() {
        if (episode == null) {
            episode = "CB-" + java.util.UUID.randomUUID().toString();
        }

        return episode;
    }

    public String getLanguage() {
        // Translate _ to - for compatibility with xml:lang
        return Locale.getDefault().toString().replace('_', '-');

    }

    public String getProductName() {
        return "XML Calabash";
    }

    public String getProductVersion() {
        String sver = processor.getSaxonProductVersion();
        String sed = processor.getUnderlyingConfiguration().getEditionCode();
        return XProcConstants.XPROC_VERSION + " (for Saxon " + sver + "/" + sed + ")";
    }

    public String getVendor() {
        return "Norman Walsh";
    }

    public String getVendorURI() {
        return "http://xmlcalabash.com/";
    }

    public String getXProcVersion() {
        return "1.0";
    }

    public String getXPathVersion() {
        return "2.0";
    }

    public boolean getPSVISupported() {
        return config.schemaAware;
    }

    private synchronized void reset() {
        errorCode = null;
        errorMessage = null;
        declaredSteps = new HashMap<QName,DeclareStep> ();
        //explicitDeclarations = false;
        pipeline = null;
        xpipeline = null;
        episode = null;
        collections = null;
        cookieStores = new HashMap<String, CookieStore>();

        xprocData = new XProcData(this);

        parser = new Parser(this);
        try {
            standardLibrary = parser.loadStandardLibrary();
            if (errorCode != null) {
                throw new XProcException(errorCode, errorMessage);
            }
        } catch (FileNotFoundException ex) {
            throw new XProcException(XProcConstants.dynamicError(9), ex);
        } catch (URISyntaxException ex) {
            throw new XProcException(XProcConstants.dynamicError(9), ex);
        } catch (SaxonApiException ex) {
            throw new XProcException(XProcConstants.dynamicError(9), ex);
        }

        if (profile != null) {
            profileHash = new HashMap<XStep, Calendar>();
            profileWriter = new TreeWriter(this);
            profileWriter.startDocument(URI.create("http://xmlcalabash.com/output/profile.xml"));
        }
    }

    public PipelineLibrary getStandardLibrary() {
        return standardLibrary;
    }

    // FIXME: This design sucks
    public XPipeline load(Input pipeline) throws SaxonApiException {
        String uri;
        switch (pipeline.getKind()) {
            case URI:
                uri = pipeline.getUri();
                break;

            case INPUT_STREAM:
                uri = pipeline.getInputStreamUri();
                break;

            default:
                throw new UnsupportedOperationException(format("Unsupported pipeline kind '%s'", pipeline.getKind()));
        }

        for (String map : config.loaders.keySet()) {
            boolean data = map.startsWith("data:");
            String pattern = map.substring(5);
            if (uri.matches(pattern)) {
                return runPipelineLoader(pipeline, config.loaders.get(map), data);
            }
        }

        try {
            return _load(pipeline);
        } catch (SaxonApiException sae) {
            error(sae);
            throw sae;
        } catch (XProcException xe) {
            error(xe);
            throw xe;
        } catch (IOException ioe) {
            error(ioe);
            throw new XProcException(ioe);
        }
    }

    public XPathCompiler newXPathCompiler(URI baseURI) {
        return newXPathCompiler(baseURI, null);
    }

    public XPathCompiler newXPathCompiler(URI baseURI, HashMap<String,String> nsbindings) {
        XPathCompiler compiler = processor.newXPathCompiler();
        compiler.setSchemaAware(processor.isSchemaAware());
        if (baseURI != null) {
            if (baseURI.isAbsolute()) {
                compiler.setBaseURI(baseURI);
            } else {
                compiler.setBaseURI(getBaseURI().resolve(baseURI));
            }
        }
        if (nsbindings != null) {
            for (String prefix : nsbindings.keySet()) {
                compiler.declareNamespace(prefix, nsbindings.get(prefix));
            }
        }
        return compiler;
    }

    private XPipeline _load(Input pipelineInput) throws SaxonApiException, IOException {
        reset();
        configurer.getXMLCalabashConfigurer().configRuntime(this);
        switch (pipelineInput.getKind()) {
            case URI:
                if (baseURI == null) {
                    pipeline = parser.loadPipeline(pipelineInput.getUri());
                } else {
                    pipeline = parser.loadPipeline(pipelineInput.getUri(), baseURI.toASCIIString());
                }
                break;

            case INPUT_STREAM:
                pipeline = parser.loadPipeline(pipelineInput.getInputStream(), pipelineInput.getInputStreamUri());
                break;

            default:
                throw new UnsupportedOperationException(format("Unsupported pipeline kind '%s'", pipelineInput.getKind()));
        }
        if (errorCode != null) {
            throw new XProcException(errorCode, errorMessage);
        }

        XRootStep root = new XRootStep(this);
        DeclareStep decl = pipeline.getDeclaration();
        decl.setup();

        if (errorCode != null) {
            throw new XProcException(errorCode, errorNode, errorMessage);
        }

        xpipeline = new XPipeline(this, pipeline, root);
        xpipeline.instantiate(decl);

        if (errorCode != null) {
            throw new XProcException(errorCode, errorMessage);
        }

        return xpipeline;
    }

    // FIXME: This design sucks
    public XPipeline use(XdmNode p_pipeline) throws SaxonApiException {
        try {
            return _use(p_pipeline);
        } catch (SaxonApiException sae) {
            error(sae);
            throw sae;
        } catch (XProcException xe) {
            error(xe);
            throw xe;
        }
    }
    private XPipeline _use(XdmNode p_pipeline) throws SaxonApiException {
        reset();
        configurer.getXMLCalabashConfigurer().configRuntime(this);
        pipeline = parser.usePipeline(p_pipeline);
        if (errorCode != null) {
            throw new XProcException(errorCode, errorMessage);
        }

        XRootStep root = new XRootStep(this);
        DeclareStep decl = pipeline.getDeclaration();
        decl.setup();

        if (errorCode != null) {
            throw new XProcException(errorCode, errorMessage);
        }

        xpipeline = new XPipeline(this, pipeline, root);
        xpipeline.instantiate(decl);

        if (errorCode != null) {
            throw new XProcException(errorCode, errorMessage);
        }

        return xpipeline;
    }

    // FIXME: This design sucks
    public XLibrary loadLibrary(Input library) throws SaxonApiException {
        String libraryURI;
        switch (library.getKind()) {
            case URI:
                libraryURI = library.getUri();
                break;

            case INPUT_STREAM:
                libraryURI = library.getInputStreamUri();
                break;

            default:
                throw new UnsupportedOperationException(format("Unsupported library kind '%s'", library.getKind()));
        }

        for (String map : config.loaders.keySet()) {
            boolean data = map.startsWith("data:");
            String pattern = map.substring(5);
            if (libraryURI.matches(pattern)) {
                return runLibraryLoader(library, config.loaders.get(map), data);
            }
        }

        try {
            return _loadLibrary(library);
        } catch (SaxonApiException sae) {
            error(sae);
            throw sae;
        } catch (XProcException xe) {
            error(xe);
            throw xe;
        } catch (IOException ioe) {
            error(ioe);
            throw new XProcException(ioe);
        }
    }

    private XLibrary _loadLibrary(Input library) throws SaxonApiException, IOException {
        PipelineLibrary plibrary;
        switch (library.getKind()) {
            case URI:
                plibrary = parser.loadLibrary(library.getUri());
                break;

            case INPUT_STREAM:
                plibrary = parser.loadLibrary(library.getInputStream(), library.getInputStreamUri());
                break;

            default:
                throw new UnsupportedOperationException(format("Unsupported library kind '%s'", library.getKind()));
        }

        if (errorCode != null) {
            throw new XProcException(errorCode, errorMessage);
        }

        XLibrary xlibrary = new XLibrary(this, plibrary);

        if (errorCode != null) {
            throw new XProcException(errorCode, errorMessage);
        }

        return xlibrary;
    }

    // FIXME: This design sucks
    public XLibrary useLibrary(XdmNode library) throws SaxonApiException {
        try {
            return _useLibrary(library);
        } catch (SaxonApiException sae) {
            error(sae);
            throw sae;
        } catch (XProcException xe) {
            error(xe);
            throw xe;
        }
    }

    private XLibrary _useLibrary(XdmNode library) throws SaxonApiException {
        PipelineLibrary plibrary = parser.useLibrary(library);
        if (errorCode != null) {
            throw new XProcException(errorCode, errorMessage);
        }

        XLibrary xlibrary = new XLibrary(this, plibrary);

        if (errorCode != null) {
            throw new XProcException(errorCode, errorMessage);
        }

        return xlibrary;
    }

    private XPipeline runPipelineLoader(Input pipeline, String loaderURI, boolean data) throws SaxonApiException {
        XdmNode pipeDoc = runLoader(pipeline, loaderURI, data);
        return use(pipeDoc);
    }

    private XLibrary runLibraryLoader(Input library, String loaderURI, boolean data) throws SaxonApiException {
        XdmNode libDoc = runLoader(library, loaderURI, data);
        return useLibrary(libDoc);
    }

    private XdmNode runLoader(Input pipeline, String loaderURI, boolean data) throws SaxonApiException {
        XPipeline loader = null;

        try {
            loader = _load(new Input(loaderURI));
        } catch (SaxonApiException | XProcException sae) {
            error(sae);
            throw sae;
        } catch (IOException ioe) {
            error(ioe);
            throw new XProcException(ioe);
        }

        XdmNode pipeDoc = null;
        switch (pipeline.getKind()) {
            case URI:
                if (data) {
                    ReadableData rdata = new ReadableData(this, XProcConstants.c_result, getStaticBaseURI().resolve(pipeline.getUri()).toASCIIString(), "text/plain");
                    pipeDoc = rdata.read();
                } else {
                    pipeDoc = parse(pipeline.getUri(), getStaticBaseURI().toASCIIString());
                }
                break;

            case INPUT_STREAM:
                if (data) {
                    ReadableData rdata = new ReadableData(this, XProcConstants.c_result, pipeline.getInputStream(), "text/plain");
                    pipeDoc = rdata.read();
                } else {
                    pipeDoc = parse(new InputSource(pipeline.getInputStream()));
                }
                break;

            default:
                throw new UnsupportedOperationException(format("Unsupported pipeline kind '%s'", pipeline.getKind()));
        }

        loader.clearInputs("source");
        loader.writeTo("source", pipeDoc);
        loader.run();
        ReadablePipe xformed = loader.readFrom("result");
        pipeDoc = xformed.read();

        reset();
        return pipeDoc;
    }

    public Processor getProcessor() {
        return processor;
    }

    public XdmNode parse(String uri, String base) {
        return parse(uri, base, false);
    }

    public XdmNode parse(String uri, String base, boolean validate) {
        return uriResolver.parse(uri, base, validate);
    }

    public XdmNode parse(InputSource isource) {
        return uriResolver.parse(isource);
    }

    public void declareStep(QName name, DeclareStep step) {
        DeclareStep d = getDeclaration(name);
        if (d != null) {
            if (!d.equals(step))
                throw new XProcException(step, "Duplicate step type: " + name);
        } else {
            declaredSteps.put(name, step);
        }
    }

    public DeclareStep getDeclaration(QName type) {
        DeclareStep decl = null;
        if (standardLibrary != null)
            decl = standardLibrary.getDeclaration(type);
        DeclareStep d = declaredSteps.get(type);
        if (d != null) {
            if (decl == null)
                decl = d;
            else
                throw new XProcException(d, "Duplicate step type: " + type);
        }
        return decl;
    }

    public Set<QName> getInScopeTypes() {
        Set<QName> decls = new HashSet<>();
        decls.addAll(declaredSteps.keySet());
        if (standardLibrary != null)
            decls.addAll(standardLibrary.getInScopeTypes());
        return decls;
    }

    public synchronized CookieStore getCookieStore(String key) {
        if (cookieStores.containsKey(key))
            return cookieStores.get(key);
        BasicCookieStore cookieStore = new BasicCookieStore();
        cookieStores.put(key, cookieStore);
        return cookieStore;
    }

    public synchronized void setCookieStore(String key, CookieStore cookieStore) {
        if (cookieStore == null) {
            removeCookieStore(key);
        } else {
            this.cookieStores.put(key, cookieStore);
        }
    }

    public synchronized void removeCookieStore(String key) {
        this.cookieStores.remove(key);
    }

    public synchronized HttpClient getHttpClient() {
    	if (this.httpClient == null) {
    	    HttpClientBuilder builder = HttpClientBuilder.create();
            // Provide custom retry handler is necessary
            builder.setRetryHandler(new StandardHttpRequestRetryHandler(3, false));
            return this.httpClient = builder.build();
    	} else {
    		return httpClient;
    	}
    }

    public synchronized void setHttpClient(HttpClient client) {
        this.httpClient = client;
    }

    public QName getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // ===========================================================
    // This logging stuff is still accessed through XProcRuntime
    // so that messages can be formatted in a common way and so
    // that errors can be trapped.

    public void error(XProcRunnable step, XdmNode node, String message, QName code) {
        if (errorCode == null) {
            errorCode = code;
            errorNode = node;
            errorMessage = message;
        }

        msgListener.error(step, node, message, code);
    }

    public void error(Throwable error) {
        msgListener.error(error);
    }

    public void warning(XProcRunnable step, XdmNode node, String message) {
        msgListener.warning(step, node, message);
    }

    public void warning(Throwable error) {
        msgListener.warning(error);
    }

    public void info(XProcRunnable step, XdmNode node, String message) {
        msgListener.info(step, node, message);
    }

    // ===========================================================

    private Stack<XStep> runningSteps = new Stack<XStep>();

    public void start(XStep step) {
        runningSteps.push(step);

        if (profile == null) {
            return;
        }

        boolean first = profileHash.isEmpty();

        Calendar start = GregorianCalendar.getInstance();
        profileHash.put(step, start);
        
        AttributeMap profileAttr = EmptyAttributeMap.getInstance();

        if (first) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "timestamp"), df.format(new Date())));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "episode"), getEpisode()));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "language"), getLanguage()));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "product-name"), getProductName()));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "product-version"), getProductVersion()));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "product-vendor"), getVendor()));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "product-vendor-uri"), getVendorURI()));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "xproc-version"), getXProcVersion()));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "xpath-version"), getXPathVersion()));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(new QName("", "psvi-supported"), ""+getPSVISupported()));
        }

        String name = step.getType().getClarkName();
        if ((p_declare_step_clark.equals(name) || p_pipeline_clark.equals(name))
                && step.getType() != null
                && step.getStep().getDeclaredType() != null) {
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(profileType, step.getStep().getDeclaredType().getClarkName()));
        } else {
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(profileType, name));
        }

        profileAttr = profileAttr.put(TypeUtils.attributeInfo(profileName, step.getStep().getName()));
        if (step.getStep().getNode() != null) {
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(profileHref, step.getStep().xplFile()));
            profileAttr = profileAttr.put(TypeUtils.attributeInfo(profileLine, ""+step.getStep().xplLine()));
        }

        profileWriter.addStartElement(profileProfile, profileAttr);
    }

    public XStep runningStep() {
        return runningSteps.peek();
    }

    public void finish(XStep step) {
        runningSteps.pop();

        if (profile == null) {
            return;
        }

        Calendar start = profileHash.get(step);
        long time = GregorianCalendar.getInstance().getTimeInMillis() - start.getTimeInMillis();
        profileHash.remove(step);

        profileWriter.addStartElement(TypeUtils.fqName(profileTime), Untyped.INSTANCE);
        profileWriter.addText("" + time);
        profileWriter.addEndElement();
        profileWriter.addEndElement();

        if (profileHash.isEmpty()) {
            profileWriter.endDocument();
            XdmNode profile = profileWriter.getResult();

            InputStream xsl = getClass().getResourceAsStream("/etc/patch-profile.xsl");
            if (xsl == null) {
                throw new UnsupportedOperationException("Failed to load profile_patch.xsl from JAR file.");
            }

            try {
                XsltCompiler compiler = getProcessor().newXsltCompiler();
                compiler.setSchemaAware(false);
                XsltExecutable exec = compiler.compile(new SAXSource(new InputSource(xsl)));
                XsltTransformer transformer = exec.load();
                transformer.setInitialContextNode(profile);
                XdmDestination result = new XdmDestination();
                transformer.setDestination(result);
                transformer.transform();

                Serializer serializer = getProcessor().newSerializer();
                serializer.setOutputProperty(Serializer.Property.INDENT, "yes");

                OutputStream outstr = null;
                try {
                    switch (this.profile.getKind()) {
                    case URI:
                        URI furi = URI.create(this.profile.getUri());
                        outstr = new FileOutputStream(new File(furi));
                        break;

                    case OUTPUT_STREAM:
                        outstr = this.profile.getOutputStream();
                        break;

                    default:
                        throw new UnsupportedOperationException(format("Unsupported profile kind '%s'", this.profile.getKind()));
                    }

                    serializer.setOutputStream(outstr);
                    S9apiUtils.serialize(this, result.getXdmNode(), serializer);
                } finally {
                    if (outstr != null && !System.out.equals(outstr) && !System.err.equals(outstr)) {
                        outstr.close();
                    }
                }

                profileWriter = new TreeWriter(this);
                profileWriter.startDocument(URI.create("http://xmlcalabash.com/output/profile.xml"));
            } catch (SaxonApiException | IOException sae) {
                throw new XProcException(sae);
            }
        }
    }

    /**
     * Registers an {@code XProcSystemPropertySet}.
     * It will be consulted whenever the
     * <a href="http://www.w3.org/TR/xproc/#f.system-property">{@code p:system-property}</a> function is evaluated.
     *
     * <p>The {@linkplain com.xmlcalabash.util.XProcSystemPropertySet#BUILTIN built-in}
     * {@code XProcSystemPropertySet} is added automatically.
     * Other property sets may be added with this method by applications using XML Calabash.</p>
     *
     * @see #getSystemProperty
     * @param systemPropertySet The set of values to add.
     */
    public void addSystemPropertySet(XProcSystemPropertySet systemPropertySet) {
        systemPropertySets.add(systemPropertySet);
    }

    /**
     * Looks up a <a href="http://www.w3.org/TR/xproc/#f.system-property">system property</a> by the given name.
     * If no such system property is found, this method returns {@code null}.
     *
     * <p>This method consults {@linkplain com.xmlcalabash.util.XProcSystemPropertySet#BUILTIN the built-in}
     * {@link com.xmlcalabash.util.XProcSystemPropertySet}, and any other
     * {@code XProcSystemPropertySet}s that have been {@linkplain #addSystemPropertySet registered}.</p>
     *
     * @see #addSystemPropertySet
     * @see com.xmlcalabash.util.XProcSystemPropertySet#systemProperty
     * @param propertyName the name of the system property to look up
     * @return the string value of that system property, or {@code null}
     * @throws XProcException if any error occurs
     */
    public String getSystemProperty(QName propertyName) throws XProcException {
        synchronized (systemPropertySets) {
            for (XProcSystemPropertySet propSet : systemPropertySets) {
                String value = propSet.systemProperty(this, propertyName);
                if (value != null)
                    return value;
            }
        }

        return null;
    }
}
