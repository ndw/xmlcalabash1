package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcException;

import java.util.Vector;
import java.util.Hashtable;
import java.io.File;

import net.sf.saxon.s9api.QName;

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

    public boolean schemaAwareExplicit = false;
    public boolean schemaAware = false;

    public boolean safeModeExplicit = false;
    public boolean safeMode = false;

    public String configFile = null;
    public String logStyle = null;
    public String processLogFile = null;
    public String entityResolverClass = null;
    public String uriResolverClass = null;
    public QName stepName = null;
    public String libraryURI = null;
    public String pipelineURI = null;
    public Hashtable<String, Vector<String>> inputs = new Hashtable<String,Vector<String>> ();
    public Hashtable<String, String> outputs = new Hashtable<String,String> ();
    public Hashtable<String, Hashtable<QName,String>> params = new Hashtable<String, Hashtable<QName,String>> ();
    public Hashtable<QName, String> options = new Hashtable<QName,String> ();
    public Hashtable<String, String> bindings = new Hashtable<String, String> ();

    public boolean extensionValues = false;

    private String[] args = null;
    private int argpos = 0;
    private String arg = null;

    public void parse(String[] args) {
        this.args = args;
        argpos = 0;

        while (arg != null || argpos < args.length) {
            if (arg == null) {
                arg = args[argpos];
            }

            if (! arg.startsWith("-")) {
                break;
            }

            if (arg.startsWith("-a") || arg.startsWith("--schema-aware")) {
                schemaAware = parseBoolean("a","schema-aware");
                schemaAwareExplicit = true;
                continue;
            }

            if (arg.startsWith("-d") || arg.startsWith("--debug")) {
                debug = parseBoolean("d","debug");
                debugExplicit = true;
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


            if (arg.startsWith("-s") || arg.startsWith("--step")) {
                stepName = parseQName("s","step");
                continue;
            }

            if (arg.startsWith("-l") || arg.startsWith("--library")) {
                libraryURI = parseString("l","library");
                continue;
            }

            if (arg.startsWith("-X") || arg.startsWith("--extension")) {
                String ext = parseString("X", "extension");
                if ("general-values".equals(ext)) {
                    extensionValues = true;
                } else {
                    throw new XProcException("Unexpected extension name: " + ext);
                }
                continue;
            }

            if (arg.startsWith("-")) {
                throw new XProcException("Unrecognized option: " + arg);
            }
        }

        if (libraryURI == null && argpos < args.length) {
            pipelineURI = args[argpos++];
        }

        while (argpos < args.length) {
            if (args[argpos].startsWith("-")) {
                throw new XProcException("Only option bindings can occur on the command line after the pipeline document.");
            }
            parseOption(args[argpos++]);
        }
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
            return value;
        }

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
        String sOpt = "-" + shortName;
        String lOpt = "--" + longName;
        String opt = null;
        String oarg = arg;

        if (arg.startsWith(sOpt)) {
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

        if (!inputs.containsKey(v.key)) {
            inputs.put(v.key, new Vector<String> ());
        }

        String uri = v.value;
        if ("-".equals(uri) || uri.startsWith("http:") || uri.startsWith("https:") || uri.startsWith("file:")) {
            inputs.get(v.key).add(uri);
        } else {
            File f = new File(uri);
            String fn = URIUtils.encode(f.getAbsolutePath());
            // FIXME: HACK!
            if ("\\".equals(System.getProperty("file.separator"))) {
                fn = "/" + fn;
            }
            inputs.get(v.key).add("file://" + fn);
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

        if (bindings.containsKey(v.key)) {
            throw new XProcException("Duplicate namespace binding: " + v.key);
        }

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
            qname = new QName(uri, name.substring(cpos+1), prefix);
        } else {
            qname = new QName("", name);
        }

        Hashtable<QName,String> portParams;
        if (!params.containsKey(port)) {
            portParams = new Hashtable<QName,String> ();
        } else {
            portParams = params.get(port);
        }

        if (portParams.containsKey(qname)) {
            throw new XProcException("Duplicate parameter name: " + v.key);
        }

        portParams.put(qname, v.value);
        params.put(port, portParams);
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

        if (options.containsKey(qname)) {
            throw new XProcException("Duplicate option name: " + key);
        }

        options.put(qname, value);
    }

    private class KeyValuePair {
        public String key = null;
        public String value = null;

        public KeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

}
