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
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
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
    }

    public void run(String[] args) throws SaxonApiException, IOException, URISyntaxException {
        ParseArgs cmd = new ParseArgs();
        try {
            cmd.parse(args);
        } catch (XProcException xe) {
            System.err.println(xe.getMessage());
            usage();
        }

        try {
            XProcConfiguration config = null;

            try {
                if (cmd.schemaAware) {
                    config = new XProcConfiguration(cmd.schemaAware);
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

            config.extensionValues = cmd.extensionValues;

            debug = config.debug;

            runtime = new XProcRuntime(config);

            XPipeline pipeline = null;

            if (cmd.pipelineURI != null) {
                pipeline = runtime.load(cmd.pipelineURI);
            } else if (cmd.impliedPipeline()) {
                XdmNode implicitPipeline = cmd.implicitPipeline(runtime);

                if (debug) {
                    System.err.println("Implicit pipeline:");
                    Processor qtproc = runtime.getProcessor();
                    DocumentBuilder builder = qtproc.newDocumentBuilder();
                    builder.setBaseURI(new URI("http://example.com/"));
                    XQueryCompiler xqcomp = qtproc.newXQueryCompiler();
                    XQueryExecutable xqexec = xqcomp.compile(".");
                    XQueryEvaluator xqeval = xqexec.load();
                    xqeval.setContextItem(implicitPipeline);

                    Serializer serializer = new Serializer();

                    serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
                    serializer.setOutputProperty(Serializer.Property.METHOD, "xml");

                    serializer.setOutputStream(System.err);

                    xqeval.setDestination(serializer);
                    xqeval.run();
                }

                pipeline=runtime.use(implicitPipeline);
                
/*
                pipeline = cmd.implicitPipeline();
            } else if (cmd.libraryURI != null) {
                XLibrary library = runtime.loadLibrary(cmd.libraryURI);
                if (cmd.stepName == null) {
                    pipeline = library.getFirstPipeline();
                } else {
                    pipeline = library.getPipeline(cmd.stepName);
                }
*/
            } else if (config.pipeline != null) {
                XdmNode doc = config.pipeline.read();
                pipeline = runtime.use(doc);
            }

            // Special case for running XProc standard library steps directly
            if (pipeline == null && cmd.stepName != null
                    && XProcConstants.NS_XPROC.equals(cmd.stepName.getNamespaceURI())) {
                pipeline = runtime.getStandardLibrary().getPipeline(cmd.stepName);
            }

            if (errors || pipeline == null) {
                usage();
            }

            // Process parameters from the configuration...
            for (String port : config.params.keySet()) {
                Hashtable<QName,String> hash = cmd.params.get(port);
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
            for (String port : cmd.params.keySet()) {
                Hashtable<QName,String> hash = cmd.params.get(port);
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

                            SAXSource source = null;
                            if ("-".equals(uri)) {
                                source = new SAXSource(new InputSource(System.in));
                                DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
                                doc = builder.build(source);
                            } else {
                                source = new SAXSource(new InputSource(uri));
                                doc = runtime.parse(uri, URIUtils.cwdAsURI().toASCIIString());
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

            for (QName optname : cmd.options.keySet()) {
                RuntimeValue value = new RuntimeValue(cmd.options.get(optname), null, null);
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
                WritableDocument wd = new WritableDocument(runtime,uri,serial);
                ReadablePipe rpipe = pipeline.readFrom(port);
                while (rpipe.moreDocuments()) {
                    wd.write(rpipe.read());
                }
            }
        } catch (XProcException err) {
            if (err.getErrorCode() != null) {
                String message = "Pipeline failed: err:" + err.getErrorCode() + ": " + err.getMessage();
                if (err.getStep() != null) {
                    message += "  at " + err.getStep();
                }
                message += "  " + errorMessage(err.getErrorCode());
                error(logger, null, message, err.getErrorCode());
                if (err.getCause() != null) {
                    Throwable cause = err.getCause();
                    error(logger, null, "Underlying exception: " + cause, null);
                }
                if (debug) {
                    err.printStackTrace();
                }
            } else {
                error(logger, null, "Pipeline failed: " + err.toString(), null);
                if (err.getCause() != null) {
                    Throwable cause = err.getCause();
                    error(logger, null, "Underlying exception: " + cause, null);
                }
                if (debug) {
                    err.printStackTrace();
                }
            }
            /*
            if (debug) {
                err.printStackTrace();
            }
            */
        } catch (Exception err) {
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

    private void usage() throws IOException {
        System.out.println("Calabash version " + XProcConstants.XPROC_VERSION + ", an XProc processor");
        System.out.println("Copyright (c) 2007-2009 Norman Walsh");
        System.out.println("See http://xmlcalabash.com/");
        System.out.println("");

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
            try {
                SAXSource source = new SAXSource(new InputSource(instream));
                DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
                XdmNode doc = builder.build(source);
                XdmSequenceIterator iter = doc.axisIterator(Axis.DESCENDANT, new QName(XProcConstants.NS_XPROC_ERROR,"error"));
                while (iter.hasNext()) {
                    XdmNode error = (XdmNode) iter.next();
                    if (code.getLocalName().equals(error.getAttributeValue(_code))) {
                        return error.getStringValue();
                    }
                }
            } catch (SaxonApiException sae) {
                // nop;
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
