package com.xmlcalabash.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.sax.SAXSource;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XLibrary;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.xml.sax.InputSource;

import static com.xmlcalabash.core.XProcConstants.NS_XPROC;
import static com.xmlcalabash.core.XProcConstants.p_data;
import static com.xmlcalabash.core.XProcConstants.p_declare_step;
import static com.xmlcalabash.core.XProcConstants.p_document;
import static com.xmlcalabash.core.XProcConstants.p_empty;
import static com.xmlcalabash.core.XProcConstants.p_import;
import static com.xmlcalabash.core.XProcConstants.p_input;
import static com.xmlcalabash.core.XProcConstants.p_output;
import static com.xmlcalabash.core.XProcConstants.p_pipe;
import static com.xmlcalabash.core.XProcConstants.p_with_param;
import static com.xmlcalabash.util.JSONtoXML.knownFlavor;
import static com.xmlcalabash.util.LogOptions.DIRECTORY;
import static com.xmlcalabash.util.LogOptions.OFF;
import static com.xmlcalabash.util.LogOptions.PLAIN;
import static com.xmlcalabash.util.LogOptions.WRAPPED;
import static com.xmlcalabash.util.URIUtils.cwdAsURI;
import static com.xmlcalabash.util.URIUtils.encode;
import static java.lang.System.err;
import static java.lang.System.exit;
import static java.lang.System.getProperty;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static net.sf.saxon.s9api.QName.fromClarkName;

public class UserArgs {
    private boolean needsCheck = false;
    private Boolean debug = null;
    private String profileFile = null;
    private boolean showVersion = false;
    private String saxonProcessor = null;
    private String saxonConfigFile = null;
    private boolean schemaAware = false;
    private Boolean safeMode = null;
    private String configFile = null;
    private String logStyle = null;
    private String entityResolverClass = null;
    private String uriResolverClass = null;
    private String pipelineURI = null;
    private List<String> libraries = new ArrayList<String>();
    private Map<String, String> outputs = new HashMap<String, String>();
    private Map<String, String> bindings = new HashMap<String, String>();
    private List<StepArgs> steps = new ArrayList<StepArgs>();
    private StepArgs curStep = new StepArgs();
    private StepArgs lastStep = null;
    private boolean extensionValues = false;
    private boolean allowXPointerOnText = false;
    private boolean useXslt10 = false;
    private boolean transparentJSON = false;
    private String jsonFlavor = null;

    public UserArgs() {
        // Default the prefix "p" to the XProc namespace
        bindings.put("p", NS_XPROC);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setProfileFile(String profileFile) {
        this.profileFile = profileFile;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    public void setSaxonProcessor(String saxonProcessor) {
        needsCheck = true;
        this.saxonProcessor = saxonProcessor;
        if ( !("he".equals(saxonProcessor) || "pe".equals(saxonProcessor) || "ee".equals(saxonProcessor)) ) {
            throw new XProcException("Invalid Saxon processor option: '" + saxonProcessor + "'. Must be 'he' (default), 'pe' or 'ee'.");
        }
    }

    public void setSaxonConfigFile(String saxonConfigFile) {
        needsCheck = true;
        this.saxonConfigFile = saxonConfigFile;
    }

    public void setSchemaAware(boolean schemaAware) {
        needsCheck = true;
        this.schemaAware = schemaAware;
    }

    public void setSafeMode(boolean safeMode) {
        this.safeMode = safeMode;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public void setLogStyle(String logStyle) {
        this.logStyle = logStyle;
        if (!("off".equals(logStyle) || "plain".equals(logStyle)
              || "wrapped".equals(logStyle) || "directory".equals(logStyle))) {
            throw new XProcException("Invalid log style: '" + logStyle + "'. Must be 'off', 'plain', 'wrapped' (default) or 'directory'.");
        }
    }

    public void setEntityResolverClass(String entityResolverClass) {
        this.entityResolverClass = entityResolverClass;
    }

    public void setUriResolverClass(String uriResolverClass) {
        this.uriResolverClass = uriResolverClass;
    }

    public String getPipelineURI() {
        checkArgs();
        return pipelineURI;
    }

    public void setPipelineURI(String pipelineURI) {
        needsCheck = true;
        this.pipelineURI = pipelineURI;
    }

    public void addLibrary(String libraryURI) {
        needsCheck = true;
        libraries.add(libraryURI);
    }

    public Map<String, String> getOutputs() {
        checkArgs();
        return unmodifiableMap(outputs);
    }

    public void addOutput(String port, String uri) {
        if (outputs.containsKey(port)) {
            throw new XProcException("Duplicate output binding: '" + port + "'.");
        }

        if ("-".equals(uri)) {
            outputs.put(port, uri);
        } else {
            outputs.put(port, "file://" + fixUpURI(uri));
        }
    }

    public void addBinding(String prefix, String uri) {
        bindings.put(prefix, uri);
    }

    public void setCurStepName(String name) {
        needsCheck = true;
        curStep.setName(makeQName(name));
        steps.add(curStep);
        lastStep = curStep;
        curStep = new StepArgs();
    }

    public Set<String> getInputPorts() {
        checkArgs();
        if (steps.size() != 0) {
            // If we built a compound pipeline from the arguments, then there aren't any pipeline inputs
            return emptySet();
        }
        return unmodifiableSet(curStep.inputs.keySet());
    }

    public List<String> getInputs(String port) {
        checkArgs();
        if (steps.size() != 0) {
            // If we built a compound pipeline from the arguments, then there aren't any pipeline inputs
            return emptyList();
        }
        return unmodifiableList(curStep.inputs.get(port));
    }

    public void addInput(String port, String uri, String type) {
        if ("-".equals(uri) || uri.startsWith("http:") || uri.startsWith("https:") || uri.startsWith("file:")
                || "p:empty".equals(uri)) {
            curStep.addInput(port, uri, type);
        } else {
            curStep.addInput(port, "file://" + fixUpURI(uri), type);
        }
    }

    public Set<String> getParameterPorts() {
        checkArgs();
        if (steps.size() != 0) {
            // If we built a compound pipeline from the arguments, then there aren't any pipeline parameters
            return emptySet();
        }
        return unmodifiableSet(curStep.params.keySet());
    }

    public Map<QName, String> getParameters(String port) {
        checkArgs();
        if (steps.size() != 0) {
            // If we built a compound pipeline from the arguments, then there aren't any pipeline parameters
            return emptyMap();
        }
        return unmodifiableMap(curStep.params.get(port));
    }

    public void addParam(String name, String value) {
        String port = "*";

        int cpos = name.indexOf("@");
        if (cpos > 0) {
            port = name.substring(0, cpos);
            name = name.substring(cpos + 1);
        }

        curStep.addParameter(port, makeQName(name), value);
    }

    public Set<QName> getOptionNames() {
        checkArgs();
        if (steps.size() != 0) {
            // If we built a compound pipeline from the arguments, then there aren't any pipeline options
            return emptySet();
        }
        return unmodifiableSet(curStep.options.keySet());
    }

    public String getOption(QName name) {
        checkArgs();
        if (steps.size() != 0) {
            // If we built a compound pipeline from the arguments, then there aren't any pipeline options
            return null;
        }
        return curStep.options.get(name);
    }

    public void addOption(String name, String value) {
        QName qname = makeQName(name);
        if (lastStep != null) {
            lastStep.addOption(qname, value);
        } else {
            curStep.addOption(qname, value);
        }
    }

    public void setExtensionValues(boolean extensionValues) {
        this.extensionValues = extensionValues;
    }

    public void setAllowXPointerOnText(boolean allowXPointerOnText) {
        this.allowXPointerOnText = allowXPointerOnText;
    }

    public void setUseXslt10(boolean useXslt10) {
        this.useXslt10 = useXslt10;
    }

    public void setTransparentJSON(boolean transparentJSON) {
        this.transparentJSON = transparentJSON;
    }

    public void setJsonFlavor(String jsonFlavor) {
        this.jsonFlavor = jsonFlavor;
        if (!knownFlavor(jsonFlavor)) {
            throw new XProcException("Unknown JSON flavor: '" + jsonFlavor + "'.");
        }
    }

    /**
     * This method does some sanity checks and should be called at the
     * beginning of every public method that has a return value to make
     * sure that no invalid argument combinations are used.
     *
     * It is public so that it can also be invoked explicitly to control
     * the time when these checks are done.
     *
     * @throws XProcException if something is not valid in the arguments
     */
    public void checkArgs() {
        if (needsCheck) {
            if (hasImplicitPipelineInternal() && (pipelineURI != null)) {
                throw new XProcException("You can specify a library and / or steps or a pipeline, but not both.");
            }

            if (saxonConfigFile != null) {
                if (schemaAware) {
                    throw new XProcException("Specifying schema-aware processing is an error if you specify a Saxon configuration file.");
                }
                if (saxonProcessor != null) {
                    throw new XProcException("Specifying a processor type is an error if you specify a Saxon configuration file.");
                }
            }

            needsCheck = false;
        }
    }

    public XProcConfiguration createConfiguration() throws SaxonApiException {
        checkArgs();
        XProcConfiguration config = null;

        // Blech
        try {
            String proc = saxonProcessor;
            if (schemaAware) {
                proc = "ee";
            }

            if (saxonConfigFile != null) {
                config = new XProcConfiguration(saxonConfigFile);
            } else if (proc != null) {
                config = new XProcConfiguration(proc, schemaAware);
            } else {
                config = new XProcConfiguration();
            }
        } catch (Exception e) {
            err.println("FATAL: Failed to parse Saxon configuration file.");
            err.println(e);
            exit(2);
        }

        if (configFile != null) {
            // Make this absolute because sometimes it fails from the command line otherwise. WTF?
            String cfgURI = cwdAsURI().resolve(configFile).toASCIIString();
            SAXSource source = new SAXSource(new InputSource(cfgURI));
            // No resolver, we don't have one yet
            DocumentBuilder builder = config.getProcessor().newDocumentBuilder();
            XdmNode doc = builder.build(source);
            config.parse(doc);
        }

        if (logStyle != null) {
            if (logStyle.equals("off")) {
                config.logOpt = OFF;
            } else if (logStyle.equals("plain")) {
                config.logOpt = PLAIN;
            } else if (logStyle.equals("directory")) {
                config.logOpt = DIRECTORY;
            } else {
                config.logOpt = WRAPPED;
            }
        }

        if (uriResolverClass != null) {
            config.uriResolver = uriResolverClass;
        }

        if (entityResolverClass != null) {
            config.entityResolver = entityResolverClass;
        }

        if (safeMode != null) {
            config.safeMode = safeMode;
        }

        if (debug != null) {
            config.debug = debug;
        }

        if (profileFile != null) {
            config.profileFile = profileFile;
        }

        config.extensionValues |= extensionValues;
        config.xpointerOnText |= allowXPointerOnText;
        config.transparentJSON |= transparentJSON;
        if ((jsonFlavor != null) && !knownFlavor(jsonFlavor)) {
            config.jsonFlavor = jsonFlavor;
        }
        config.useXslt10 |= useXslt10;

        return config;
    }

    /**
     * Helper method to prevent an endless-loop when using
     * {@link #hasImplicitPipeline} from within {@link #checkArgs()}
     *
     * @return whether an implicit pipeline is used
     */
    private boolean hasImplicitPipelineInternal() {
        return (steps.size() > 0) || (libraries.size() > 0);
    }

    public boolean hasImplicitPipeline() {
        checkArgs();
        return hasImplicitPipelineInternal();
    }

    public XdmNode getImplicitPipeline(XProcRuntime runtime) {
        checkArgs();
        // This is a bit of a hack...
        if (steps.size() == 0 && libraries.size() == 1) {
            try {
                XLibrary library = runtime.loadLibrary(libraries.get(0));
                curStep.setName(library.getFirstPipelineType());
                steps.add(curStep);
            } catch (SaxonApiException sae) {
                throw new XProcException(sae);
            }
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(runtime.getStaticBaseURI());
        tree.addStartElement(p_declare_step);
        tree.addAttribute(new QName("version"), "1.0");
        tree.startContent();

        tree.addStartElement(p_input);
        tree.addAttribute(new QName("port"), "source");
        tree.addAttribute(new QName("sequence"), "true");
        tree.startContent();
        tree.addEndElement();

        tree.addStartElement(p_input);
        tree.addAttribute(new QName("port"), "parameters");
        tree.addAttribute(new QName("kind"), "parameter");
        tree.startContent();
        tree.addEndElement();

        // This is a hack too. If there are no outputs, fake one.
        // Implicit pipelines default to having a single primary output port names "result"
        if (outputs.size() == 0) {
            outputs.put("result", "-");
        }

        String lastStepName = "cmdlineStep" + steps.size();
        for (String port : outputs.keySet()) {
            tree.addStartElement(p_output);
            tree.addAttribute(new QName("port"), port);
            tree.startContent();
            tree.addStartElement(p_pipe);
            tree.addAttribute(new QName("step"), lastStepName);
            tree.addAttribute(new QName("port"), port);
            tree.startContent();
            tree.addEndElement();
            tree.addEndElement();
        }

        for (String library : libraries) {
            tree.addStartElement(p_import);
            tree.addAttribute(new QName("href"), library);
            tree.startContent();
            tree.addEndElement();
        }

        int stepNum = 0;
        for (StepArgs step : steps) {
            stepNum ++;

            tree.addStartElement(step.stepName);
            tree.addAttribute(new QName("name"), "cmdlineStep" + stepNum);

            for (QName optname : step.options.keySet()) {
                tree.addAttribute(optname, step.options.get(optname));
            }

            tree.startContent();

            for (String port : step.inputs.keySet()) {
                tree.addStartElement(p_input);
                tree.addAttribute(new QName("port"), port);
                tree.startContent();

                for (String uri : step.inputs.get(port)) {
                    QName qname = p_document;
                    if (uri.startsWith("xml:")) {
                        uri = uri.substring(4);
                    } else if (uri.startsWith("data:")) {
                        qname = p_data;
                        uri = uri.substring(5);
                    } else {
                        throw new UnsupportedOperationException("Unexpected URI type: " + uri);
                    }

                    if ("p:empty".equals(uri)) {
                        tree.addStartElement(p_empty);
                        tree.startContent();
                        tree.addEndElement();
                    } else {
                        tree.addStartElement(qname);
                        tree.addAttribute(new QName("href"), uri);
                        tree.startContent();
                        tree.addEndElement();
                    }
                }
                tree.addEndElement();
            }

            for (String port : step.params.keySet()) {
                for (QName pname : step.params.get(port).keySet()) {
                    String value = step.params.get(port).get(pname);

                    if (value.contains("'") && value.contains("\"")) {
                        throw new IllegalArgumentException("I haven't figured out how to handle parameter values with both double and single quotes.");
                    } else if (value.contains("'")) {
                        value = "\"" + value + "\"";
                    } else {
                        value = "'" + value + "'";
                    }

                    tree.addStartElement(p_with_param);
                    if (!"*".equals(port)) {
                        tree.addAttribute(new QName("port"), port);
                    }
                    tree.addAttribute(new QName("name"), pname.toString());
                    tree.addAttribute(new QName("select"), value);
                    tree.startContent();
                    tree.addEndElement();
                }
            }

            tree.addEndElement();
            tree.endDocument();
        }

        tree.addEndElement();
        tree.endDocument();

        return tree.getResult();
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

    private QName makeQName(String name) {
        QName qname;

        if (name == null) {
            qname = new QName("");
        } else if (name.indexOf("{") == 0) {
            qname = fromClarkName(name);
        } else {
            int cpos = name.indexOf(":");
            if (cpos > 0) {
                String prefix = name.substring(0, cpos);
                if (!bindings.containsKey(prefix)) {
                    throw new XProcException("Unbound prefix '" + prefix + "' in: '" + name + "'.");
                }
                String uri = bindings.get(prefix);
                qname = new QName(prefix, uri, name.substring(cpos + 1));
            } else {
                qname = new QName("", name);
            }
        }

        return qname;
    }

    private class StepArgs {
        public QName stepName = null;
        public Map<String, List<String>> inputs = new HashMap<String, List<String>>();
        public Map<String, Map<QName,String>> params = new HashMap<String, Map<QName, String>>();
        public Map<QName, String> options = new HashMap<QName, String>();

        public void setName(QName name) {
            this.stepName = name;
        }

        public void addInput(String port, String uri, String type) {
            if (!inputs.containsKey(port)) {
                inputs.put(port, new ArrayList<String>());
            }

            inputs.get(port).add(type + ":" + uri);
        }

        public void addOption(QName optname, String value) {
            if (options.containsKey(optname)) {
                throw new XProcException("Duplicate option name: '" + optname + "'.");
            }

            options.put(optname, value);
        }

        public void addParameter(String port, QName name, String value) {
            Map<QName,String> portParams;
            if (!params.containsKey(port)) {
                portParams = new HashMap<QName,String> ();
            } else {
                portParams = params.get(port);
            }

            if (portParams.containsKey(name)) {
                throw new XProcException("Duplicate parameter name: '" + name + "'.");
            }

            portParams.put(name, value);
            params.put(port, portParams);
        }
    }
}
