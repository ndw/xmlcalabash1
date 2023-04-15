package com.xmlcalabash.core;

import com.nwalsh.annotations.SaxonExtensionFunction;
import com.xmlcalabash.io.DocumentSequence;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.piperack.PipelineSource;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.AxisNodes;
import com.xmlcalabash.util.Input;
import com.xmlcalabash.util.JSONtoXML;
import com.xmlcalabash.util.LogOptions;
import com.xmlcalabash.util.Output;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.URIUtils;
import net.sf.saxon.Version;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.om.NoElementsSpaceStrippingRule;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmValue;
import org.atteo.classindex.ClassFilter;
import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import static com.xmlcalabash.util.URIUtils.encode;
import static java.lang.String.format;
import static java.lang.System.getProperty;

public class XProcConfiguration {
    public static final QName _prefix = new QName("", "prefix");
    public static final QName _uri = new QName("", "uri");
    public static final QName _class_name = new QName("", "class-name");
    public static final QName _type = new QName("", "type");
    public static final QName _port = new QName("", "port");
    public static final QName _href = new QName("", "href");
    public static final QName _data = new QName("", "data");
    public static final QName _name = new QName("", "name");
    public static final QName _key = new QName("", "key");
    public static final QName _expires = new QName("", "expires");
    public static final QName _value = new QName("", "value");
    public static final QName _loader = new QName("", "loader");
    public static final QName _exclude_inline_prefixes = new QName("", "exclude-inline-prefixes");

    protected Logger logger = null;

    public String saxonProcessor = "he";
    public boolean schemaAware = false;
    public Input saxonConfig = null;
    public HashMap<String,String> nsBindings = new HashMap<String,String> ();
    public boolean debug = false;
    public boolean showMessages = false;
    public Output profile = null;
    public HashMap<String,Vector<ReadablePipe>> inputs = new HashMap<String,Vector<ReadablePipe>> ();
    public ReadablePipe pipeline = null;
    public HashMap<String,String> outputs = new HashMap<String,String> ();
    public HashMap<String,HashMap<QName,String>> params = new HashMap<String,HashMap<QName,String>> ();
    public HashMap<QName,String> options = new HashMap<QName,String> ();
    public boolean safeMode = false;
    public String stepName = null;
    public String entityResolver = "org.xmlresolver.Resolver";
    public String uriResolver = "org.xmlresolver.Resolver";
    public String errorListener = null;
    public HashMap<QName,Class> implementations = new HashMap<QName,Class> ();
    public HashMap<String,String> serializationOptions = new HashMap<String,String>();
    public LogOptions logOpt = LogOptions.WRAPPED;
    public HashMap<String,SaxonExtensionFunction> extensionFunctions = new HashMap<String,SaxonExtensionFunction>();
    public String foProcessor = null;
    public String cssProcessor = null;
    public String xprocConfigurer = null;
    public String htmlParser = "validator.nu";
    public String mailHost = null;
    public String mailPort = "25";
    public String mailUser = null;
    public String mailPass = null;
    public HashMap<String,String> loaders = new HashMap<String,String> ();
    public HashSet<String> setSaxonProperties = new HashSet<String>();
    public HashMap<String,String> proxies = new HashMap<String,String> ();

    public boolean extensionValues = false;
    public boolean xpointerOnText = false;
    public boolean transparentJSON = false;
    public boolean ignoreInvalidXmlBase = false;
    public String jsonFlavor = JSONtoXML.MARKLOGIC;
    public boolean useXslt10 = false;
    public boolean htmlSerializer = false;
    public boolean allowTextResults = false;
    public Vector<String> catalogs = new Vector<> ();

    public int piperackPort = 8088;
    public int piperackDefaultExpires = 300;
    public HashMap<String,PipelineSource> piperackDefaultPipelines = new HashMap<String,PipelineSource>();

    private Processor cfgProcessor = null;
    private boolean firstInput = false;
    private boolean firstOutput = false;

    private static HashMap<String,Feature<Boolean>> bFeatureMap = new HashMap<String,Feature<Boolean>> ();
    private static HashMap<String,Feature<Integer>> iFeatureMap = new HashMap<String,Feature<Integer>> ();
    private static HashMap<String,Feature<String>> sFeatureMap = new HashMap<String,Feature<String>> ();
    static {
        bFeatureMap.put("http://saxon.sf.net/feature/allow-external-functions", Feature.ALLOW_EXTERNAL_FUNCTIONS);
        bFeatureMap.put("http://saxon.sf.net/feature/allow-multithreading", Feature.ALLOW_MULTITHREADING);
        bFeatureMap.put("http://saxon.sf.net/feature/allow-old-java-uri-format", Feature.ALLOW_OLD_JAVA_URI_FORMAT);
        bFeatureMap.put("http://saxon.sf.net/feature/allowSyntaxExtensions", Feature.ALLOW_SYNTAX_EXTENSIONS);
        bFeatureMap.put("http://saxon.sf.net/feature/assertionsCanSeeComments", Feature.ASSERTIONS_CAN_SEE_COMMENTS);
        sFeatureMap.put("http://saxon.sf.net/feature/collation-uri-resolver-class", Feature.COLLATION_URI_RESOLVER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/collection-finder-class", Feature.COLLECTION_FINDER_CLASS);
        //x10 sFeatureMap.put("http://saxon.sf.net/feature/collection-uri-resolver-class", Feature.COLLECTION_URI_RESOLVER_CLASS);
        bFeatureMap.put("http://saxon.sf.net/feature/compile-with-tracing", Feature.COMPILE_WITH_TRACING);
        sFeatureMap.put("http://saxon.sf.net/feature/configuration-file", Feature.CONFIGURATION_FILE);
        bFeatureMap.put("http://saxon.sf.net/feature/debugByteCode", Feature.DEBUG_BYTE_CODE);
        sFeatureMap.put("http://saxon.sf.net/feature/debugByteCodeDir", Feature.DEBUG_BYTE_CODE_DIR);
        sFeatureMap.put("http://saxon.sf.net/feature/defaultCollation", Feature.DEFAULT_COLLATION);
        sFeatureMap.put("http://saxon.sf.net/feature/defaultCollection", Feature.DEFAULT_COLLECTION);
        sFeatureMap.put("http://saxon.sf.net/feature/defaultCountry", Feature.DEFAULT_COUNTRY);
        sFeatureMap.put("http://saxon.sf.net/feature/defaultLanguage", Feature.DEFAULT_LANGUAGE);
        sFeatureMap.put("http://saxon.sf.net/feature/defaultRegexEngine", Feature.DEFAULT_REGEX_ENGINE);
        bFeatureMap.put("http://saxon.sf.net/feature/disableXslEvaluate", Feature.DISABLE_XSL_EVALUATE);
        bFeatureMap.put("http://saxon.sf.net/feature/displayByteCode", Feature.DISPLAY_BYTE_CODE);
        bFeatureMap.put("http://saxon.sf.net/feature/validation", Feature.DTD_VALIDATION);
        bFeatureMap.put("http://saxon.sf.net/feature/dtd-validation-recoverable", Feature.DTD_VALIDATION_RECOVERABLE);
        bFeatureMap.put("http://saxon.sf.net/feature/eagerEvaluation", Feature.EAGER_EVALUATION);
        sFeatureMap.put("http://saxon.sf.net/feature/entityResolverClass", Feature.ENTITY_RESOLVER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/environmentVariableResolverClass", Feature.ENVIRONMENT_VARIABLE_RESOLVER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/errorListenerClass", Feature.ERROR_LISTENER_CLASS);
        bFeatureMap.put("http://saxon.sf.net/feature/expandAttributeDefaults", Feature.EXPAND_ATTRIBUTE_DEFAULTS);
        bFeatureMap.put("http://saxon.sf.net/feature/expathFileDeleteTemporaryFiles", Feature.EXPATH_FILE_DELETE_TEMPORARY_FILES);
        bFeatureMap.put("http://saxon.sf.net/feature/generateByteCode", Feature.GENERATE_BYTE_CODE);
        bFeatureMap.put("http://saxon.sf.net/feature/ignoreSAXSourceParser", Feature.IGNORE_SAX_SOURCE_PARSER);
        bFeatureMap.put("http://saxon.sf.net/feature/implicitSchemaImports", Feature.IMPLICIT_SCHEMA_IMPORTS);
        bFeatureMap.put("http://saxon.sf.net/feature/lazyConstructionMode", Feature.LAZY_CONSTRUCTION_MODE);
        sFeatureMap.put("http://saxon.sf.net/feature/licenseFileLocation", Feature.LICENSE_FILE_LOCATION);
        bFeatureMap.put("http://saxon.sf.net/feature/linenumbering", Feature.LINE_NUMBERING);
        bFeatureMap.put("http://saxon.sf.net/feature/markDefaultedAttributes", Feature.MARK_DEFAULTED_ATTRIBUTES);
        iFeatureMap.put("http://saxon.sf.net/feature/maxCompiledClasses", Feature.MAX_COMPILED_CLASSES);
        //sFeatureMap.put("http://saxon.sf.net/feature/messageEmitterClass", Feature.MESSAGE_EMITTER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/moduleURIResolverClass", Feature.MODULE_URI_RESOLVER_CLASS);
        bFeatureMap.put("http://saxon.sf.net/feature/monitorHotSpotByteCode", Feature.MONITOR_HOT_SPOT_BYTE_CODE);
        bFeatureMap.put("http://saxon.sf.net/feature/multipleSchemaImports", Feature.MULTIPLE_SCHEMA_IMPORTS);
        sFeatureMap.put("http://saxon.sf.net/feature/outputURIResolverClass", Feature.OUTPUT_URI_RESOLVER_CLASS);
        bFeatureMap.put("http://saxon.sf.net/feature/preEvaluateDocFunction", Feature.PRE_EVALUATE_DOC_FUNCTION);
        bFeatureMap.put("http://saxon.sf.net/feature/recognize-uri-query-parameters", Feature.RECOGNIZE_URI_QUERY_PARAMETERS);
        iFeatureMap.put("http://saxon.sf.net/feature/recoveryPolicy", Feature.RECOVERY_POLICY);
        sFeatureMap.put("http://saxon.sf.net/feature/recoveryPolicyName", Feature.RECOVERY_POLICY_NAME);
        iFeatureMap.put("http://saxon.sf.net/feature/resultDocumentThreads", Feature.RESULT_DOCUMENT_THREADS);
        bFeatureMap.put("http://saxon.sf.net/feature/retain-dtd-attribute-types", Feature.RETAIN_DTD_ATTRIBUTE_TYPES);
        sFeatureMap.put("http://saxon.sf.net/feature/schemaURIResolverClass", Feature.SCHEMA_URI_RESOLVER_CLASS);
        iFeatureMap.put("http://saxon.sf.net/feature/schema-validation", Feature.SCHEMA_VALIDATION);
        sFeatureMap.put("http://saxon.sf.net/feature/schema-validation-mode", Feature.SCHEMA_VALIDATION_MODE);
        sFeatureMap.put("http://saxon.sf.net/feature/serializerFactoryClass", Feature.SERIALIZER_FACTORY_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/sourceParserClass", Feature.SOURCE_PARSER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/sourceResolverClass", Feature.SOURCE_RESOLVER_CLASS);
        bFeatureMap.put("http://saxon.sf.net/feature/stableCollectionUri", Feature.STABLE_COLLECTION_URI);
        bFeatureMap.put("http://saxon.sf.net/feature/stableUnparsedText", Feature.STABLE_UNPARSED_TEXT);
        sFeatureMap.put("http://saxon.sf.net/feature/standardErrorOutputFile", Feature.STANDARD_ERROR_OUTPUT_FILE);
        sFeatureMap.put("http://saxon.sf.net/feature/streamability", Feature.STREAMABILITY);
        bFeatureMap.put("http://saxon.sf.net/feature/strictStreamability", Feature.STRICT_STREAMABILITY);
        bFeatureMap.put("http://saxon.sf.net/feature/streamingFallback", Feature.STREAMING_FALLBACK);
        sFeatureMap.put("http://saxon.sf.net/feature/strip-whitespace", Feature.STRIP_WHITESPACE);
        sFeatureMap.put("http://saxon.sf.net/feature/styleParserClass", Feature.STYLE_PARSER_CLASS);
        bFeatureMap.put("http://saxon.sf.net/feature/suppressEvaluationExpiryWarning", Feature.SUPPRESS_EVALUATION_EXPIRY_WARNING);
        bFeatureMap.put("http://saxon.sf.net/feature/suppressXPathWarnings", Feature.SUPPRESS_XPATH_WARNINGS);
        bFeatureMap.put("http://saxon.sf.net/feature/suppressXsltNamespaceCheck", Feature.SUPPRESS_XSLT_NAMESPACE_CHECK);
        iFeatureMap.put("http://saxon.sf.net/feature/thresholdForCompilingTypes", Feature.THRESHOLD_FOR_COMPILING_TYPES);
        bFeatureMap.put("http://saxon.sf.net/feature/timing", Feature.TIMING);
        bFeatureMap.put("http://saxon.sf.net/feature/trace-external-functions", Feature.TRACE_EXTERNAL_FUNCTIONS);
        sFeatureMap.put("http://saxon.sf.net/feature/traceListenerClass", Feature.TRACE_LISTENER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/traceListenerOutputFile", Feature.TRACE_LISTENER_OUTPUT_FILE);
        bFeatureMap.put("http://saxon.sf.net/feature/trace-optimizer-decisions", Feature.TRACE_OPTIMIZER_DECISIONS);
        iFeatureMap.put("http://saxon.sf.net/feature/treeModel", Feature.TREE_MODEL);
        sFeatureMap.put("http://saxon.sf.net/feature/treeModelName", Feature.TREE_MODEL_NAME);
        sFeatureMap.put("http://saxon.sf.net/feature/unparsedTextURIResolverClass", Feature.UNPARSED_TEXT_URI_RESOLVER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/uriResolverClass", Feature.URI_RESOLVER_CLASS);
        bFeatureMap.put("http://saxon.sf.net/feature/use-pi-disable-output-escaping", Feature.USE_PI_DISABLE_OUTPUT_ESCAPING);
        bFeatureMap.put("http://saxon.sf.net/feature/use-typed-value-cache", Feature.USE_TYPED_VALUE_CACHE);
        bFeatureMap.put("http://saxon.sf.net/feature/useXsiSchemaLocation", Feature.USE_XSI_SCHEMA_LOCATION);
        bFeatureMap.put("http://saxon.sf.net/feature/validation-comments", Feature.VALIDATION_COMMENTS);
        bFeatureMap.put("http://saxon.sf.net/feature/validation-warnings", Feature.VALIDATION_WARNINGS);
        bFeatureMap.put("http://saxon.sf.net/feature/version-warning", Feature.VERSION_WARNING);
        bFeatureMap.put("http://saxon.sf.net/feature/xinclude-aware", Feature.XINCLUDE);
        sFeatureMap.put("http://saxon.sf.net/feature/xml-version", Feature.XML_VERSION);
        bFeatureMap.put("http://saxon.sf.net/feature/parserFeature?uri=", Feature.XML_PARSER_FEATURE);
        bFeatureMap.put("http://saxon.sf.net/feature/parserProperty?uri=", Feature.XML_PARSER_PROPERTY);
        bFeatureMap.put("http://saxon.sf.net/feature/xqueryAllowUpdate", Feature.XQUERY_ALLOW_UPDATE);
        sFeatureMap.put("http://saxon.sf.net/feature/xqueryConstructionMode", Feature.XQUERY_CONSTRUCTION_MODE);
        bFeatureMap.put("http://saxon.sf.net/feature/xqueryEmptyLeast", Feature.XQUERY_EMPTY_LEAST);
        bFeatureMap.put("http://saxon.sf.net/feature/xqueryInheritNamespaces", Feature.XQUERY_INHERIT_NAMESPACES);
        bFeatureMap.put("http://saxon.sf.net/feature/xqueryMultipleModuleImports", Feature.XQUERY_MULTIPLE_MODULE_IMPORTS);
        bFeatureMap.put("http://saxon.sf.net/feature/xqueryPreserveBoundarySpace", Feature.XQUERY_PRESERVE_BOUNDARY_SPACE);
        bFeatureMap.put("http://saxon.sf.net/feature/xqueryPreserveNamespaces", Feature.XQUERY_PRESERVE_NAMESPACES);
        sFeatureMap.put("http://saxon.sf.net/feature/xqueryRequiredContextItemType", Feature.XQUERY_REQUIRED_CONTEXT_ITEM_TYPE);
        bFeatureMap.put("http://saxon.sf.net/feature/xquerySchemaAware", Feature.XQUERY_SCHEMA_AWARE);
        sFeatureMap.put("http://saxon.sf.net/feature/xqueryStaticErrorListenerClass", Feature.XQUERY_STATIC_ERROR_LISTENER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/xqueryVersion", Feature.XQUERY_VERSION);
        sFeatureMap.put("http://saxon.sf.net/feature/xsd-version", Feature.XSD_VERSION);
        bFeatureMap.put("http://saxon.sf.net/feature/enableAssertions", Feature.XSLT_ENABLE_ASSERTIONS);
        sFeatureMap.put("http://saxon.sf.net/feature/initialMode", Feature.XSLT_INITIAL_MODE);
        sFeatureMap.put("http://saxon.sf.net/feature/initialTemplate", Feature.XSLT_INITIAL_TEMPLATE);
        bFeatureMap.put("http://saxon.sf.net/feature/xsltSchemaAware", Feature.XSLT_SCHEMA_AWARE);
        sFeatureMap.put("http://saxon.sf.net/feature/stylesheetErrorListener", Feature.XSLT_STATIC_ERROR_LISTENER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/stylesheetURIResolver", Feature.XSLT_STATIC_URI_RESOLVER_CLASS);
        sFeatureMap.put("http://saxon.sf.net/feature/xsltVersion", Feature.XSLT_VERSION);
        iFeatureMap.put("http://saxon.sf.net/feature/regexBacktrackingLimit", Feature.REGEX_BACKTRACKING_LIMIT);
        iFeatureMap.put("http://saxon.sf.net/feature/xpathVersionForXsd", Feature.XPATH_VERSION_FOR_XSD);
        iFeatureMap.put("http://saxon.sf.net/feature/xpathVersionForXslt", Feature.XPATH_VERSION_FOR_XSLT);
    }

    public XProcConfiguration() {
        logger = LoggerFactory.getLogger(this.getClass());
        initSaxonProcessor("he", false, null);
        init();
    }

    // This constructor is historical, the (String, boolean) constructor is preferred
    public XProcConfiguration(boolean schemaAware) {
        logger = LoggerFactory.getLogger(this.getClass());
        initSaxonProcessor("he", schemaAware, null);
        init();
    }

    public XProcConfiguration(Input saxoncfg) {
        logger = LoggerFactory.getLogger(this.getClass());
        initSaxonProcessor(null, false, saxoncfg);
        init();
    }

    public XProcConfiguration(String proctype, boolean schemaAware) {
        logger = LoggerFactory.getLogger(this.getClass());
        initSaxonProcessor(proctype, schemaAware, null);
        init();
    }

    public XProcConfiguration(Processor processor) {
        logger = LoggerFactory.getLogger(this.getClass());
        cfgProcessor = processor;
        loadConfiguration();
        if (schemaAware != processor.isSchemaAware()) {
            throw new XProcException("Schema awareness in configuration conflicts with specified processor.");
        }
        init();
    }

    public Processor getProcessor() {
        return cfgProcessor;
    }

    private void initSaxonProcessor(String proctype, boolean schemaAware, Input saxoncfg) {
        if (schemaAware) {
            proctype = "ee";
        }

        createSaxonProcessor(proctype, schemaAware, saxoncfg);
        loadConfiguration();

        // If we got a schema aware processor, make sure it's reflected in our config
        // FIXME: are there other things that should be reflected this way?
        this.schemaAware = cfgProcessor.isSchemaAware();
        saxonProcessor = Version.softwareEdition.toLowerCase();

        if (saxoncfg != null) {
            // If there was a Saxon configuration, then it wins
            schemaAware = this.schemaAware;
            proctype = saxonProcessor;
        }

        if (!(proctype == null || saxonProcessor.equals(proctype))
                || schemaAware != this.schemaAware
                || (saxoncfg == null && saxonConfig != null)) {
            // Drat. We have to restart to get the right configuration.
            nsBindings.clear();
            inputs.clear();
            outputs.clear();
            params.clear();
            options.clear();
            implementations.clear();
            extensionFunctions.clear();

            createSaxonProcessor(saxonProcessor, this.schemaAware, saxonConfig);
            loadConfiguration();

            // If we got a schema aware processor, make sure it's reflected in our config
            // FIXME: are there other things that should be reflected this way?
            this.schemaAware = cfgProcessor.isSchemaAware();
            saxonProcessor = Version.softwareEdition.toLowerCase();
        }
    }

    private void init() {
        // If we got a schema aware processor, make sure it's reflected in our config
        // FIXME: are there other things that should be reflected this way?
        this.schemaAware = cfgProcessor.isSchemaAware();
        saxonProcessor = Version.softwareEdition.toLowerCase();
        findStepClasses();
        findExtensionFunctions();
        findImplConfiguration();

        String classPath = System.getProperty("java.class.path");
        String[] pathElements = classPath.split(System.getProperty("path.separator"));
        for (String path : pathElements) {
            // Make the path absolute wrt the cwd so that it can be opened later regardless of context
            path = new File(path).getAbsolutePath();
            try {
                String jarFileURL = URLDecoder.decode(new File(path).toURI().toString().replace("+", "%2B"), "UTF-8");
                JarFile jar = new JarFile(path);
                ZipEntry catalog = jar.getEntry("catalog.xml");
                if (catalog != null) {
                    catalogs.add("jar:" + jarFileURL + "!/catalog.xml");
                    logger.debug("Using catalog: jar:" + jarFileURL + "!/catalog.xml");
                }
                catalog = jar.getEntry("META-INF/catalog.xml");
                if (catalog != null) {
                    catalogs.add("jar:" + jarFileURL + "!/META-INF/catalog.xml");
                    logger.debug("Using catalog: jar:" + jarFileURL + "!/META-INF/catalog.xml");
                }
            } catch (IOException e) {
                // If it's not a jar file, maybe it's a directory with a catalog
                String catfn = path;
                if (!catfn.endsWith("/")) {
                    catfn += "/";
                }
                catfn += "catalog.xml";
                File f = new File(catfn);
                if (f.exists() && f.isFile()) {
                    catalogs.add(f.toURI().toString());
                    logger.debug("Using catalog: " + catfn);
                }
            }
        }
    }

    private void createSaxonProcessor(String proctype, boolean schemaAware, Input saxoncfg) {
        boolean licensed = schemaAware || !"he".equals(proctype);

        if (saxoncfg != null) {
            try {
                InputStream instream = null;
                switch (saxoncfg.getKind()) {
                    case URI:
                        URI furi = URI.create(saxoncfg.getUri());
                        instream = new FileInputStream(new File(furi));
                        break;

                    case INPUT_STREAM:
                        instream = saxoncfg.getInputStream();
                        break;

                    default:
                        throw new UnsupportedOperationException(format("Unsupported saxonConfig kind '%s'", saxoncfg.getKind()));
                }

                SAXSource source = new SAXSource(new InputSource(instream));
                cfgProcessor = new Processor(source);
            } catch (FileNotFoundException | SaxonApiException e) {
                throw new XProcException(e);
            }
        } else {
            cfgProcessor = new Processor(licensed);
        }

        cfgProcessor.getUnderlyingConfiguration().getParseOptions().withSpaceStrippingRule(NoElementsSpaceStrippingRule.getInstance());

        String actualtype = Version.softwareEdition;
        if ((proctype != null) && !"he".equals(proctype) && (!actualtype.toLowerCase().equals(proctype))) {
            System.err.println("Failed to obtain " + proctype.toUpperCase() + " processor; using " + actualtype + " instead.");
        }
    }

    private void findStepClasses() {
        logger.debug("Current implementations: " + implementations.size());
        logger.debug("Searching for implementations:");
        Iterable<Class<?>> classes = ClassFilter.only().from(ClassIndex.getAnnotated(XMLCalabash.class));
        for (Class<?> klass : classes) {
            XMLCalabash annotation = klass.getAnnotation(XMLCalabash.class);
            for (String clarkName: annotation.type().split("\\s+")) {
                try {
                    QName name = QName.fromClarkName(clarkName);
                    logger.debug("Found step type annotation: " + clarkName);
                    if (implementations.containsKey(name)) {
                        logger.debug("Ignoring step type annotation for configured step: " + clarkName);
                    } else {
                        implementations.put(name, klass);
                    }
                } catch (IllegalArgumentException iae) {
                    logger.debug("Failed to parse step annotation type: " + clarkName);
                }
            }
        }
        logger.debug("After search: " + implementations.size());
    }

    private void findExtensionFunctions() {
        logger.debug("Searching for Saxon extension functions on the class path");
        Iterable<Class<?>> classes = ClassIndex.getAnnotated(SaxonExtensionFunction.class);
        for (Class<?> klass : classes) {
            String name = klass.getCanonicalName();
            SaxonExtensionFunction annotation = klass.getAnnotation(SaxonExtensionFunction.class);
            logger.debug("Found Saxon extension function: " + klass.getCanonicalName());
            if (extensionFunctions.containsKey(name)) {
                logger.debug("Duplicate saxon extension function class: " + name);
            }
            extensionFunctions.put(name, annotation);
        }
    }

    private void findImplConfiguration() {
        try {
            Enumeration<URL> uriEnum = this.getClass().getClassLoader().getResources("com.xmlcalabash.properties");
            while (uriEnum.hasMoreElements()) {
                URL url = uriEnum.nextElement();
                logger.debug("Loading properties: " + url);

                URLConnection conn = url.openConnection();
                InputStream stream = conn.getInputStream();
                Properties props = new Properties();
                props.load(stream);

                HashMap<String,String> nsmap = new HashMap<String,String>();
                Pattern nsPattern = Pattern.compile("namespace\\s+(.+)$");
                Pattern sPattern = Pattern.compile("step\\s+(.+)$");
                Pattern qPattern = Pattern.compile("^([^:]+):([^:]+)$");

                // Properties are unordered so find the namespace bindings
                for (String name : props.stringPropertyNames()) {
                    String value = (String) props.get(name);
                    Matcher matcher = nsPattern.matcher(value);
                    if (matcher.matches()) {
                        if (nsmap.containsKey(name)) {
                            throw new XProcException("Cannot redefine namespace bindings in property file");
                        }
                        nsmap.put(name, matcher.group(1));
                    }
                }

                for (String name : props.stringPropertyNames()) {
                    String value = (String) props.get(name);

                    Matcher nsMatcher = nsPattern.matcher(value);
                    Matcher sMatcher = sPattern.matcher(value);

                    if (nsMatcher.matches()) {
                        // nop
                    } else if (sMatcher.matches()) {
                        String qnames = sMatcher.group(1);
                        for (String lexQName : qnames.split("\\s*,\\s*")) {
                            Matcher qMatcher = qPattern.matcher(lexQName);
                            if (qMatcher.matches()) {
                                String pfx = qMatcher.group(1);
                                String local = qMatcher.group(2);

                                if (nsmap.containsKey(pfx)) {
                                    try {
                                        QName qname = new QName(pfx, nsmap.get(pfx), local);
                                        if (implementations.containsKey(qname)) {
                                            logger.debug("Ignoring step property for configured step: " + qname.getClarkName());
                                        } else {
                                            Class<?> klass = Class.forName(name);
                                            logger.debug("Loaded step from property: " + qname.getClarkName());
                                            implementations.put(qname, klass);
                                        }
                                    } catch (ClassNotFoundException cfne) {
                                        logger.debug("Class not found, ignoring: " + name + " = " + value);
                                    }
                                } else {
                                    logger.debug("No namespace binding for " + pfx + ", ignoring: " + name + "=" + value);
                                }
                            } else {
                                logger.debug("Unparseable step QName: " + lexQName);
                            }
                        }
                    } else {
                        logger.debug("Unparseable property, ignoring: " + name + " = " + value);
                    }
                }
            }
        } catch (IOException ex) {
            logger.debug("Loading properties: " + ex.getMessage());
        }
    }

    private String fixUpURI(String uri) {
        File f = new File(uri);
        String fn = encode(f.getAbsolutePath());
        // FIXME: HACK!
        if ("\\".equals(getProperty("file.separator"))) {
            fn = "/" + fn;
        }
        return fn;
    }

    private void loadConfiguration() {
        URI home = URIUtils.homeAsURI();
        URI cwd = URIUtils.cwdAsURI();
        URI puri = home;

        String cfg = System.getProperty("com.xmlcalabash.config.global");
        try {
            InputStream instream = null;

            if (cfg == null) {
                instream = getClass().getResourceAsStream("/etc/configuration.xml");
                if (instream == null) {
                    throw new UnsupportedOperationException("Failed to load configuration from JAR file");
                }
                // No resolver, we don't have one yet
                SAXSource source = new SAXSource(new InputSource(instream));
                DocumentBuilder builder = cfgProcessor.newDocumentBuilder();
                builder.setLineNumbering(true);
                builder.setBaseURI(puri);
                parse(builder.build(source));
            } else {
                parse(readXML(cfg, cwd.toASCIIString()));
            }
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        cfg = System.getProperty("com.xmlcalabash.config.user", ".calabash");
        if ("".equals(cfg)) {
            // skip loading the user configuration
        } else {
            try {
                XdmNode cnode = readXML(cfg, home.toASCIIString());
                parse(cnode);
            } catch (XProcException xe) {
                if (XProcConstants.dynamicError(11).equals(xe.getErrorCode())) {
                    // nop; file not found is ok
                } else {
                    throw xe;
                }
            }
        }

        cfg = System.getProperty("com.xmlcalabash.config.local", ".calabash");
        if ("".equals(cfg)) {
            // skip loading the local configuration
        } else {
            try {
                XdmNode cnode = readXML(cfg, cwd.toASCIIString());
                parse(cnode);
            } catch (XProcException xe) {
                if (XProcConstants.dynamicError(11).equals(xe.getErrorCode())) {
                    // nop; file not found is ok
                } else {
                    throw xe;
                }
            }
        }

        // What about properties?
        saxonProcessor = System.getProperty("com.xmlcalabash.saxon-processor", saxonProcessor);

        if ( !("he".equals(saxonProcessor) || "pe".equals(saxonProcessor) || "ee".equals(saxonProcessor)) ) {
            throw new XProcException("Invalid Saxon processor specified in com.xmlcalabash.saxon-processor property.");
        }

        String saxonConfigProperty = System.getProperty("com.xmlcalabash.saxon-configuration");
        if (saxonConfigProperty != null) {
            saxonConfig = new Input("file://" + fixUpURI(saxonConfigProperty));
        }

        schemaAware = "true".equals(System.getProperty("com.xmlcalabash.schema-aware", ""+schemaAware));
        debug = "true".equals(System.getProperty("com.xmlcalabash.debug", ""+debug));
        showMessages = "true".equals(System.getProperty("com.xmlcalabash.show-messages", ""+showMessages));
        String profileProperty = System.getProperty("com.xmlcalabash.profile");
        if (profileProperty != null) {
            profile = new Output("file://" + fixUpURI(profileProperty));
        }
        extensionValues = "true".equals(System.getProperty("com.xmlcalabash.general-values", ""+extensionValues));
        xpointerOnText = "true".equals(System.getProperty("com.xmlcalabash.xpointer-on-text", ""+xpointerOnText));
        transparentJSON = "true".equals(System.getProperty("com.xmlcalabash.transparent-json", ""+transparentJSON));
        ignoreInvalidXmlBase = "true".equals(System.getProperty("com.xmlcalabash.ignore-invalid-xml-base", ""+ignoreInvalidXmlBase));
        allowTextResults = "true".equals(System.getProperty("com.xmlcalabash.allow-text-results", ""+allowTextResults));
        safeMode = "true".equals(System.getProperty("com.xmlcalabash.safe-mode", ""+safeMode));
        jsonFlavor = System.getProperty("com.xmlcalabash.json-flavor", jsonFlavor);
        useXslt10 = "true".equals(System.getProperty("com.xmlcalabash.use-xslt-10", ""+useXslt10));
        htmlSerializer = "true".equals(System.getProperty("com.xmlcalabash.html-serializer", ""+htmlSerializer));
        entityResolver = System.getProperty("com.xmlcalabash.entity-resolver", entityResolver);
        uriResolver = System.getProperty("com.xmlcalabash.uri-resolver", uriResolver);
        errorListener = System.getProperty("com.xmlcalabash.error-listener", errorListener);
        foProcessor = System.getProperty("com.xmlcalabash.fo-processor", foProcessor);
        cssProcessor = System.getProperty("com.xmlcalabash.css-processor", cssProcessor);
        xprocConfigurer = System.getProperty("com.xmlcalabash.xproc-configurer", xprocConfigurer);
        htmlParser = System.getProperty("com.xmlcalabash.html-parser", htmlParser);
        mailHost = System.getProperty("com.xmlcalabash.mail-host", mailHost);
        mailPort = System.getProperty("com.xmlcalabash.mail-port", mailPort);
        mailUser = System.getProperty("com.xmlcalabash.mail-username", mailUser);
        mailPass = System.getProperty("com.xmlcalabash.mail-password", mailPass);

        if ("true".equals(System.getProperty("proxySet"))) {
            String host = System.getProperty("proxyHost");
            String port = System.getProperty("proxyPort");
            if (host != null && port != null) {
                proxies.put("http", host + ":" + port);
            }
        }

        if (System.getProperty("com.xmlcalabash.log-style") != null) {
            String s = System.getProperty("com.xmlcalabash.log-style");
            if ("off".equals(s)) {
                logOpt = LogOptions.OFF;
            } else if ("plain".equals(s)) {
                logOpt = LogOptions.PLAIN;
            } else if ("wrapped".equals(s)) {
                logOpt = LogOptions.WRAPPED;
            } else if ("directory".equals(s)) {
                logOpt = LogOptions.DIRECTORY;
            } else {
                throw new XProcException("Invalid log-style specified in com.xmlcalabash.log-style property");
            }
        }

        if (System.getProperty("com.xmlcalabash.piperack-port") != null) {
            piperackPort = Integer.parseInt(System.getProperty("com.xmlcalabash.piperack-port"));
        }

        if (System.getProperty("com.xmlcalabash.piperack-default-expires") != null) {
            piperackDefaultExpires = Integer.parseInt(System.getProperty("com.xmlcalabash.piperack-port"));
        }

        String[] boolSerNames = new String[] {"byte-order-mark", "escape-uri-attributes",
                "include-content-type","indent", "omit-xml-declaration", "undeclare-prefixes"};
        String[] strSerNames = new String[] {"doctype-public", "doctype-system", "encoding",
                "media-type", "normalization-form", "version", "standalone", "cdata-section-elements"};

        for (String name : boolSerNames) {
            String s = System.getProperty("com.xmlcalabash.serial."+name);
            if ("true".equals(s) || "false".equals(s)) {
                serializationOptions.put(name, s);
            }
        }

        for (String name : strSerNames) {
            String s = System.getProperty("com.xmlcalabash.serial."+name);
            if (s != null) {
                serializationOptions.put(name, s);
            }
        }

        String method = System.getProperty("com.xmlcalabash.serial.method");
        if ("html".equals(method) || "xhtml".equals(method) || "text".equals(method) || "xml".equals(method)) {
            serializationOptions.put(method, method);
        }
    }

    public XdmNode readXML(String href, String base) {
        Source source = null;
        href = URIUtils.encode(href);

        try {
            URI baseURI = new URI(base);
            source = new SAXSource(new InputSource(baseURI.resolve(href).toASCIIString()));
        } catch (URISyntaxException use) {
            throw new XProcException(use);
        }

        // No resolver, we don't have one yet
        DocumentBuilder builder = cfgProcessor.newDocumentBuilder();
        builder.setLineNumbering(true);

        try {
            return builder.build(source);
        } catch (SaxonApiException sae) {
            throw new XProcException(XProcConstants.dynamicError(11), sae);
        }
    }


    public void parse(XdmNode doc) {
        if (doc.getNodeKind() == XdmNodeKind.DOCUMENT) {
            doc = S9apiUtils.getDocumentElement(doc);
        }

        for (XdmNode node : new AxisNodes(null, doc, Axis.CHILD, AxisNodes.PIPELINE)) {
            NamespaceUri uri = node.getNodeName().getNamespaceUri();
            String localName = node.getNodeName().getLocalName();

            if (XProcConstants.NS_CALABASH_CONFIG == uri
                    || XProcConstants.NS_EXPROC_CONFIG == uri) {
                if ("implementation".equals(localName)) {
                    parseImplementation(node);
                } else if ("saxon-processor".equals(localName)) {
                    parseSaxonProcessor(node);
                } else if ("saxon-configuration".equals(localName)) {
                    parseSaxonConfiguration(node);
                } else if ("schema-aware".equals(localName)) {
                    parseSchemaAware(node);
                } else if ("namespace-binding".equals(localName)) {
                    parseNamespaceBinding(node);
                } else if ("debug".equals(localName)) {
                    parseDebug(node);
                } else if ("show-messages".equals(localName)) {
                    parseShowMessages(node);
                } else if ("profile".equals(localName)) {
                    parseProfile(node);
                } else if ("entity-resolver".equals(localName)) {
                    parseEntityResolver(node);
                } else if ("input".equals(localName)) {
                    parseInput(node);
                } else if ("output".equals(localName)) {
                    parseOutput(node);
                } else if ("with-option".equals(localName)) {
                    parseWithOption(node);
                } else if ("with-param".equals(localName)) {
                    parseWithParam(node);
                } else if ("safe-mode".equals(localName)) {
                    parseSafeMode(node);
                } else if ("step-name".equals(localName)) {
                    parseStepName(node);
                } else if ("uri-resolver".equals(localName)) {
                    parseURIResolver(node);
                } else if ("step-error-listener".equals(localName)) {
                    parseErrorListener(node);
                } else if ("pipeline".equals(localName)) {
                    parsePipeline(node);
                } else if ("serialization".equals(localName)) {
                    parseSerialization(node);
                } else if ("extension-function".equals(localName)) {
                    parseExtensionFunction(node);
                } else if ("fo-processor".equals(localName)) {
                    parseFoProcessor(node);
                } else if ("css-processor".equals(localName)) {
                    parseCssProcessor(node);
                } else if ("xproc-configurer".equals(localName)) {
                    parseXProcConfigurer(node);
                } else if ("default-system-property".equals(localName)) {
                    parseSystemProperty(node);
                } else if ("extension".equals(localName)) {
                    parseExtension(node);
                } else if ("html-parser".equals(localName)) {
                    parseHtmlParser(node);
                } else if ("sendmail".equals(localName)) {
                    parseSendMail(node);
                } else if ("saxon-configuration-property".equals(localName)) {
                    saxonConfigurationProperty(node);
                } else if ("log-style".equals(localName)) {
                    logStyle(node);
                } else if ("proxy".equals(localName)) {
                    parseProxy(node);
                } else if ("pipeline-loader".equals(localName)) {
                    pipelineLoader(node);
                } else if ("piperack-port".equals(localName)) {
                    piperackPort(node);
                } else if ("piperack-default-expires".equals(localName)) {
                    piperackDefaultExpires(node);
                } else if ("piperack-load-pipeline".equals(localName)) {
                    piperackLoadPipeline(node);
                } else {
                    throw new XProcException(doc, "Unexpected configuration option: " + localName);
                }
            }
        }

        firstInput = true;
        firstOutput = true;
    }


	public boolean isStepAvailable(QName type) {
        if (implementations.containsKey(type)) {
            Class<?> klass = implementations.get(type);
            try {
                Method method = klass.getMethod("isAvailable");
                return (Boolean) method.invoke(null);
            } catch (NoSuchMethodException e) {
                return true; // Failure to implement the method...
            } catch (InvocationTargetException e) {
                return true; // ...or to implement it...
            } catch (IllegalAccessException e) {
                return true; // ...badly doesn't mean it's not available.
            }
        } else {
            return false;
        }
	}

	public XProcStep newStep(XProcRuntime runtime,XAtomicStep step){
        Class<?> klass = implementations.get(step.getType());
        if (klass == null) {
            throw new XProcException("Misconfigured. No 'class' in configuration for " + step.getType());
        }

        String className = klass.getName();
        // FIXME: This isn't really very secure...
        if (runtime.getSafeMode() && !className.startsWith("com.xmlcalabash.")) {
            throw XProcException.dynamicError(21);
        }

		try {
			Constructor<? extends XProcStep> constructor = Class.forName(className).asSubclass(XProcStep.class).getConstructor(XProcRuntime.class, XAtomicStep.class);
			return constructor.newInstance(runtime,step);
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

    public static void showVersion(XProcRuntime runtime) {
        System.out.println("XML Calabash version " + XProcConstants.XPROC_VERSION + ", an XProc processor.");
        if (runtime != null) {
            System.out.print("Running on Saxon version ");
            System.out.print(runtime.getConfiguration().getProcessor().getSaxonProductVersion());
            System.out.print(", ");
            System.out.print(runtime.getConfiguration().getProcessor().getUnderlyingConfiguration().getEditionCode());
            System.out.println(" edition.");
        }
        System.out.println("Copyright (c) 2007-2019 Norman Walsh");
        System.out.println("See docs/notices/NOTICES in the distribution for licensing");
        System.out.println("See also http://xmlcalabash.com/ for more information");
        System.out.println("");
    }

    private void parseSaxonProcessor(XdmNode node) {
        String value = node.getStringValue().trim();

        if ( !("he".equals(value) || "pe".equals(value) || "ee".equals(value)) ) {
            throw new XProcException(node, "Invalid Saxon processor: " + value + ". Must be 'he', 'pe', or 'ee'.");
        }

        saxonProcessor = value;
    }

    private void parseSaxonConfiguration(XdmNode node) {
        String value = node.getStringValue().trim();
        saxonConfig = new Input("file://" + fixUpURI(value));
    }

    private void parseSchemaAware(XdmNode node) {
        String value = node.getStringValue().trim();

        if (!"true".equals(value) && !"false".equals(value)) {
            throw new XProcException(node, "Invalid configuration value for schema-aware: "+ value);
        }

        schemaAware = "true".equals(value);
    }

    private void parseNamespaceBinding(XdmNode node) {
        String aname = node.getAttributeValue(_prefix);
        String avalue = node.getAttributeValue(_uri);
        nsBindings.put(aname, avalue);
    }

    private void parseDebug(XdmNode node) {
        String value = node.getStringValue().trim();
        debug = "true".equals(value);
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new XProcException(node, "Invalid configuration value for debug: "+ value);
        }
    }

    private void parseShowMessages(XdmNode node) {
        String value = node.getStringValue().trim();
        showMessages = "true".equals(value);
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new XProcException(node, "Invalid configuration value for show-messages: "+ value);
        }
    }

    private void parseProfile(XdmNode node) {
        profile = new Output("file://" + fixUpURI(node.getStringValue().trim()));
    }

    private void parseEntityResolver(XdmNode node) {
        String value = node.getAttributeValue(_class_name);
        if (value == null) {
            throw new XProcException(node, "Missing required attribute: class-name");
        }
        entityResolver = value;
    }

    private void parseExtensionFunction(XdmNode node) {
        String value = node.getAttributeValue(_class_name);
        if (value == null) {
            throw new XProcException(node, "Missing required attribute: class-name");
        }
        extensionFunctions.put(value, null);
    }

    private void parseFoProcessor(XdmNode node) {
        String value = node.getAttributeValue(_class_name);
        if (value == null) {
            throw new XProcException(node, "Missing required attribute: class-name");
        }
        foProcessor = value;
    }

    private void parseCssProcessor(XdmNode node) {
        String value = node.getAttributeValue(_class_name);
        if (value == null) {
            throw new XProcException(node, "Missing required attribute: class-name");
        }
        cssProcessor = value;
    }

    private void parseXProcConfigurer(XdmNode node) {
        String value = node.getAttributeValue(_class_name);
        if (value == null) {
            throw new XProcException(node, "Missing required attribute: class-name");
        }
        xprocConfigurer = value;
    }

    private void parseSystemProperty(XdmNode node) {
        String name = node.getAttributeValue(_name);
        String value = node.getAttributeValue(_value);
        if (name == null || value == null) {
            throw new XProcException("Configuration option 'default-system-property' cannot have null name or value");
        }
        if (System.getProperty(name) == null) {
            System.setProperty(name, value);
        }
    }

    private void parseExtension(XdmNode node) {
        String name = node.getAttributeValue(_name);
        String value = node.getAttributeValue(_value);
        if (name == null || value == null) {
            throw new XProcException("Configuration option 'extension' cannot have null name or value");
        }

        if ("general-values".equals(name)) {
            extensionValues = "true".equals(value);
        } else if ("xpointer-on-text".equals(name)) {
            xpointerOnText = "true".equals(value);
        } else if ("ignore-invalid-xml-base".equals(name)) {
            ignoreInvalidXmlBase = "true".equals(value);
        } else if ("transparent-json".equals(name)) {
            transparentJSON = "true".equals(value);
        } else if ("json-flavor".equals(name)) {
            jsonFlavor = value;
            if (! JSONtoXML.knownFlavor(jsonFlavor)) {
                throw new XProcException("Unrecognized JSON flavor: " + jsonFlavor);
            }
        } else if ("allow-text-results".equals(name)) {
            allowTextResults = "true".equals(value);
        } else if ("use-xslt-1.0".equals(name) || "use-xslt-10".equals(name)) {
            useXslt10 = "true".equals(value);
        } else if ("html-serializer".equals(name)) {
            htmlSerializer = "true".equals(value);
        } else {
            throw new XProcException("Unrecognized extension in configuration: " + name);
        }
    }

    private void parseHtmlParser(XdmNode node) {
        String value = node.getAttributeValue(_value);
        if (value == null) {
            throw new XProcException("Configuration option 'html-parser' cannot have null value");
        }

        if ("validator.nu".equals(value) || "tagsoup".equals(value)) {
            htmlParser = value;
        } else {
            throw new XProcException("Unrecognized value in html-parser: " + value);
        }
    }

    private void parseSendMail(XdmNode node) {
        String host = node.getAttributeValue(new QName("", "host"));
        String port = node.getAttributeValue(_port);
        String user = node.getAttributeValue(new QName("", "username"));
        String pass = node.getAttributeValue(new QName("", "password"));

        if (host != null) { mailHost = host; }
        if (port != null) { mailPort = port; }
        if (user != null) {
            mailUser = user;
            if (pass == null) {
                throw new XProcException("Misconfigured sendmail: user specified without password");
            }
            mailPass = pass;
        }
    }

    private void parseProxy(XdmNode node) {
        String host = node.getAttributeValue(new QName("", "host"));
        String port = node.getAttributeValue(_port);
        String scheme = node.getAttributeValue(new QName("", "scheme"));

        if (scheme == null) {
            scheme = "http";
        }

        if (host == null || port == null) {
            throw new XProcException("Misconfigured proxy: missing host or port");
        }

        proxies.put(scheme, host + ":" + port);
    }

    private void saxonConfigurationProperty(XdmNode node) {
        String value = node.getAttributeValue(_value);
        String key = node.getAttributeValue(_key);
        String type = node.getAttributeValue(_type);
        if (key == null || value == null) {
            throw new XProcException("Configuration option 'saxon-configuration-property' cannot have a null key or value");
        }

        if (bFeatureMap.containsKey(key) || iFeatureMap.containsKey(key) || sFeatureMap.containsKey(key)) {
            if (type.equals("boolean")) {
                if (bFeatureMap.containsKey(key)) {
                    Feature<Boolean> feature = bFeatureMap.get(key);
                    cfgProcessor.setConfigurationProperty(feature, "true".equals(value));
                } else {
                    throw new XProcException("Saxon feature is not boolean: " + key);
                }
            } else if (type.equals("integer")) {
                if (iFeatureMap.containsKey(key)) {
                    Feature<Integer> feature = iFeatureMap.get(key);
                    cfgProcessor.setConfigurationProperty(feature, Integer.parseInt(value));
                } else {
                    throw new XProcException("Saxon feature is not an integer: " + key);
                }
            } else {
                if (sFeatureMap.containsKey(key)) {
                    Feature<String> feature = sFeatureMap.get(key);
                    cfgProcessor.setConfigurationProperty(feature, value);
                } else {
                    throw new XProcException("Saxon feature is not a string: " + key);
                }
            }
        } else {
            throw new XProcException("Unknown Saxon feature: " + key);
        }
    }

    private void logStyle(XdmNode node) {
        String style = node.getAttributeValue(_value);
        if ("off".equals(style)) {
            logOpt = LogOptions.OFF;
        } else if ("plain".equals(style)) {
            logOpt = LogOptions.PLAIN;
        } else if ("wrapped".equals(style)) {
            logOpt = LogOptions.WRAPPED;
        } else if ("directory".equals(style)) {
            logOpt = LogOptions.DIRECTORY;
        } else {
            throw new XProcException("Configuration option 'log-style' must be one of 'off', 'plain', 'wrapped', or 'directory'");
        }
    }

    private void pipelineLoader(XdmNode node) {
        String data = node.getAttributeValue(_data);
        String href = node.getAttributeValue(_href);
        String loader = node.getAttributeValue(_loader);
        if ((data == null && href == null) || (data != null && href != null)) {
            throw new XProcException("Configuration option 'pipeline-loader' must have one of 'href' or 'data'");
        }
        if (loader == null) {
            throw new XProcException("Configuration option 'pipeline-loader' must specify a 'loader'");
        }

        if (data == null) {
            loaders.put("href:" + href, loader);
        } else {
            loaders.put("data:" + data, loader);
        }
    }

    private void piperackPort(XdmNode node) {
        String portno = node.getStringValue().trim();
        piperackPort = Integer.parseInt(portno);
    }

    private void piperackDefaultExpires(XdmNode node) {
        String secs = node.getStringValue().trim();
        piperackDefaultExpires = Integer.parseInt(secs);
    }

    private void piperackLoadPipeline(XdmNode node) {
        String uri = node.getStringValue().trim();
        String name = node.getAttributeValue(_name);
        int expires = -1;

        String s = node.getAttributeValue(_expires);
        if (s != null) {
            expires = Integer.parseInt(s);
        }

        if (name == null) {
            throw new XProcException(node, "You must specify a name for default pipelines.");
        }

        PipelineSource src = new PipelineSource(uri, name, expires);
        piperackDefaultPipelines.put(name, src);
    }

    private void parseInput(XdmNode node) {
        String port = node.getAttributeValue(_port);
        String href = node.getAttributeValue(_href);
        Vector<XdmValue> docnodes = new Vector<XdmValue> ();
        boolean sawElement = false;

        // FIXME: shouldn't this test for a "document" that doesn't have any document element?
        for (XdmNode child : new AxisNodes(null, node, Axis.CHILD, AxisNodes.ALL)) {
            if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                if (sawElement) {
                    throw new XProcException(node, "Invalid configuration value for input '" + port + "': content is not a valid XML document.");
                }
                sawElement = true;
            }
            docnodes.add(child);
        }

        if (firstInput) {
            inputs.clear();
            firstInput = false;
        }

        if (!inputs.containsKey(port)) {
            inputs.put(port, new Vector<ReadablePipe> ());
        }

        Vector<ReadablePipe> documents = inputs.get(port);

        if (href != null) {
            if (docnodes.size() > 0) {
                throw new XProcException(node, "Invalid configuration value for input '" + port + "': href and content on input.");
            }

            documents.add(new ConfigDocument(href, node.getBaseURI().toASCIIString()));
        } else {
            HashSet<NamespaceUri> excludeURIs = S9apiUtils.excludeInlinePrefixes(node, node.getAttributeValue(_exclude_inline_prefixes));
            documents.add(new ConfigDocument(docnodes, excludeURIs));
        }
    }

    private void parsePipeline(XdmNode node) {
        String href = node.getAttributeValue(_href);
        Vector<XdmValue> docnodes = new Vector<XdmValue> ();
        boolean sawElement = false;

        for (XdmNode child : new AxisNodes(null, node, Axis.CHILD, AxisNodes.PIPELINE)) {
            if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                if (sawElement) {
                    throw new XProcException(node, "Content of pipeline is not a valid XML document.");
                }
                sawElement = true;
            }
            docnodes.add(child);
        }

        if (href != null) {
            if (docnodes.size() > 0) {
                throw new XProcException(node, "XProcConfiguration error: href and content on pipeline");
            }
            pipeline = new ConfigDocument(href, node.getBaseURI().toASCIIString());
        } else {
            HashSet<NamespaceUri> excludeURIs = S9apiUtils.excludeInlinePrefixes(node, node.getAttributeValue(_exclude_inline_prefixes));
            pipeline = new ConfigDocument(docnodes, excludeURIs);
        }
    }

    private void parseOutput(XdmNode node) {
        String port = node.getAttributeValue(_port);
        String href = node.getAttributeValue(_href);

        for (XdmNode child : new AxisNodes(null, node, Axis.CHILD, AxisNodes.PIPELINE)) {
            if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                throw new XProcException(node, "Output must be empty.");
            }
        }

        if (firstOutput) {
            outputs.clear();
            firstOutput = false;
        }

        href = node.getBaseURI().resolve(href).toASCIIString();

        if ("-".equals(href) || href.startsWith("http:") || href.startsWith("https:") || href.startsWith("file:")) {
            outputs.put(port, href);
        } else {
            File f = new File(href);
            String fn = URIUtils.encode(f.getAbsolutePath());
            // FIXME: HACK!
            if ("\\".equals(System.getProperty("file.separator"))) {
                fn = "/" + fn;
            }
            outputs.put(port, "file://" + fn);
        }
    }

    private void parseWithOption(XdmNode node) {
        String nameStr = node.getAttributeValue(_name);
        String value = node.getAttributeValue(_value);

        QName name = new QName(nameStr,node);

        options.put(name,value);
    }

    private void parseWithParam(XdmNode node) {
        String port = node.getAttributeValue(_port);
        String nameStr = node.getAttributeValue(_name);
        String value = node.getAttributeValue(_value);

        QName name = new QName(nameStr,node);

        if (port == null) {
            port = "*";
        }

        HashMap<QName,String> pvalues;
        if (params.containsKey(port)) {
            pvalues = params.get(port);
        } else {
            pvalues = new HashMap<QName,String> ();
        }

        pvalues.put(name, value);

        params.put(port,pvalues);
    }

    private void parseSafeMode(XdmNode node) {
        String value = node.getStringValue().trim();

        safeMode = "true".equals(value);
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new XProcException(node, "Unexpected configuration value for safe-mode: "+ value);
        }
    }


    private void parseStepName(XdmNode node) {
        stepName = node.getStringValue().trim();
    }

    private void parseURIResolver(XdmNode node) {
        uriResolver = node.getAttributeValue(_class_name);
    }

    private void parseErrorListener(XdmNode node) {
        errorListener = node.getAttributeValue(_class_name);
    }

    private void parseImplementation(XdmNode node) {
        String nameStr = node.getAttributeValue(_type);
        String value = node.getAttributeValue(_class_name);

        if (nameStr == null || value == null) {
            throw new XProcException(node, "Unexpected implementation in configuration; must have both type and class-name attributes");
        }

        for (String tname : nameStr.split("\\s+")) {
            QName name = new QName(tname,node);
            try {
                Class<?> klass = Class.forName(value);
                logger.debug("Found step type annotation: " + name.getClarkName());
                if (implementations.containsKey(name)) {
                    logger.debug("Ignoring step type annotation for configured step: " + name.getClarkName());
                } else {
                    implementations.put(name, klass);
                }
            } catch (ClassNotFoundException e) {
                logger.debug("Class not found: " + value);
            } catch (NoClassDefFoundError e) {
                String msg = e.getMessage();
                logger.debug("Cannot instantiate " + value + ", missing class: " + msg);
            }
        }
    }

    private void parseSerialization(XdmNode node) {
        String[] attributeNames = new String[] {"byte-order-mark", "cdata-section-elements",
                "doctype-public", "doctype-system", "encoding", "escape-uri-attributes",
                "include-content-type", "indent", "media-type", "method", "normalization-form",
                "omit-xml-declaration", "standalone", "undeclare-prefixes", "version"};

        checkAttributes(node, attributeNames , false);

        for (String name : attributeNames) {
            String value = node.getAttributeValue(new QName(name));
            if (value == null) {
                continue;
            }

            if ("byte-order-mark".equals(name) || "escape-uri-attributes".equals(name)
                    || "include-content-type".equals(name) || "indent".equals(name)
                    || "omit-xml-declaration".equals(name) || "undeclare-prefixes".equals(name)) {
                checkBoolean(node, name, value);
                serializationOptions.put(name, value);
            } else if ("method".equals(name)) {
                QName methodName = new QName(value, node);
                if ("".equals(methodName.getPrefix())) {
                    String method = methodName.getLocalName();
                    if ("html".equals(method) || "xhtml".equals(method) || "text".equals(method) || "xml".equals(method)) {
                        serializationOptions.put(name, method);
                    } else {
                        throw new XProcException(node, "Configuration error: only the xml, xhtml, html, and text serialization methods are supported.");
                    }
                } else {
                    throw new XProcException(node, "Configuration error: only the xml, xhtml, html, and text serialization methods are supported.");
                }
            } else {
                serializationOptions.put(name, value);
            }

            for (XdmNode snode : new AxisNodes(null, node, Axis.CHILD, AxisNodes.PIPELINE)) {
                throw new XProcException(node, "Configuration error: serialization must be empty");
            }
        }
    }

    private void checkAttributes(XdmNode node, String[] attrs, boolean optionShortcutsOk) {
        HashSet<String> hash = new HashSet<> ();
        if (attrs != null) {
            Collections.addAll(hash, attrs);
        }
        HashSet<String> options = null;

        for (XdmNode attr : new AxisNodes(node, Axis.ATTRIBUTE)) {
            QName aname = attr.getNodeName();
            if (NamespaceUri.NULL == aname.getNamespaceUri()) {
                if (hash.contains(aname.getLocalName())) {
                    // ok
                } else if (optionShortcutsOk) {
                    if (options == null) {
                        options = new HashSet<String> ();
                    }
                    options.add(aname.getLocalName());
                } else {
                    throw new XProcException(node, "Configuration error: attribute \"" + aname + "\" not allowed on " + node.getNodeName());
                }
            } else if (XProcConstants.NS_XPROC == aname.getNamespaceUri()) {
                throw new XProcException(node, "Configuration error: attribute \"" + aname + "\" not allowed on " + node.getNodeName());
            }
            // Everything else is ok
        }
    }

    private void checkBoolean(XdmNode node, String name, String value) {
        if (value != null && !"true".equals(value) && !"false".equals(value)) {
            throw new XProcException(node, "Configuration error: " + name + " on serialization must be 'true' or 'false'");
        }
    }

    private class ConfigDocument implements ReadablePipe {
        private String href = null;
        private String base = null;
        private Vector<XdmValue> nodes = null;
        private boolean read = false;
        private XdmNode doc = null;
        private HashSet<NamespaceUri> excludeUris = null;

        public ConfigDocument(String href, String base) {
            this.href = href;
            this.base = base;
        }

        public ConfigDocument(Vector<XdmValue> nodes, HashSet<NamespaceUri> excludeUris) {
            this.nodes = nodes;
            this.excludeUris = excludeUris;
        }

        public void canReadSequence(boolean sequence) {
            // nop; always false
        }

        public boolean readSequence() {
            return false;
        }

        public XdmNode read() {
            read = true;

            if (doc != null) {
                return doc;
            }

            if (nodes != null) {
                // Find the document element so we can get the base URI
                XdmNode node = null;
                for (int pos = 0; pos < nodes.size() && node == null; pos++) {
                    if (((XdmNode) nodes.get(pos)).getNodeKind() == XdmNodeKind.ELEMENT) {
                        node = (XdmNode) nodes.get(pos);
                    }
                }
                assert node != null;

                XdmDestination dest = new XdmDestination();
                try {
                    S9apiUtils.writeXdmValue(cfgProcessor, nodes, dest, node.getBaseURI());
                    doc = dest.getXdmNode();
                    if (excludeUris.size() != 0) {
                        doc = S9apiUtils.removeNamespaces(cfgProcessor, doc, excludeUris, true);
                    }
                } catch (SaxonApiException sae) {
                    throw new XProcException(sae);
                }
            } else {
                doc = readXML(href, base);
            }

            return doc;
        }

        public void setReader(Step step) {
            // I don't care
        }

        public void setNames(String stepName, String portName) {
            // nop;
        }

        public void resetReader() {
            read = false;
        }

        public boolean moreDocuments() {
            return !read;
        }

        public boolean closed() {
            return false;
        }

        public int documentCount() {
            return 1;
        }

        public DocumentSequence documents() {
            throw new XProcException("You can't get the document sequence of an input from the config file!");
        }
    }


}
