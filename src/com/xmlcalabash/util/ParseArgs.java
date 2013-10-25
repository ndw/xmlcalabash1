package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcException;

import static com.xmlcalabash.util.Input.Type.DATA;
import static com.xmlcalabash.util.Input.Type.XML;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 3, 2008
 * Time: 6:37:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class ParseArgs {
    private String[] args = null;
    private int argpos = 0;
    private String arg = null;
    private UserArgs userArgs = new UserArgs();

    public UserArgs parse(String[] args) {
        this.args = args;
        argpos = 0;

        while (arg != null || argpos < args.length) {
            if (arg == null) {
                arg = args[argpos];
            }

            if (arg.startsWith("-P") || arg.startsWith("--saxon-processor")) {
                userArgs.setSaxonProcessor(parseString("P","saxon-processor"));
                continue;
            }

            if (arg.startsWith("--saxon-configuration")) {
                userArgs.setSaxonConfig(parseString(null, "saxon-configuration"));
                continue;
            }

            if (arg.startsWith("-a") || arg.startsWith("--schema-aware")) {
                userArgs.setSchemaAware(parseBoolean("a","schema-aware"));
                continue;
            }

            if (arg.startsWith("-D") || arg.startsWith("--debug")) {
                userArgs.setDebug(parseBoolean("D","debug"));
                continue;
            }

            if (arg.startsWith("--profile")) {
                userArgs.setProfile(parseString(null, "profile"));
                continue;
            }

            if (arg.startsWith("-S") || arg.startsWith("--safe-mode")) {
                userArgs.setSafeMode(parseBoolean("S","safe-mode"));
                continue;
            }

            if (arg.startsWith("-c") || arg.startsWith("--config")) {
                userArgs.setConfig(parseString("c", "config"));
                continue;
            }

            if (arg.startsWith("-G") || arg.startsWith("--log-style")) {
                userArgs.setLogStyle(parseString("G","log-style"));
                continue;
            }

            if (arg.startsWith("-E") || arg.startsWith("--entity-resolver")) {
                userArgs.setEntityResolverClass(parseString("E","entity-resolver"));
                continue;
            }

            if (arg.startsWith("-U") || arg.startsWith("--uri-resolver")) {
                userArgs.setUriResolverClass(parseString("U","uri-resolver"));
                continue;
            }

            if (arg.startsWith("-i") || arg.equals("--input")) {
                String s = parseString("i", "input");
                if (s.contains("=")) {
                    KeyValuePair v = parseOption(s);
                    userArgs.addInput(v.key, v.value, XML);
                } else {
                    userArgs.addInput(null, s, XML);
                }
                continue;
            }

            if (arg.startsWith("-d") || arg.equals("--data-input")) {
                String s = parseString("d", "data-input");
                String contentType = null;
                if (s.contains("@")) {
                    KeyValuePair v = parseOption(s, "@");
                    contentType = v.key;
                    s = v.value;
                }
                if (s.contains("=")) {
                    KeyValuePair v = parseOption(s);
                    userArgs.addInput(v.key, v.value, DATA, contentType);
                } else {
                    userArgs.addInput(null, s, DATA, contentType);
                }
                continue;
            }

            if (arg.startsWith("-o") || arg.equals("--output")) {
                String s = parseString("o", "output");
                if (s.contains("=")) {
                    KeyValuePair v = parseOption(s);
                    userArgs.addOutput(v.key, v.value);
                } else {
                    userArgs.addOutput(null, s);
                }
                continue;
            }

            if (arg.startsWith("-b") || arg.equals("--binding")) {
                KeyValuePair v = parseKeyValue("b", "binding");
                userArgs.addBinding(v.key, v.value);
                continue;
            }

            if (arg.startsWith("-p") || arg.equals("--with-param")) {
                KeyValuePair v = parseKeyValue("p", "with-param");
                userArgs.addParam(v.key, v.value);
                continue;
            }

            if (arg.startsWith("-v") || arg.equals("--version")) {
                userArgs.setShowVersion(parseBoolean("v","version"));
                continue;
            }

            if (arg.startsWith("-s") || arg.startsWith("--step")) {
                userArgs.setCurStepName(parseString("s","step"));
                continue;
            }

            if (arg.startsWith("-l") || arg.startsWith("--library")) {
                userArgs.addLibrary(parseString("l","library"));
                continue;
            }

            if (arg.startsWith("-X") || arg.startsWith("--extension")) {
                String ext = parseString("X", "extension");
                if ("general-values".equals(ext)) {
                    userArgs.setExtensionValues(true);
                } else if ("xpointer-on-text".equals(ext)) {
                    userArgs.setAllowXPointerOnText(true);
                } else if ("use-xslt-1.0".equals(ext) || "use-xslt-10".equals(ext)) {
                    userArgs.setUseXslt10(true);
                } else if ("transparent-json".equals(ext)) {
                    userArgs.setTransparentJSON(true);
                } else if (ext.startsWith("json-flavor=")) {
                    userArgs.setJsonFlavor(ext.substring(12));
                } else {
                    throw new XProcException("Unexpected extension: " + ext);
                }
                continue;
            }

            if (arg.startsWith("-")) {
                throw new XProcException("Unrecognized option: '" + arg + "'.");
            }

            if (arg.contains("=")) {
                KeyValuePair v = parseOption(arg);
                userArgs.addOption(v.key, v.value);
                arg = null;
                argpos++;
            } else {
                break;
            }
        }

        if (argpos < args.length) {
            userArgs.setPipeline(args[argpos++]);
        }

        while (argpos < args.length) {
            if (args[argpos].startsWith("-")) {
                throw new XProcException("Only options can occur on the command line after the pipeline document.");
            }
            KeyValuePair v = parseOption(args[argpos++]);
            userArgs.addOption(v.key, v.value);
        }

        userArgs.checkArgs();

        return userArgs;
    }

    public UserArgs parsePiperack(String[] args) {
        this.args = args;
        argpos = 0;

        while (arg != null || argpos < args.length) {
            if (arg == null) {
                arg = args[argpos];
            }

            if (arg.startsWith("-P") || arg.startsWith("--saxon-processor")) {
                userArgs.setSaxonProcessor(parseString("P","saxon-processor"));
                continue;
            }

            if (arg.startsWith("--saxon-configuration")) {
                userArgs.setSaxonConfig(parseString(null, "saxon-configuration"));
                continue;
            }

            if (arg.startsWith("-a") || arg.startsWith("--schema-aware")) {
                userArgs.setSchemaAware(parseBoolean("a","schema-aware"));
                continue;
            }

            if (arg.startsWith("-D") || arg.startsWith("--debug")) {
                userArgs.setDebug(parseBoolean("D","debug"));
                continue;
            }

            if (arg.startsWith("--profile")) {
                userArgs.setProfile(parseString(null, "profile"));
                continue;
            }

            if (arg.startsWith("-S") || arg.startsWith("--safe-mode")) {
                userArgs.setSafeMode(parseBoolean("S","safe-mode"));
                continue;
            }

            if (arg.startsWith("-c") || arg.startsWith("--config")) {
                userArgs.setConfig(parseString("c", "config"));
                continue;
            }

            if (arg.startsWith("-G") || arg.startsWith("--log-style")) {
                userArgs.setLogStyle(parseString("G","log-style"));
                continue;
            }

            if (arg.startsWith("-E") || arg.startsWith("--entity-resolver")) {
                userArgs.setEntityResolverClass(parseString("E","entity-resolver"));
                continue;
            }

            if (arg.startsWith("-U") || arg.startsWith("--uri-resolver")) {
                userArgs.setUriResolverClass(parseString("U","uri-resolver"));
                continue;
            }

            if (arg.startsWith("-v") || arg.equals("--version")) {
                userArgs.setShowVersion(parseBoolean("v","version"));
                continue;
            }

            if (arg.startsWith("--piperack-port")) {
                String s = parseString(null, "piperack-port");
                userArgs.setPiperackPort(Integer.parseInt(s));
                continue;
            }

            if (arg.startsWith("--piperack-default-expires")) {
                String s = parseString(null, "piperack-default-expires");
                userArgs.setPiperackExpires(Integer.parseInt(s));
                continue;
            }

            if (arg.startsWith("-X") || arg.startsWith("--extension")) {
                String ext = parseString("X", "extension");
                if ("general-values".equals(ext)) {
                    userArgs.setExtensionValues(true);
                } else if ("xpointer-on-text".equals(ext)) {
                    userArgs.setAllowXPointerOnText(true);
                } else if ("use-xslt-1.0".equals(ext) || "use-xslt-10".equals(ext)) {
                    userArgs.setUseXslt10(true);
                } else if ("transparent-json".equals(ext)) {
                    userArgs.setTransparentJSON(true);
                } else if (ext.startsWith("json-flavor=")) {
                    userArgs.setJsonFlavor(ext.substring(12));
                } else {
                    throw new XProcException("Unexpected extension: " + ext);
                }
                continue;
            }

            if (arg.startsWith("-")) {
                throw new XProcException("Unrecognized option: '" + arg + "'.");
            }

            if (arg.contains("=")) {
                KeyValuePair v = parseOption(arg);
                userArgs.addOption(v.key, v.value);
                arg = null;
                argpos++;
            } else {
                break;
            }
        }

        if (argpos < args.length) {
            userArgs.setPipeline(args[argpos++]);
        }

        while (argpos < args.length) {
            if (args[argpos].startsWith("-")) {
                throw new XProcException("Only options can occur on the command line after the pipeline document.");
            }
            KeyValuePair v = parseOption(args[argpos++]);
            userArgs.addOption(v.key, v.value);
        }

        userArgs.checkArgs();

        return userArgs;
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

    private KeyValuePair parseKeyValue(String shortName, String longName) {
        String sOpt = "-" + (shortName == null ? "" : shortName);
        String lOpt = "--" + longName;
        String opt = null;

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
            throw new XProcException("Unparseable command line argument: '" + arg + "'.");
        }

        return parseOption(opt);
    }

    private KeyValuePair parseOption(String opt) {
        return parseOption(opt, "=");
    }

    private KeyValuePair parseOption(String opt, String delimiter) {
        String key = null;
        String value = null;

        int delpos = opt.indexOf(delimiter);
        if (delpos > 0) {
            key = opt.substring(0,delpos);
            value = opt.substring(delpos+1);
        } else {
            throw new XProcException("Unparseable command line argument: '" + opt + "'.");
        }

        return new KeyValuePair(key, value);
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
