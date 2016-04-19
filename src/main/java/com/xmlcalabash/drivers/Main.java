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
import com.xmlcalabash.util.Closer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private Logger logger = LoggerFactory.getLogger(Main.class);
    private boolean debug = false;
    private int chaseMemoryLeaks = 0;

    /*
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.run(args);
        if (exitStatus != 0) {
            System.exit(exitStatus);
        }
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
            runtime = new XProcRuntime(config);
            debug = config.debug;

            if (chaseMemoryLeaks != 0) {
                while (chaseMemoryLeaks > 0) {
                    System.err.println("Checking for memory leaks, running " + chaseMemoryLeaks);
                    run(userArgs, config);
                    //System.out.println("Hit enter to run again: ");
                    //System.in.read();
                    chaseMemoryLeaks--;
                }
            } else {
                if (run(userArgs, config)) {
                    // It's just sooo much nicer if there's a newline at the end.
                    System.out.println();
                }
            }

        } catch (UnsupportedOperationException uoe) {
            usage();
        } catch (XProcException err) {
            exitStatus = 1;
            if (err.getErrorCode() != null) {
                logger.error(errorMessage(err.getErrorCode()));
            } else {
                logger.error(err.getMessage());
            }

            Throwable cause = err.getCause();
            while (cause != null && cause instanceof XProcException) {
                cause = cause.getCause();
            }

            if (cause != null) {
                logger.error("Underlying exception: " + cause.getMessage());
            }

            logger.debug(err.getMessage(), err);
        } catch (Exception err) {
            exitStatus = 1;
            logger.error("Pipeline failed: " + err.getMessage());
            if (err.getCause() != null) {
                Throwable cause = err.getCause();
                logger.error("Underlying exception: " + cause.getMessage());
            }
            logger.debug(err.getMessage(), err);
        } finally {
            // Here all memory should be freed by the next gc, right?
            if (runtime != null) {
                runtime.close();
            }
        }
    }

    // This method runs the pipeline but doesn't catch any exceptions.
    // The idea is you could call this from some other object and catch (or not) the
    // exceptions yourself.
    public void runMethod(String[] args) throws IOException, SaxonApiException, URISyntaxException {
        UserArgs userArgs = new ParseArgs().parse(args);

        XProcConfiguration config = userArgs.createConfiguration();
        runtime = new XProcRuntime(config);
        debug = config.debug;

        try {
            run(userArgs, config);
        } finally {
            // Here all memory should be freed by the next gc, right?
            if (runtime != null) {
                runtime.close();
            }
        }
    }

    boolean run(UserArgs userArgs, XProcConfiguration config) throws SaxonApiException, IOException, URISyntaxException {
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

                Serializer serializer = runtime.getProcessor().newSerializer();

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
                                    try {
                                        doc = runtime.parse(new InputSource(inputStream));
                                    } finally {
                                        Closer.close(inputStream);
                                    }
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
                                    try {
                                        rd = new ReadableData(runtime, c_data, inputStream, input.getContentType());
                                        doc = rd.read();
                                    } finally {
                                        Closer.close(inputStream);
                                    }
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
                logger.trace("Copy output from " + port + " to stdout");
            } else {
                switch (output.getKind()) {
                    case URI:
                        logger.trace("Copy output from " + port + " to " + output.getUri());
                        break;

                    case OUTPUT_STREAM:
                        String outputStreamClassName = output.getOutputStream().getClass().getName();
                        logger.trace("Copy output from " + port + " to " + outputStreamClassName + " stream");
                        break;

                    default:
                        throw new UnsupportedOperationException(format("Unsupported output kind '%s'", output.getKind()));
                }
            }

            Serialization serial = pipeline.getSerialization(port);

            if (serial == null) {
                // Use the configuration options
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

            // Command line values override pipeline or configuration specified values
            for (String name: new String[] {
                    "byte-order-mark", "escape-uri-attributes", "include-content-type",
                    "indent", "omit-xml-declaration", "undeclare-prefixes", "method",
                    "doctype-public", "doctype-system", "encoding", "media-type",
                    "normalization-form", "standalone", "version" }) {
                String value = userArgs.getSerializationParameter(port, name);
                if (value == null) {
                    value = userArgs.getSerializationParameter(name);
                    if (value == null) {
                        continue;
                    }
                }

                if ("byte-order-mark".equals(name)) serial.setByteOrderMark("true".equals(value));
                if ("escape-uri-attributes".equals(name)) serial.setEscapeURIAttributes("true".equals(value));
                if ("include-content-type".equals(name)) serial.setIncludeContentType("true".equals(value));
                if ("indent".equals(name)) serial.setIndent("true".equals(value));
                if ("omit-xml-declaration".equals(name)) serial.setOmitXMLDeclaration("true".equals(value));
                if ("undeclare-prefixes".equals(name)) serial.setUndeclarePrefixes("true".equals(value));
                if ("method".equals(name)) serial.setMethod(new QName("", value));
                // N.B. cdata-section-elements isn't allowed
                if ("doctype-public".equals(name)) serial.setDoctypePublic(value);
                if ("doctype-system".equals(name)) serial.setDoctypeSystem(value);
                if ("encoding".equals(name)) serial.setEncoding(value);
                if ("media-type".equals(name)) serial.setMediaType(value);
                if ("normalization-form".equals(name)) serial.setNormalizationForm(value);
                if ("standalone".equals(name)) serial.setStandalone(value);
                if ("version".equals(name)) serial.setVersion(value);
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

            try {
                ReadablePipe rpipe = pipeline.readFrom(port);
                while (rpipe.moreDocuments()) {
                    wd.write(rpipe.read());
                }
            } finally {
                if (output != null) {
                    wd.close();
                }
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
        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                System.err.println(line);
            }
        } finally {
            // BufferedReader.close also closes the underlying stream, so only 
            // one close() call is necessary.
            // instream.close();
            br.close();
        }
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
}
