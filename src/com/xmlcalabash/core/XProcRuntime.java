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
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.runtime.XLibrary;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.runtime.XRootStep;
import com.xmlcalabash.runtime.XStep;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.SaxonApiException;
import com.xmlcalabash.model.Parser;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.PipelineLibrary;
import com.xmlcalabash.util.XProcURIResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.Vector;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.commons.httpclient.Cookie;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 *
 * @author ndw
 */
public class XProcRuntime {
    protected Logger logger = Logger.getLogger("com.xmlcalabash");
    private XProcProcessor xproc = null;
    private Parser parser = null;
    private QName errorCode = null;
    private XdmNode errorNode = null;
    private String errorMessage = null;
    private Hashtable<QName, DeclareStep> declaredSteps = new Hashtable<QName,DeclareStep> ();
    private XPipeline xpipeline = null;
    private Vector<Throwable> errors = null;
    private static String episode = null;
    private Hashtable<String,Vector<XdmNode>> collections = null;
    private XProcData xprocData = null;
    private PipelineLibrary standardLibrary = null;
    private XLibrary xStandardLibrary = null;
    private Hashtable<String,Vector<Cookie>> cookieHash = new Hashtable<String,Vector<Cookie>> ();
    private Vector<XProcExtensionFunctionDefinition> exFuncs = new Vector<XProcExtensionFunctionDefinition>();

    private XProcMessageListener msgListener = null;
    private XProcURIResolver uriResolver = null;

    private boolean debug = false;
    private File profileFile = null;
    private Hashtable<XStep,Calendar> profileHash = null;
    private TreeWriter profileWriter = null;
    private QName profileProfile = new QName("http://xmlcalabash.com/ns/profile", "profile");
    private QName profileType = new QName("", "type");
    private QName profileName = new QName("", "name");
    private QName profileTime = new QName("http://xmlcalabash.com/ns/profile", "time");

    public XProcRuntime (XProcProcessor xproc) {
        this.xproc = xproc;
        Processor processor = xproc.getProcessor();

        uriResolver = xproc.getURIResolver();
        msgListener = xproc.getMessageListener();

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

        reset();
    }

    public void setProfileOutput(File outputFile) {
        profileFile = outputFile;
        profileHash = new Hashtable<XStep, Calendar> ();
        profileWriter = new TreeWriter(this);
        try {
            profileWriter.startDocument(new URI("http://xmlcalabash.com/output/profile.xml"));
        } catch (URISyntaxException use) {
            // nop;
        }
    }

    public XProcRuntime(XProcRuntime runtime) {
        xproc = runtime.xproc;
        uriResolver = runtime.uriResolver;
        msgListener = runtime.msgListener;
        standardLibrary = runtime.standardLibrary;
        xStandardLibrary = runtime.xStandardLibrary;
        cookieHash = runtime.cookieHash;
        profileFile = runtime.profileFile;
        reset();
    }

    public void close() {
        for (XProcExtensionFunctionDefinition xf : exFuncs) {
            xf.close();
        }
    }

    public XProcData getXProcData() {
        return xprocData;
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

    public boolean getAllowGeneralExpressions() {
        return xproc.getAllowGeneralExpressions();
    }

    public boolean getAllowXPointerOnText() {
        return xproc.getAllowXPointerOnText();
    }

    public boolean transparentJSON() {
        return xproc.transparentJSON();
    }

    public String jsonFlavor() {
        return xproc.jsonFlavor();
    }

    public boolean getUseXslt10Processor() {
        return xproc.getUseXslt10Processor();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean getDebug() {
        return debug;
    }

    public void cache(XdmNode doc, URI baseURI) {
        uriResolver.cache(doc, baseURI);
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
        return xproc.getPSVISupported();
    }

    public URI getStaticBaseURI() {
        return xproc.getStaticBaseURI();
    }

    public XPathCompiler newXPathCompiler() {
        return xproc.getProcessor().newXPathCompiler();
    }

    public XsltCompiler newXsltCompiler() {
        return getXProcProcessor().getProcessor().newXsltCompiler();
    }

    public DocumentBuilder newDocumentBuilder() {
        return xproc.getProcessor().newDocumentBuilder();
    }

    public String htmlParser() {
        return xproc.htmlParser();
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

    private void reset() {
        errorCode = null;
        errorMessage = null;
        declaredSteps = new Hashtable<QName,DeclareStep> ();
        xpipeline = null;
        errors = null;
        episode = null;
        collections = null;
        cookieHash = new Hashtable<String,Vector<Cookie>> ();
        debug = xproc.getConfiguration().debug;

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
        for (String map : xproc.getConfiguration().loaders.keySet()) {
            boolean data = map.startsWith("data:");
            String pattern = map.substring(5);
            if (pipelineURI.matches(pattern)) {
                return runPipelineLoader(pipelineURI, xproc.getConfiguration().loaders.get(map), data);
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
        xproc.getConfigurer().getXMLCalabashConfigurer().configRuntime(this);
        DeclareStep pipeline = parser.loadPipeline(pipelineURI);
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
        xproc.getConfigurer().getXMLCalabashConfigurer().configRuntime(this);
        DeclareStep pipeline = parser.usePipeline(p_pipeline);
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
        for (String map : xproc.getConfiguration().loaders.keySet()) {
            boolean data = map.startsWith("data:");
            String pattern = map.substring(5);
            if (libraryURI.matches(pattern)) {
                return runLibraryLoader(libraryURI, xproc.getConfiguration().loaders.get(map), data);
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
            ReadableData rdata = new ReadableData(this, XProcConstants.c_result, xproc.getStaticBaseURI().resolve(pipelineURI).toASCIIString(), "text/plain");
            pipeDoc = rdata.read();
        } else {
            pipeDoc = parse(pipelineURI, xproc.getStaticBaseURI().toASCIIString());
        }

        loader.clearInputs("source");
        loader.writeTo("source", pipeDoc);
        loader.run();
        ReadablePipe xformed = loader.readFrom("result");
        pipeDoc = xformed.read();

        reset();
        return pipeDoc;
    }
    
    public XProcProcessor getXProcProcessor() {
        return xproc;
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

    public void clearCookies(String key) {
        if (cookieHash.containsKey(key)) {
            cookieHash.get(key).clear();
        }
    }

    public void addCookie(String key, Cookie cookie) {
        if (!cookieHash.containsKey(key)) {
            cookieHash.put(key, new Vector<Cookie> ());
        }

        cookieHash.get(key).add(cookie);
    }

    public Vector<Cookie> getCookies(String key) {
        if (cookieHash.containsKey(key)) {
            return cookieHash.get(key);
        } else {
            return new Vector<Cookie> ();
        }
    }

    public QName getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public XProcStep newStep(XAtomicStep step){
        String className = xproc.implementation(step.getType());
        if (className == null) {
            throw new UnsupportedOperationException("Misconfigured. No 'class' in configuration for " + step.getType());
        }

        // FIXME: This isn't really very secure...
        if (xproc.getSafeMode() && !className.startsWith("com.xmlcalabash.")) {
            throw XProcException.dynamicError(21);
        }

        try {
            Constructor constructor = Class.forName(className).getConstructor(XProcRuntime.class, XAtomicStep.class);
            return (XProcStep) constructor.newInstance(this,step);
        } catch (NoSuchMethodException nsme) {
            throw new UnsupportedOperationException("No such method: " + className, nsme);
        } catch (ClassNotFoundException cfne) {
            throw new UnsupportedOperationException("Class not found: " + className, cfne);
        } catch (InstantiationException ie) {
            throw new UnsupportedOperationException("Instantiation error", ie);
        } catch (IllegalAccessException iae) {
            throw new UnsupportedOperationException("Illegal access error", iae);
        } catch (InvocationTargetException ite) {
            throw new UnsupportedOperationException("Invocation target exception", ite);
        }
    }

    public boolean getSafeMode() {
        return xproc.getSafeMode();
    }

    public XPipeline getPipeline() {
        return xpipeline;
    }

    public void serialize(XdmNode node, Serializer serializer) throws SaxonApiException {
        S9apiUtils.serialize(xproc, node, serializer);
    }

    public void serialize(Vector<XdmNode> nodes, Serializer serializer) throws SaxonApiException {
        S9apiUtils.serialize(xproc, nodes, serializer);
    }

    public InputSource xdmToInputSource(XdmNode node) throws SaxonApiException {
        return S9apiUtils.xdmToInputSource(xproc, node);
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
            profileWriter.addAttribute(new QName("", "psvi-supported"), ""+xproc.getPSVISupported());
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
                XsltCompiler compiler = getXProcProcessor().getProcessor().newXsltCompiler();
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
                    outstr = new FileOutputStream(profileFile);
                }

                serializer.setOutputStream(outstr);
                serialize(result.getXdmNode(), serializer);
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
