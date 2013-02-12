package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XLibrary;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import java.io.File;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 3, 2008
 * Time: 6:37:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class ParseArgs {
    public boolean debugExplicit = false;
    public boolean debug = false;
    public String profileFile = null;
    public boolean showVersion = false;

    public String saxonProcessor = null;
    public String saxonConfigFile = null;
    public boolean schemaAwareExplicit = false;
    public boolean schemaAware = false;

    public boolean safeModeExplicit = false;
    public boolean safeMode = false;

    public String configFile = null;
    public String logStyle = null;
    public String entityResolverClass = null;
    public String uriResolverClass = null;
    private QName stepName = null;
    public String pipelineURI = null;
    private Vector<String> libraries = new Vector<String> ();
    public Hashtable<String, String> outputs = new Hashtable<String,String> ();
    private Hashtable<String, String> bindings = new Hashtable<String, String> ();
    private Vector<StepArgs> steps = new Vector<StepArgs> ();
    private StepArgs curStep = new StepArgs();
    private StepArgs lastStep = null;

    public boolean extensionValues = false;
    public boolean allowXPointerOnText = false;
    public boolean transparentJSON = false;
    public String jsonFlavor = null;
    public boolean useXslt10 = false;

    private String[] args = null;
    private int argpos = 0;
    private String arg = null;

    public void parse(String[] args) {
        this.args = args;
        argpos = 0;

        // Default the prefix "p" to the XProc namespace
        bindings.put("p", XProcConstants.NS_XPROC);

        while (arg != null || argpos < args.length) {
            if (arg == null) {
                arg = args[argpos];
            }

            if (arg.startsWith("-P") || arg.startsWith("--saxon-processor")) {
                saxonProcessor = parseString("P","saxon-processor");
                if ( !("he".equals(saxonProcessor) || "pe".equals(saxonProcessor) || "ee".equals(saxonProcessor)) ) {
                    throw new XProcException("Invalid Saxon processor option: " + saxonProcessor + ". Must be 'he', 'pe', or 'ee'.");
                }
                continue;
            }

            if (arg.startsWith("--saxon-configuration")) {
                saxonConfigFile = parseString(null, "saxon-configuration");
                continue;
            }

            if (arg.startsWith("-a") || arg.startsWith("--schema-aware")) {
                schemaAware = parseBoolean("a","schema-aware");
                schemaAwareExplicit = true;
                continue;
            }

            if (arg.startsWith("-D") || arg.startsWith("--debug")) {
                debug = parseBoolean("D","debug");
                debugExplicit = true;
                continue;
            }

            if (arg.startsWith("--profile")) {
                profileFile = parseString(null, "profile");
                continue;
            }

            if (arg.startsWith("-S") || arg.startsWith("--safe-mode")) {
                safeMode = parseBoolean("S","safe-mode");
                safeModeExplicit = true;
                continue;
            }

            if (arg.startsWith("-c") || arg.startsWith("--config")) {
                configFile = parseString("c","config");
                continue;
            }

            if (arg.startsWith("-G") || arg.startsWith("--log-style")) {
                logStyle = parseString("G","log-style");
                if ("off".equals(logStyle) || "plain".equals(logStyle)
                        || "wrapped".equals(logStyle) || "directory".equals(logStyle)) {
                    // ok
                } else {
                    throw new XProcException("Invalid log style: " + logStyle);
                }
                continue;
            }

            if (arg.startsWith("-E") || arg.startsWith("--entity-resolver")) {
                entityResolverClass = parseString("E","entity-resolver");
                continue;
            }

            if (arg.startsWith("-U") || arg.startsWith("--uri-resolver")) {
                uriResolverClass = parseString("U","uri-resolver");
                continue;
            }

            if (arg.startsWith("-i") || arg.equals("--input")) {
                parseInput("i", "input");
                continue;
            }

            if (arg.startsWith("-d") || arg.equals("--data-input")) {
                parseDataInput("d", "data-input");
                continue;
            }

            if (arg.startsWith("-o") || arg.equals("--output")) {
                parseOutput("o", "output");
                continue;
            }

            if (arg.startsWith("-b") || arg.equals("--binding")) {
                parseBinding("b","binding");
                continue;
            }

            if (arg.startsWith("-p") || arg.equals("--with-param")) {
                parseParam("p","with-param");
                continue;
            }

            if (arg.startsWith("-v") || arg.equals("--version")) {
                showVersion = parseBoolean("v","version");
                continue;
            }

            if (arg.startsWith("-s") || arg.startsWith("--step")) {
                stepName = parseQName("s","step");
                curStep.setName(stepName);
                steps.add(curStep);
                lastStep = curStep;
                curStep = new StepArgs();
                continue;
            }

            if (arg.startsWith("-l") || arg.startsWith("--library")) {
                String libraryURI = parseString("l","library");
                libraries.add(libraryURI);
                continue;
            }

            if (arg.startsWith("-X") || arg.startsWith("--extension")) {
                String ext = parseString("X", "extension");
                if ("general-values".equals(ext)) {
                    extensionValues = true;
                } else if ("xpointer-on-text".equals(ext)) {
                    allowXPointerOnText = true;
                } else if ("use-xslt-1.0".equals(ext) || "use-xslt-10".equals(ext)) {
                    useXslt10 = true;
                } else if ("transparent-json".equals(ext)) {
                    transparentJSON = true;
                } else if (ext.startsWith("json-flavor=")) {
                    jsonFlavor = ext.substring(12);
                    if (!JSONtoXML.knownFlavor(jsonFlavor)) {
                        throw new XProcException("Can't parse JSON flavor '" + ext + "' or unrecognized format: " + jsonFlavor);
                    }
                } else {
                    throw new XProcException("Unexpected extension: " + ext);
                }
                continue;
            }

            if (arg.startsWith("-")) {
                throw new XProcException("Unrecognized option: " + arg);
            }

            if (arg.contains("=")) {
                parseOption(arg);
                arg = null;
                argpos++;
                continue;
            } else {
                break;
            }
        }

        if (stepName != null && argpos < args.length && !args[argpos].contains("=")) {
            throw new XProcException("Bad command line. You can specify a step or a pipeline, not both.");
        }

        // FIXME: What about running the first pipeline in a library by default?
        if (libraries.size() == 0 && stepName == null && argpos < args.length) {
            pipelineURI = args[argpos++];
        }

        while (argpos < args.length) {
            if (args[argpos].startsWith("-")) {
                throw new XProcException("Only options can occur on the command line after the pipeline document.");
            }
            parseOption(args[argpos++]);
        }
    }

    public Set<String> getInputPorts() {
        if (steps.size() != 0) {
            // If we built a compound pipeline from the command line, then there aren't any pipeline inputs
            return new HashSet<String>();
        }
        return curStep.inputs.keySet();
    }

    public Vector<String> getInputs(String port) {
        if (steps.size() != 0) {
            // If we built a compound pipeline from the command line, then there aren't any pipeline inputs
            return new Vector<String> ();
        }
        return curStep.inputs.get(port);
    }

    public Set<QName> getOptionNames() {
        if (steps.size() != 0) {
            // If we built a compound pipeline from the command line, then there aren't any pipeline inputs
            return new HashSet<QName>();
        }
        return curStep.options.keySet();
    }

    public String getOption(QName name) {
        if (steps.size() != 0) {
            // If we built a compound pipeline from the command line, then there aren't any pipeline inputs
            return null;
        }
        return curStep.options.get(name);
    }

    public Set<String> getParameterPorts() {
        if (steps.size() != 0) {
            // If we built a compound pipeline from the command line, then there aren't any pipeline inputs
            return new HashSet<String>();
        }
        return curStep.params.keySet();
    }

    public Set<QName> getParameterNames(String port) {
        if (steps.size() != 0) {
            // If we built a compound pipeline from the command line, then there aren't any pipeline inputs
            return new HashSet<QName>();
        }
        return curStep.params.get(port).keySet();
    }

    public String getParameter(String port, QName name) {
        if (steps.size() != 0) {
            // If we built a compound pipeline from the command line, then there aren't any pipeline inputs
            return null;
        }
        return curStep.params.get(port).get(name);
    }

    public boolean impliedPipeline() {
        return steps.size() > 0 || libraries.size() > 0;
    }

    public XdmNode implicitPipeline(XProcRuntime runtime) {
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
        tree.addStartElement(XProcConstants.p_declare_step);
        tree.addAttribute(new QName("version"), "1.0");
        tree.startContent();

        tree.addStartElement(XProcConstants.p_input);
        tree.addAttribute(new QName("port"), "source");
        tree.addAttribute(new QName("sequence"), "true");
        tree.startContent();
        tree.addEndElement();

        tree.addStartElement(XProcConstants.p_input);
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
            tree.addStartElement(XProcConstants.p_output);
            tree.addAttribute(new QName("port"), port);
            tree.startContent();
            tree.addStartElement(XProcConstants.p_pipe);
            tree.addAttribute(new QName("step"), lastStepName);
            tree.addAttribute(new QName("port"), port);
            tree.startContent();
            tree.addEndElement();
            tree.addEndElement();
        }

        for (String library : libraries) {
            tree.addStartElement(XProcConstants.p_import);
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
                tree.addStartElement(XProcConstants.p_input);
                tree.addAttribute(new QName("port"), port);
                tree.startContent();

                for (String uri : step.inputs.get(port)) {
                    QName qname = XProcConstants.p_document;
                    if (uri.startsWith("xml:")) {
                        uri = uri.substring(4);
                    } else if (uri.startsWith("data:")) {
                        qname = XProcConstants.p_data;
                        uri = uri.substring(5);
                    } else {
                        throw new UnsupportedOperationException("Unexpected URI type: " + uri);
                    }
                    
                    if ("p:empty".equals(uri)) {
                        tree.addStartElement(XProcConstants.p_empty);
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

                    tree.addStartElement(XProcConstants.p_with_param);
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

    private boolean parseBoolean(String shortName, String longName) {
        String sOpt = "-" + shortName;
        String lOpt = "--" + longName;
        boolean bool = false;

        if (arg.startsWith(sOpt)) {
            bool = true;
            if (arg.equals(sOpt)) {
                arg = null;
                argpos++;
            } else {
                arg = "-" + arg.substring(2);
            }
            return bool;
        }

        if (arg.equals(lOpt)) {
            bool = true;
            arg = null;
            argpos++;
            return bool;
        }

        if (arg.startsWith(lOpt + "=")) {
            arg = arg.substring(lOpt.length()+1);

            if (!arg.equals("true") && !arg.equals("false")) {
                throw new XProcException("Unparseable command line argument: " + lOpt + "=" + arg);
            }

            bool = arg.equals("true");
            arg = null;
            argpos++;
            return bool;
        }

        throw new XProcException("Unparseable command line argument: " + arg);
    }

    private String parseString(String shortName, String longName) {
        String value = null;

        if (shortName != null) {
            String sOpt = "-" + shortName;
            if (arg.startsWith(sOpt)) {
                if (arg.equals(sOpt)) {
                    value = args[++argpos];
                    arg = null;
                    argpos++;
                } else {
                    value = arg.substring(2);
                    arg = null;
                    argpos++;
                }
                return value;
            }
        }

        String lOpt = "--" + longName;

        if (arg.equals(lOpt)) {
            value = args[++argpos];
            arg = null;
            argpos++;
            return value;
        }

        if (arg.startsWith(lOpt + "=")) {
            value = arg.substring(lOpt.length()+1);
            arg = null;
            argpos++;
            return value;
        }

        throw new XProcException("Unparseable command line argument: " + arg);
    }

    private QName parseQName(String shortName, String longName) {
        String sOpt = "-" + shortName;
        String lOpt = "--" + longName;
        String value = null;

        if (arg.startsWith(sOpt)) {
            if (arg.equals(sOpt)) {
                value = args[++argpos];
                arg = null;
                argpos++;
            } else {
                value = arg.substring(2);
                arg = null;
                argpos++;
            }
        } else if (arg.equals(lOpt)) {
            value = args[++argpos];
            arg = null;
            argpos++;
        } else if (arg.startsWith(lOpt + "=")) {
            value = arg.substring(lOpt.length()+1);
            arg = null;
            argpos++;
        } else {
            throw new XProcException("Unparseable command line argument: " + arg);
        }

        QName qname = null;

        int cpos = value.indexOf(":");
        if (cpos > 0) {
            String prefix = value.substring(0, cpos);
            if (!bindings.containsKey(prefix)) {
                throw new XProcException("Unbound prefix \"" + prefix + "\": " + value);
            }
            String uri = bindings.get(prefix);
            qname = new QName(prefix, uri, value.substring(cpos+1));
        } else {
            qname = new QName("", value);
        }

        return qname;
    }

    private KeyValuePair parseKeyValue(String shortName, String longName) {
        String sOpt = "-" + (shortName == null ? "" : shortName);
        String lOpt = "--" + longName;
        String opt = null;
        String oarg = arg;

        if (shortName != null && arg.startsWith(sOpt)) {
            if (arg.equals(sOpt)) {
                opt = args[++argpos];
                arg = null;
                argpos++;
            } else {
                opt = arg.substring(2);
                arg = null;
                argpos++;
            }
        } else if (arg.equals(lOpt)) {
            opt = args[++argpos];
            arg = null;
            argpos++;
        } else {
            throw new XProcException("Unparseable command line argument: " + arg);
        }

        String key = null;
        String value = null;

        int eqpos = opt.indexOf("=");
        if (eqpos > 0) {
            key = opt.substring(0,eqpos);
            value = opt.substring(eqpos+1);
        } else {
            key = opt;
            value = null;
        }

        if (value == null) {
            throw new XProcException("Unparseable command line argument: " + oarg);
        }

        return new KeyValuePair(key, value);
    }


    private void parseInput(String shortName, String longName) {
        KeyValuePair v = parseKeyValue(shortName, longName);

        String uri = v.value;
        if ("-".equals(uri) || uri.startsWith("http:") || uri.startsWith("https:") || uri.startsWith("file:")
                || "p:empty".equals(uri)) {
            curStep.addInput(v.key, uri, "xml");
        } else {
            File f = new File(uri);
            String fn = URIUtils.encode(f.getAbsolutePath());
            // FIXME: HACK!
            if ("\\".equals(System.getProperty("file.separator"))) {
                fn = "/" + fn;
            }
            curStep.addInput(v.key, "file://" + fn, "xml");
        }
    }

    private void parseDataInput(String shortName, String longName) {
        KeyValuePair v = parseKeyValue(shortName, longName);

        String uri = v.value;
        if ("-".equals(uri) || uri.startsWith("http:") || uri.startsWith("https:") || uri.startsWith("file:")
                || "p:empty".equals(uri)) {
            curStep.addInput(v.key, uri, "data");
        } else {
            File f = new File(uri);
            String fn = URIUtils.encode(f.getAbsolutePath());
            // FIXME: HACK!
            if ("\\".equals(System.getProperty("file.separator"))) {
                fn = "/" + fn;
            }
            curStep.addInput(v.key, "file://" + fn, "data");
        }
    }

    private void parseOutput(String shortName, String longName) {
        KeyValuePair v = parseKeyValue(shortName, longName);

        if (outputs.containsKey(v.key)) {
            throw new XProcException("Duplicate output binding: " + v.key);
        }

        String uri = v.value;
        if ("-".equals(uri)) {
            outputs.put(v.key, uri);
        } else {
            File f = new File(uri);
            String fn = URIUtils.encode(f.getAbsolutePath());
            // FIXME: HACK!
            if ("\\".equals(System.getProperty("file.separator"))) {
                fn = "/" + fn;
            }
            outputs.put(v.key, "file://" + fn);
        }
    }

    private void parseBinding(String shortName, String longName) {
        KeyValuePair v = parseKeyValue(shortName, longName);

        bindings.put(v.key, v.value);
    }

    private void parseParam(String shortName, String longName) {
        KeyValuePair v = parseKeyValue(shortName, longName);

        String port = "*";
        String name = v.key;
        String uri = null;
        QName qname = null;

        int cpos = name.indexOf("@");
        if (cpos > 0) {
            port = name.substring(0, cpos);
            name = name.substring(cpos+1);
        }

        cpos = name.indexOf(":");
        if (cpos > 0) {
            String prefix = name.substring(0, cpos);
            if (!bindings.containsKey(prefix)) {
                throw new XProcException("Unbound prefix \"" + prefix + "\": " + v.key + "=" + v.value);
            }
            uri = bindings.get(prefix);
            qname = new QName(prefix, uri, name.substring(cpos+1));
        } else {
            qname = new QName("", name);
        }

        curStep.addParameter(port, qname, v.value);
    }

    private void parseOption(String opt) {
        String key = null;
        String value = null;

        int eqpos = opt.indexOf("=");
        if (eqpos > 0) {
            key = opt.substring(0,eqpos);
            value = opt.substring(eqpos+1);
        } else {
            key = opt;
            value = null;
        }

        if (value == null) {
            throw new XProcException("Unparseable option: " + opt);
        }

        String uri = null;
        QName qname = null;

        int cpos = key.indexOf(":");
        if (cpos > 0) {
            String prefix = key.substring(0, cpos);
            if (!bindings.containsKey(prefix)) {
                throw new XProcException("Unbound prefix \"" + prefix + "\": " + key + "=" + value);
            }
            uri = bindings.get(prefix);
            qname = new QName(uri, key.substring(cpos+1), prefix);
        } else {
            qname = new QName("", key);
        }

        if (lastStep != null) {
            lastStep.addOption(qname, value);
        } else {
            curStep.addOption(qname, value);
        }
    }

    private class KeyValuePair {
        public String key = null;
        public String value = null;

        public KeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private class StepArgs {
        public QName stepName = null;
        public Hashtable<String, Vector<String>> inputs = new Hashtable<String,Vector<String>> ();
        public Hashtable<String, Hashtable<QName,String>> params = new Hashtable<String, Hashtable<QName,String>> ();
        public Hashtable<QName, String> options = new Hashtable<QName,String> ();

        public StepArgs() {
        }

        public void setName(QName name) {
            this.stepName = name;
        }

        public void addInput(String port, String uri, String type) {
            if (!inputs.containsKey(port)) {
                inputs.put(port, new Vector<String> ());
            }

            inputs.get(port).add(type + ":" + uri);
        }

        public void addOption(QName optname, String value) {
            if (options.containsKey(optname)) {
                throw new XProcException("Duplicate option name: " + optname);
            }

            options.put(optname, value);
        }

        public void addParameter(String port, QName name, String value) {
            Hashtable<QName,String> portParams;
            if (!params.containsKey(port)) {
                portParams = new Hashtable<QName,String> ();
            } else {
                portParams = params.get(port);
            }

            if (portParams.containsKey(name)) {
                throw new XProcException("Duplicate parameter name: " + name);
            }

            portParams.put(name, value);
            params.put(port, portParams);
        }
    }
}