/*
 * Main.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * Portions Copyright 2007 Sun Microsystems, Inc.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://xproc.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */

package com.xmlcalabash.drivers;

import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;
import java.net.URISyntaxException;

import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.URIUtils;
import com.xmlcalabash.util.ParseArgs;
import com.xmlcalabash.util.LogOptions;

/**
 *
 * @author ndw
 */
public class Main {
    private static boolean errors = false;
    private static QName _code = new QName("code");
    private static int exitStatus = 0;
    private XProcRuntime runtime = null;
    private boolean readStdin = false;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private boolean debug = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SaxonApiException, IOException, URISyntaxException {
        Main main = new Main();
        main.run(args);
        System.exit(exitStatus);
    }

    public void run(String[] args) throws SaxonApiException, IOException, URISyntaxException {
        ParseArgs cmd = new ParseArgs();
        try {
            cmd.parse(args);
        } catch (XProcException xe) {
            System.err.println(xe.getMessage());
            usage();
        }

        if (cmd.saxonConfigFile != null) {
            if (cmd.schemaAware) {
                throw new XProcException("Specifying schema-aware processing is an error if you specify a Saxon configuration file.");
            }
            if (cmd.saxonProcessor != null) {
                throw new XProcException("Specifying a processor type is an error if you specify a Saxon configuration file.");
            }
        }

        try {
            XProcConfiguration config = null;

            // Blech
            try {
                String proc = cmd.saxonProcessor;
                if (cmd.schemaAware) {
                    proc = "ee";
                }

                if (cmd.saxonConfigFile != null) {
                    config = new XProcConfiguration(cmd.saxonConfigFile);
                } else if (proc != null) {
                    config = new XProcConfiguration(proc, cmd.schemaAware);
                } else {
                    config = new XProcConfiguration();
                }
            } catch (Exception e) {
                System.err.println("FATAL: Failed to parse configuration file.");
                System.err.println(e);
                System.exit(2);
            }

            if (cmd.configFile != null) {
                // Make this absolute because sometimes it fails from the command line otherwise. WTF?
                String cfgURI = URIUtils.cwdAsURI().resolve(cmd.configFile).toASCIIString();
                SAXSource source = new SAXSource(new InputSource(cfgURI));
                // No resolver, we don't have one yet
                DocumentBuilder builder = config.getProcessor().newDocumentBuilder();
                XdmNode doc = builder.build(source);
                config.parse(doc);
            }

            if (cmd.logStyle != null) {
                if (cmd.logStyle.equals("off")) {
                    config.logOpt = LogOptions.OFF;
                } else if (cmd.logStyle.equals("plain")) {
                    config.logOpt = LogOptions.PLAIN;
                } else if (cmd.logStyle.equals("directory")) {
                    config.logOpt = LogOptions.DIRECTORY;
                } else {
                    config.logOpt = LogOptions.WRAPPED;
                }
            }

            if (cmd.uriResolverClass != null) {
                config.uriResolver = cmd.uriResolverClass;
            }

            if (cmd.entityResolverClass != null) {
                config.entityResolver = cmd.entityResolverClass;
            }

            if (cmd.safeModeExplicit) {
                config.safeMode = cmd.safeMode;
            }
            
            if (cmd.debugExplicit) {
                config.debug = cmd.debug;
            }

            if (cmd.profileFile != null) {
                config.profileFile = cmd.profileFile;
            }

            config.extensionValues |= cmd.extensionValues;
            config.xpointerOnText |= cmd.allowXPointerOnText;
            config.transparentJSON |= cmd.transparentJSON;
            if (cmd.jsonFlavor != null) {
                config.jsonFlavor = cmd.jsonFlavor;
            }
            config.useXslt10 |= cmd.useXslt10;

            debug = config.debug;

            runtime = new XProcRuntime(config);

            XPipeline pipeline = null;

            if (cmd.showVersion) {
                showVersion();
            }

            if (cmd.pipelineURI != null) {
                pipeline = runtime.load(cmd.pipelineURI);
            } else if (cmd.impliedPipeline()) {
                XdmNode implicitPipeline = cmd.implicitPipeline(runtime);

                if (debug) {
                    System.err.println("Implicit pipeline:");

                    Serializer serializer = new Serializer();

                    serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
                    serializer.setOutputProperty(Serializer.Property.METHOD, "xml");

                    serializer.setOutputStream(System.err);

                    S9apiUtils.serialize(runtime, implicitPipeline, serializer);
                }

                pipeline=runtime.use(implicitPipeline);
                
            } else if (config.pipeline != null) {
                XdmNode doc = config.pipeline.read();
                pipeline = runtime.use(doc);
            }
            
            if (errors || pipeline == null) {
                usage();
            }

            // Process parameters from the configuration...
            for (String port : config.params.keySet()) {
                Hashtable<QName,String> hash = config.params.get(port);
                if ("*".equals(port)) {
                    for (QName name : hash.keySet()) {
                        pipeline.setParameter(name, new RuntimeValue(hash.get(name)));
                    }
                } else {
                    for (QName name : hash.keySet()) {
                        pipeline.setParameter(port, name, new RuntimeValue(hash.get(name)));
                    }
                }
            }

            // Now process parameters from the command line...
            for (String port : cmd.getParameterPorts()) {
                for (QName name : cmd.getParameterNames(port)) {
                    if ("*".equals(port)) {
                        pipeline.setParameter(name, new RuntimeValue(cmd.getParameter(port, name)));
                    } else {
                        pipeline.setParameter(port, name, new RuntimeValue(cmd.getParameter(port,name)));
                    }
                }
            }

            Set<String> ports = pipeline.getInputs();
            Set<String> cmdPorts = cmd.getInputPorts();
            Set<String> cfgPorts = config.inputs.keySet();
            HashSet<String> allPorts = new HashSet<String>();
            allPorts.addAll(cmdPorts);
            allPorts.addAll(cfgPorts);

            for (String port : allPorts) {
                if (!ports.contains(port)) {
                    throw new XProcException("There is a binding for the port '" + port + "' but the pipeline declares no such port.");
                }

                pipeline.clearInputs(port);

                if (cmdPorts.contains(port)) {
                    XdmNode doc = null;
                    for (String uri : cmd.getInputs(port)) {
                        if (uri.startsWith("xml:")) {
                            uri = uri.substring(4);

                            if ("-".equals(uri)) {
                                doc = runtime.parse(new InputSource(System.in));
                            } else {
                                doc = runtime.parse(new InputSource(uri));
                            }
                        } else if (uri.startsWith("data:")) {
                            uri = uri.substring(5);
                            ReadableData rd = new ReadableData(runtime, XProcConstants.c_data, uri, "text/plain");
                            doc = rd.read();
                        } else {
                            throw new UnsupportedOperationException("Unexpected input type: " + uri);
                        }

                        pipeline.writeTo(port, doc);
                    }
                } else {
                    for (ReadablePipe pipe : config.inputs.get(port)) {
                        XdmNode doc = pipe.read();
                        pipeline.writeTo(port, doc);
                    }
                }
            }

            String stdio = null;

            // Implicit binding for stdin?
            String implicitPort = null;
            for (String port : pipeline.getInputs()) {
                if (!allPorts.contains(port)) {
                    if (pipeline.getDeclareStep().getInput(port).getPrimary()
                            && !pipeline.getDeclareStep().getInput(port).getParameterInput()) {
                        implicitPort = port;
                    }
                }
            }

            if (implicitPort != null && !pipeline.hasReadablePipes(implicitPort)) {
                XdmNode doc = runtime.parse(new InputSource(System.in));
                pipeline.writeTo(implicitPort, doc);
            }

            // Look for explicit binding to "-"
            for (String port : pipeline.getOutputs()) {
                String uri = null;

                if (cmd.outputs.containsKey(port)) {
                    uri = cmd.outputs.get(port);
                } else if (config.outputs.containsKey(port)) {
                    uri = config.outputs.get(port);
                }

                if ("-".equals(uri) && stdio == null) {
                    stdio = port;
                }
            }

            // Look for implicit binding to "-"
            for (String port : pipeline.getOutputs()) {
                String uri = null;

                if (cmd.outputs.containsKey(port)) {
                    uri = cmd.outputs.get(port);
                } else if (config.outputs.containsKey(port)) {
                    uri = config.outputs.get(port);
                }

                if (uri == null) {
                    if (stdio == null) {
                        stdio = port;
                    } else {
                        warning(logger, null, "You didn't specify any binding for the output port '" + port + "', its output will be discard.");
                    }
                }
            }

            for (QName optname : config.options.keySet()) {
                RuntimeValue value = new RuntimeValue(config.options.get(optname), null, null);
                pipeline.passOption(optname, value);
            }

            for (QName optname : cmd.getOptionNames()) {
                RuntimeValue value = new RuntimeValue(cmd.getOption(optname), null, null);
                pipeline.passOption(optname, value);
            }

            pipeline.run();

            for (String port : pipeline.getOutputs()) {
                String uri = null;
                if (cmd.outputs.containsKey(port)) {
                    uri = cmd.outputs.get(port);
                } else if (config.outputs.containsKey(port)) {
                    uri = config.outputs.get(port);
                }

                if (port.equals(stdio)) {
                    finest(logger, null, "Copy output from " + port + " to stdout");
                    uri = null;
                } else if (uri == null) {
                    // You didn't bind it, and it isn't going to stdout, so it's going into the bit bucket.
                    continue;
                } else {
                    finest(logger, null, "Copy output from " + port + " to " + uri);
                }

                Serialization serial = pipeline.getSerialization(port);

                if (serial == null) {
                    // Use the configuration options
                    // FIXME: should each of these be considered separately?
                    // FIXME: should there be command-line options to override these settings?
                    serial = new Serialization(runtime, pipeline.getNode()); // The node's a hack
                    for (String name : config.serializationOptions.keySet()) {
                        String value = config.serializationOptions.get(name);

                        if ("byte-order-mark".equals(name)) serial.setByteOrderMark("true".equals(value));
                        if ("escape-uri-attributes".equals(name)) serial.setEscapeURIAttributes("true".equals(value));
                        if ("include-content-type".equals(name)) serial.setIncludeContentType("true".equals(value));
                        if ("indent".equals(name)) serial.setIndent("true".equals(value));
                        if ("omit-xml-declaration".equals(name)) serial.setOmitXMLDeclaration("true".equals(value));
                        if ("undeclare-prefixes".equals(name)) serial.setUndeclarePrefixes("true".equals(value));
                        if ("method".equals(name)) serial.setMethod(new QName("", value));

                        // FIXME: if ("cdata-section-elements".equals(name)) serial.setCdataSectionElements();
                        if ("doctype-public".equals(name)) serial.setDoctypePublic(value);
                        if ("doctype-system".equals(name)) serial.setDoctypeSystem(value);
                        if ("encoding".equals(name)) serial.setEncoding(value);
                        if ("media-type".equals(name)) serial.setMediaType(value);
                        if ("normalization-form".equals(name)) serial.setNormalizationForm(value);
                        if ("standalone".equals(name)) serial.setStandalone(value);
                        if ("version".equals(name)) serial.setVersion(value);
                    }
                }

                // I wonder if there's a better way...
                WritableDocument wd = null;
                if (uri != null) {
                    URI furi = new URI(uri);
                    String filename = furi.getPath();
                    FileOutputStream outfile = new FileOutputStream(filename);
                    wd = new WritableDocument(runtime,filename,serial,outfile);
                } else {
                    wd = new WritableDocument(runtime,uri,serial);
                }

                ReadablePipe rpipe = pipeline.readFrom(port);
                while (rpipe.moreDocuments()) {
                    wd.write(rpipe.read());
                }

                if (uri!=null) {
                   wd.close();
                }
            }

            // Here all memory should be freed by the next gc, right?
            pipeline = null;
            runtime.close();

            if (stdio != null) {
                // It's just sooo much nicer if there's a newline at the end.
                System.out.println();
            }
        } catch (XProcException err) {
            exitStatus = 1;
            if (err.getErrorCode() != null) {
                error(logger, null, errorMessage(err.getErrorCode()), err.getErrorCode());
            } else {
                error(logger, null, err.toString(), null);
            }

            Throwable cause = err.getCause();
            while (cause != null && cause instanceof XProcException) {
                cause = cause.getCause();
            }

            if (cause != null) {
                error(logger, null, "Underlying exception: " + cause, null);
            }

            if (debug) {
                err.printStackTrace();
            }
        } catch (Exception err) {
            exitStatus = 1;
            error(logger, null, "Pipeline failed: " + err.toString(), null);
            if (err.getCause() != null) {
                Throwable cause = err.getCause();
                error(logger, null, "Underlying exception: " + cause, null);
            }
            if (debug) {
                err.printStackTrace();
            }
        }
    }

    private void showVersion() {
        System.out.println("XML Calabash version " + XProcConstants.XPROC_VERSION + ", an XProc processor.");
        if (runtime != null) {
            System.out.print("Running on Saxon version ");
            System.out.print(runtime.getConfiguration().getProcessor().getSaxonProductVersion());
            System.out.print(", ");
            System.out.print(runtime.getConfiguration().getProcessor().getUnderlyingConfiguration().getEditionCode());
            System.out.println(" edition.");
        }
        System.out.println("Copyright (c) 2007-2012 Norman Walsh");
        System.out.println("See docs/notices/NOTICES in the distribution for licensing");
        System.out.println("See also http://xmlcalabash.com/ for more information");
        System.out.println("");
    }
    
    private void usage() throws IOException {
        showVersion();

        InputStream instream = getClass().getResourceAsStream("/etc/usage.txt");
        if (instream == null) {
            throw new UnsupportedOperationException("Failed to load usage text from JAR file. This \"can't happen\".");
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(instream));
        String line = null;
        while ((line = br.readLine()) != null) {
            System.err.println(line);
        }
        instream.close();
        br.close();
        System.exit(1);
    }

    private String errorMessage(QName code) {
        InputStream instream = getClass().getResourceAsStream("/etc/error-list.xml");
        if (instream != null) {
            XdmNode doc = runtime.parse(new InputSource(instream));
            XdmSequenceIterator iter = doc.axisIterator(Axis.DESCENDANT, new QName(XProcConstants.NS_XPROC_ERROR,"error"));
            while (iter.hasNext()) {
                XdmNode error = (XdmNode) iter.next();
                if (code.getLocalName().equals(error.getAttributeValue(_code))) {
                    return error.getStringValue();
                }
            }
        }
        return "Unknown error";
    }

    // ===========================================================
    // Logging methods repeated here so that they don't rely
    // on the XProcRuntime constructor succeeding.

    private String message(XdmNode node, String message) {
        String baseURI = "(unknown URI)";
        int lineNumber = -1;

        if (node != null) {
            baseURI = node.getBaseURI().toASCIIString();
            lineNumber = node.getLineNumber();
            return baseURI + ":" + lineNumber + ": " + message;
        } else {
            return message;
        }

    }

    public void error(Logger logger, XdmNode node, String message, QName code) {
        logger.severe(message(node, message));
    }

    public void warning(Logger logger, XdmNode node, String message) {
        logger.warning(message(node, message));
    }

    public void info(Logger logger, XdmNode node, String message) {
        logger.info(message(node, message));
    }

    public void fine(Logger logger, XdmNode node, String message) {
        logger.fine(message(node, message));
    }

    public void finer(Logger logger, XdmNode node, String message) {
        logger.finer(message(node, message));
    }

    public void finest(Logger logger, XdmNode node, String message) {
        logger.finest(message(node, message));
    }

    // ===========================================================

}
