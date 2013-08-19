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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

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
import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.Parser;
import com.xmlcalabash.model.PipelineLibrary;
import com.xmlcalabash.runtime.XLibrary;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.runtime.XRootStep;
import com.xmlcalabash.runtime.XStep;
import com.xmlcalabash.util.DefaultXProcConfigurer;
import com.xmlcalabash.util.DefaultXProcMessageListener;
import com.xmlcalabash.util.JSONtoXML;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.StepErrorListener;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.URIUtils;
import com.xmlcalabash.util.XProcURIResolver;

/**
 *
 * @author ndw
 */
public class XProcRuntime {
    protected Logger logger = Logger.getLogger("com.xmlcalabash");
    private Processor processor = null;
    private Parser parser = null;
    private XProcURIResolver uriResolver = null;
    private XProcConfiguration config = null;
    private QName errorCode = null;
    private XdmNode errorNode = null;
    private String errorMessage = null;
    private Hashtable<QName, DeclareStep> declaredSteps = new Hashtable<QName,DeclareStep> ();
    private DeclareStep pipeline = null;
    private XPipeline xpipeline = null;
    private static String episode = null;
    private Hashtable<String,Vector<XdmNode>> collections = null;
    private URI staticBaseURI = null;
    private boolean allowGeneralExpressions = true;
    private boolean allowXPointerOnText = true;
    private boolean transparentJSON = false;
    private String jsonFlavor = JSONtoXML.MARKLOGIC;
    private boolean useXslt10 = false;
    private XProcData xprocData = null;
    private Logger log = null;
    private XProcMessageListener msgListener = null;
    private PipelineLibrary standardLibrary = null;
    private XLibrary xStandardLibrary = null;
    private HttpClient httpClient;
    private Map<String, CookieStore> cookieStores;
    private XProcConfigurer configurer = null;
    private String htmlParser = null;
    private Vector<XProcExtensionFunctionDefinition> exFuncs = new Vector<XProcExtensionFunctionDefinition>();

    private String profileFile = null;
    private Hashtable<XStep,Calendar> profileHash = null;
    private TreeWriter profileWriter = null;
    private QName profileProfile = new QName("http://xmlcalabash.com/ns/profile", "profile");
    private QName profileType = new QName("", "type");
    private QName profileName = new QName("", "name");
    private QName profileTime = new QName("http://xmlcalabash.com/ns/profile", "time");

    public XProcRuntime(XProcConfiguration config) {
        this.config = config;
        processor = config.getProcessor();

        if (config.xprocConfigurer != null) {
            try {
                String className = config.xprocConfigurer;
                Constructor constructor = Class.forName(className).getConstructor(XProcRuntime.class);
                configurer = (XProcConfigurer) constructor.newInstance(this);
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

        log = Logger.getLogger(this.getClass().getName());

        Configuration saxonConfig = processor.getUnderlyingConfiguration();
        uriResolver = new XProcURIResolver(this);
        saxonConfig.setURIResolver(uriResolver);
        staticBaseURI = URIUtils.cwdAsURI();

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

        processor.getUnderlyingConfiguration().setURIResolver(uriResolver);

        StepErrorListener errListener = new StepErrorListener(this);
        saxonConfig.setErrorListener(errListener);

        allowGeneralExpressions = config.extensionValues;
        allowXPointerOnText = config.xpointerOnText;
        transparentJSON = config.transparentJSON;
        jsonFlavor = config.jsonFlavor;
        useXslt10 = config.useXslt10;

        if (config.profileFile != null) {
            profileFile = config.profileFile;
            profileHash = new Hashtable<XStep, Calendar> ();
            profileWriter = new TreeWriter(this);
            try {
                profileWriter.startDocument(new URI("http://xmlcalabash.com/output/profile.xml"));
            } catch (URISyntaxException use) {
                // nop;
            }
        }

        for (String className : config.extensionFunctions) {
            try {
                Object def = Class.forName(className).newInstance();
                finer(null, null, "Instantiated: " + className);
                if (def instanceof ExtensionFunctionDefinition)
                    processor.registerExtensionFunction((ExtensionFunctionDefinition) def);
                else if (def instanceof ExtensionFunction)
                    processor.registerExtensionFunction((ExtensionFunction) def);
                else
                    finer(null, null, "Failed to instantiate extension function " + className + " because that class implements neither ExtensionFunction nor ExtensionFunctionDefinition.");
            } catch (NoClassDefFoundError ncdfe) {
                finer(null, null, "Failed to instantiate extension function: " + className);
            } catch (Exception e) {
                finer(null, null, "Failed to instantiate extension function: " + className);
            }
        }

        htmlParser = config.htmlParser;

        reset();
    }

    public XProcRuntime(XProcRuntime runtime) {
        processor = runtime.processor;
        uriResolver = runtime.uriResolver;
        config = runtime.config;
        staticBaseURI = runtime.staticBaseURI;
        useXslt10 = runtime.useXslt10;
        log = runtime.log;
        msgListener = runtime.msgListener;
        standardLibrary = runtime.standardLibrary;
        xStandardLibrary = runtime.xStandardLibrary;
        httpClient = runtime.httpClient;
        cookieStores = runtime.cookieStores;
        configurer = runtime.configurer;
        allowGeneralExpressions = runtime.allowGeneralExpressions;
        allowXPointerOnText = runtime.allowXPointerOnText;
        transparentJSON = runtime.transparentJSON;
        jsonFlavor = runtime.jsonFlavor;
        profileFile = runtime.profileFile;

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

        reset();
    }

    public void resetExtensionFunctions() {
        for (XProcExtensionFunctionDefinition xf : exFuncs) {
            processor.registerExtensionFunction(xf);
        }
    }

    public void close() {
        for (XProcExtensionFunctionDefinition xf : exFuncs) {
            xf.close();
        }
        HttpClientUtils.closeQuietly(httpClient);
        httpClient = null;
    }

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

    public String getProfileFile() {
        return profileFile;
    }
    public void setProfileFile(String fn) {
        profileFile = fn;
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

    public void setURIResolver(URIResolver resolver) {
        uriResolver.setUnderlyingURIResolver(resolver);
    }

    public void setEntityResolver(EntityResolver resolver) {
        uriResolver.setUnderlyingEntityResolver(resolver);
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
            collections = new Hashtable<String,Vector<XdmNode>> ();
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
            MessageDigest digest = null;
            GregorianCalendar calendar = new GregorianCalendar();
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsae) {
                throw XProcException.dynamicError(36);
            }

            byte[] hash = digest.digest(calendar.toString().getBytes());
            episode = "CB";
            for (byte b : hash) {
                episode = episode + Integer.toHexString(b & 0xff);
            }
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
        return XProcConstants.XPROC_VERSION;
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

    public XLibrary getStandardLibrary() {
        if (xStandardLibrary == null) {
            xStandardLibrary = new XLibrary(this, standardLibrary);

            if (errorCode != null) {
                throw new XProcException(errorCode, errorMessage);
            }
        }

        return xStandardLibrary;
    }

    private synchronized void reset() {
        errorCode = null;
        errorMessage = null;
        declaredSteps = new Hashtable<QName,DeclareStep> ();
        //explicitDeclarations = false;
        pipeline = null;
        xpipeline = null;
        episode = null;
        collections = null;
        cookieStores = new HashMap<String, CookieStore>();

        xprocData = new XProcData(this);

        parser = new Parser(this);
        try {
            // FIXME: I should *do* something with these libraries, shouldn't I?
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

        if (profileFile != null) {
            profileHash = new Hashtable<XStep, Calendar>();
            profileWriter = new TreeWriter(this);
            try {
                profileWriter.startDocument(new URI("http://xmlcalabash.com/output/profile.xml"));
            } catch (URISyntaxException use) {
                // nop;
            }
        }
    }

    // FIXME: This design sucks
    public XPipeline load(String pipelineURI) throws SaxonApiException {
        for (String map : config.loaders.keySet()) {
            boolean data = map.startsWith("data:");
            String pattern = map.substring(5);
            if (pipelineURI.matches(pattern)) {
                return runPipelineLoader(pipelineURI, config.loaders.get(map), data);
            }
        }

        try {
            return _load(pipelineURI);
        } catch (SaxonApiException sae) {
            error(sae);
            throw sae;
        } catch (XProcException xe) {
            error(xe);
            throw xe;
        }
    }

    private XPipeline _load(String pipelineURI) throws SaxonApiException {
        reset();
        configurer.getXMLCalabashConfigurer().configRuntime(this);
        pipeline = parser.loadPipeline(pipelineURI);
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
    public XLibrary loadLibrary(String libraryURI) throws SaxonApiException {
        for (String map : config.loaders.keySet()) {
            boolean data = map.startsWith("data:");
            String pattern = map.substring(5);
            if (libraryURI.matches(pattern)) {
                return runLibraryLoader(libraryURI, config.loaders.get(map), data);
            }
        }

        try {
            return _loadLibrary(libraryURI);
        } catch (SaxonApiException sae) {
            error(sae);
            throw sae;
        } catch (XProcException xe) {
            error(xe);
            throw xe;
        }
    }

    private XLibrary _loadLibrary(String libraryURI) throws SaxonApiException {

        PipelineLibrary plibrary = parser.loadLibrary(libraryURI);
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

    private XPipeline runPipelineLoader(String pipelineURI, String loaderURI, boolean data) throws SaxonApiException {
        XdmNode pipeDoc = runLoader(pipelineURI, loaderURI, data);
        return use(pipeDoc);
    }

    private XLibrary runLibraryLoader(String pipelineURI, String loaderURI, boolean data) throws SaxonApiException {
        XdmNode libDoc = runLoader(pipelineURI, loaderURI, data);
        return useLibrary(libDoc);
    }
    
    private XdmNode runLoader(String pipelineURI, String loaderURI, boolean data) throws SaxonApiException {
        XPipeline loader = null;

        try {
            loader = _load(loaderURI);
        } catch (SaxonApiException sae) {
            error(sae);
            throw sae;
        } catch (XProcException xe) {
            error(xe);
            throw xe;
        }

        XdmNode pipeDoc = null;
        if (data) {
            ReadableData rdata = new ReadableData(this, XProcConstants.c_result, getStaticBaseURI().resolve(pipelineURI).toASCIIString(), "text/plain");
            pipeDoc = rdata.read();
        } else {
            pipeDoc = parse(pipelineURI, getStaticBaseURI().toASCIIString());
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
        if (declaredSteps.containsKey(name)) {
            throw new XProcException(step, "Duplicate declaration for " + name);
        } else {
            declaredSteps.put(name, step);
        }
    }

    public DeclareStep getBuiltinDeclaration(QName name) {
        if (declaredSteps.containsKey(name)) {
            return declaredSteps.get(name);
        } else {
            throw XProcException.staticError(44, null, "Unexpected step name: " + name);
        }
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
            SystemDefaultHttpClient httpClient = new SystemDefaultHttpClient();
            // Provide custom retry handler is necessary
            httpClient.setHttpRequestRetryHandler(new StandardHttpRequestRetryHandler(3, false));
            return this.httpClient = httpClient;
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

    public void fine(XProcRunnable step, XdmNode node, String message) {
        msgListener.fine(step, node, message);
    }

    public void finer(XProcRunnable step, XdmNode node, String message) {
        msgListener.finer(step, node, message);
    }

    public void finest(XProcRunnable step, XdmNode node, String message) {
        msgListener.finest(step, node, message);
    }

    // ===========================================================

    public void start(XStep step) {
        if (profileFile == null) {
            return;
        }

        boolean first = profileHash.isEmpty();

        Calendar start = GregorianCalendar.getInstance();
        profileHash.put(step, start);
        profileWriter.addStartElement(profileProfile);

        if (first) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            profileWriter.addAttribute(new QName("", "timestamp"), df.format(new Date()));
            profileWriter.addAttribute(new QName("", "episode"), getEpisode());
            profileWriter.addAttribute(new QName("", "language"), getLanguage());
            profileWriter.addAttribute(new QName("", "product-name"), getProductName());
            profileWriter.addAttribute(new QName("", "product-version"), getProductVersion());
            profileWriter.addAttribute(new QName("", "product-vendor"), getVendor());
            profileWriter.addAttribute(new QName("", "product-vendor-uri"), getVendorURI());
            profileWriter.addAttribute(new QName("", "xproc-version"), getXProcVersion());
            profileWriter.addAttribute(new QName("", "xpath-version"), getXPathVersion());
            profileWriter.addAttribute(new QName("", "psvi-supported"), ""+getPSVISupported());
        }

        String name = step.getType().getClarkName();
        profileWriter.addAttribute(profileType, name);
        profileWriter.addAttribute(profileName, step.getStep().getName());
        profileWriter.startContent();
    }

    public void finish(XStep step) {
        if (profileFile == null) {
            return;
        }

        Calendar start = profileHash.get(step);
        long time = GregorianCalendar.getInstance().getTimeInMillis() - start.getTimeInMillis();
        profileHash.remove(step);

        profileWriter.addStartElement(profileTime);
        profileWriter.startContent();
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

                Serializer serializer = new Serializer();
                serializer.setOutputProperty(Serializer.Property.INDENT, "yes");

                OutputStream outstr = null;
                if ("-".equals(profileFile)) {
                    outstr = System.out;
                } else {
                    outstr = new FileOutputStream(new File(profileFile));
                }

                serializer.setOutputStream(outstr);
                S9apiUtils.serialize(this, result.getXdmNode(), serializer);
                outstr.close();

                profileWriter = new TreeWriter(this);
                try {
                    profileWriter.startDocument(new URI("http://xmlcalabash.com/output/profile.xml"));
                } catch (URISyntaxException use) {
                    // nop;
                }
            } catch (SaxonApiException sae) {
                throw new XProcException(sae);
            } catch (FileNotFoundException fnfe) {
                throw new XProcException(fnfe);
            } catch (IOException ioe) {
                throw new XProcException(ioe);
            }
        }
    }
}
