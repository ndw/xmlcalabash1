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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.Input;
import com.xmlcalabash.util.Output;
import com.xmlcalabash.util.Output.Kind;
import com.xmlcalabash.util.ParseArgs;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.UserArgs;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.xml.sax.InputSource;

import static com.xmlcalabash.core.XProcConstants.c_data;
import static com.xmlcalabash.util.Output.Kind.OUTPUT_STREAM;
import static java.lang.String.format;

/**
 *
 * @author ndw
 */
public class Main {
    private static QName _code = new QName("code");
    private static int exitStatus = 0;
    private XProcRuntime runtime = null;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private boolean debug = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.run(args);
        System.exit(exitStatus);
    }

    public void run(String[] args) throws IOException {
        UserArgs userArgs = null;
        try {
            userArgs = new ParseArgs().parse(args);
        } catch (XProcException xe) {
            System.err.println(xe.getMessage());
            usage();
        }

        try {
            XProcConfiguration config = userArgs.createConfiguration();

            if (run(userArgs, config)) {
                // It's just sooo much nicer if there's a newline at the end.
                System.out.println();
            }

            // Here all memory should be freed by the next gc, right?
            runtime.close();
        } catch (UnsupportedOperationException uoe) {
            usage();
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

    boolean run(UserArgs userArgs, XProcConfiguration config) throws SaxonApiException, IOException, URISyntaxException {
        runtime = new XProcRuntime(config);
        debug = config.debug;

        if (userArgs.isShowVersion()) {
            XProcConfiguration.showVersion(runtime);
        }

        XPipeline pipeline = null;

        if (userArgs.getPipeline() != null) {
            pipeline = runtime.load(userArgs.getPipeline());
        } else if (userArgs.hasImplicitPipeline()) {
            XdmNode implicitPipeline = userArgs.getImplicitPipeline(runtime);

            if (debug) {
                System.err.println("Implicit pipeline:");

                Serializer serializer = new Serializer();

                serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
                serializer.setOutputProperty(Serializer.Property.METHOD, "xml");

                serializer.setOutputStream(System.err);

                S9apiUtils.serialize(runtime, implicitPipeline, serializer);
            }

            pipeline = runtime.use(implicitPipeline);
        } else if (config.pipeline != null) {
            XdmNode doc = config.pipeline.read();
            pipeline = runtime.use(doc);
        } else {
            throw new UnsupportedOperationException("Either a pipeline or libraries and / or steps must be given");
        }

        // Process parameters from the configuration...
        for (String port : config.params.keySet()) {
            Map<QName, String> parameters = config.params.get(port);
            setParametersOnPipeline(pipeline, port, parameters);
        }

        // Now process parameters from the command line...
        for (String port : userArgs.getParameterPorts()) {
            Map<QName, String> parameters = userArgs.getParameters(port);
            setParametersOnPipeline(pipeline, port, parameters);
        }

        Set<String> ports = pipeline.getInputs();
        Set<String> userArgsInputPorts = userArgs.getInputPorts();
        Set<String> cfgInputPorts = config.inputs.keySet();
        Set<String> allPorts = new HashSet<String>();
        allPorts.addAll(userArgsInputPorts);
        allPorts.addAll(cfgInputPorts);

        // map a given input without port specification to the primary non-parameter input implicitly
        for (String port : ports) {
            if (!allPorts.contains(port) && allPorts.contains(null)
                && pipeline.getDeclareStep().getInput(port).getPrimary()
                && !pipeline.getDeclareStep().getInput(port).getParameterInput()) {

                if (userArgsInputPorts.contains(null)) {
                    userArgs.setDefaultInputPort(port);
                    allPorts.remove(null);
                    allPorts.add(port);
                }
                break;
            }
        }

        for (String port : allPorts) {
            if (!ports.contains(port)) {
                throw new XProcException("There is a binding for the port '" + port + "' but the pipeline declares no such port.");
            }

            pipeline.clearInputs(port);

            if (userArgsInputPorts.contains(port)) {
                XdmNode doc = null;
                for (Input input : userArgs.getInputs(port)) {
                    switch (input.getType()) {
                        case XML:
                            switch (input.getKind()) {
                                case URI:
                                    String uri = input.getUri();
                                    if ("-".equals(uri)) {
                                        doc = runtime.parse(new InputSource(System.in));
                                    } else {
                                        doc = runtime.parse(new InputSource(uri));
                                    }
                                    break;

                                case INPUT_STREAM:
                                    InputStream inputStream = input.getInputStream();
                                    doc = runtime.parse(new InputSource(inputStream));
                                    inputStream.close();
                                    break;

                                default:
                                    throw new UnsupportedOperationException(format("Unsupported input kind '%s'", input.getKind()));
                            }
                            break;

                        case DATA:
                            ReadableData rd;
                            switch (input.getKind()) {
                                case URI:
                                    rd = new ReadableData(runtime, c_data, input.getUri(), input.getContentType());
                                    doc = rd.read();
                                    break;

                                case INPUT_STREAM:
                                    InputStream inputStream = input.getInputStream();
                                    rd = new ReadableData(runtime, c_data, inputStream, input.getContentType());
                                    doc = rd.read();
                                    inputStream.close();
                                    break;

                                default:
                                    throw new UnsupportedOperationException(format("Unsupported input kind '%s'", input.getKind()));
                            }
                            break;

                        default:
                            throw new UnsupportedOperationException(format("Unsupported input type '%s'", input.getType()));
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

        // Implicit binding for stdin?
        String implicitPort = null;
        for (String port : ports) {
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

        Map<String, Output> portOutputs = new HashMap<String, Output>();

        Map<String, Output> userArgsOutputs = userArgs.getOutputs();
        for (String port : pipeline.getOutputs()) {
            // Bind to "-" implicitly
            Output output = null;

            if (userArgsOutputs.containsKey(port)) {
                output = userArgsOutputs.get(port);
            } else if (config.outputs.containsKey(port)) {
                output = new Output(config.outputs.get(port));
            } else if (userArgsOutputs.containsKey(null)
                       && pipeline.getDeclareStep().getOutput(port).getPrimary()) {
                // Bind unnamed port to primary output port
                output = userArgsOutputs.get(null);
            }

            // Look for explicit binding to "-"
            if ((output != null) && (output.getKind() == Kind.URI) && "-".equals(output.getUri())) {
                output = null;
            }

            portOutputs.put(port, output);
        }

        for (QName optname : config.options.keySet()) {
            RuntimeValue value = new RuntimeValue(config.options.get(optname), null, null);
            pipeline.passOption(optname, value);
        }

        for (QName optname : userArgs.getOptionNames()) {
            RuntimeValue value = new RuntimeValue(userArgs.getOption(optname), null, null);
            pipeline.passOption(optname, value);
        }

        pipeline.run();

        for (String port : pipeline.getOutputs()) {
            Output output;
            if (portOutputs.containsKey(port)) {
                output = portOutputs.get(port);
            } else {
                // You didn't bind it, and it isn't going to stdout, so it's going into the bit bucket.
                continue;
            }

            if ((output == null) || ((output.getKind() == OUTPUT_STREAM) && System.out.equals(output.getOutputStream()))) {
                finest(logger, null, "Copy output from " + port + " to stdout");
            } else {
                switch (output.getKind()) {
                    case URI:
                        finest(logger, null, "Copy output from " + port + " to " + output.getUri());
                        break;

                    case OUTPUT_STREAM:
                        String outputStreamClassName = output.getOutputStream().getClass().getName();
                        finest(logger, null, "Copy output from " + port + " to " + outputStreamClassName + " stream");
                        break;

                    default:
                        throw new UnsupportedOperationException(format("Unsupported output kind '%s'", output.getKind()));
                }
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
            if (output == null) {
                wd = new WritableDocument(runtime, null, serial);
            } else {
                switch (output.getKind()) {
                    case URI:
                        URI furi = new URI(output.getUri());
                        String filename = furi.getPath();
                        FileOutputStream outfile = new FileOutputStream(filename);
                        wd = new WritableDocument(runtime, filename, serial, outfile);
                        break;

                    case OUTPUT_STREAM:
                        OutputStream outputStream = output.getOutputStream();
                        wd = new WritableDocument(runtime, null, serial, outputStream);
                        break;

                    default:
                        throw new UnsupportedOperationException(format("Unsupported output kind '%s'", output.getKind()));
                }
            }

            ReadablePipe rpipe = pipeline.readFrom(port);
            while (rpipe.moreDocuments()) {
                wd.write(rpipe.read());
            }

            if (output != null) {
                wd.close();
            }
        }

        return portOutputs.containsValue(null);
    }

    private void setParametersOnPipeline(XPipeline pipeline, String port, Map<QName, String> parameters) {
        if ("*".equals(port)) {
            for (QName name : parameters.keySet()) {
                pipeline.setParameter(name, new RuntimeValue(parameters.get(name)));
            }
        } else {
            for (QName name : parameters.keySet()) {
                pipeline.setParameter(port, name, new RuntimeValue(parameters.get(name)));
            }
        }
    }

    private void usage() throws IOException {
        System.out.println();
        XProcConfiguration.showVersion(runtime);

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
