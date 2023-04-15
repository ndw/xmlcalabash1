/*
 * RunTests.java
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

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.*;
import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


/**
 *
 * @author ndw
 */
public class RunTestReport {
    public static final QName _path = new QName("path");
    public static final QName _port = new QName("port");
    public static final QName _href = new QName("href");
    public static final QName _name = new QName("name");
    public static final QName _value = new QName("value");

    public static final String NS_TEST = "http://xproc.org/ns/testsuite";
    public static final QName t_test = new QName(NS_TEST, "test");
    public static final QName t_title = new QName(NS_TEST, "title");
    public static final QName t_description = new QName(NS_TEST, "description");
    public static final QName t_input = new QName(NS_TEST, "input");
    public static final QName t_output = new QName(NS_TEST, "output");
    public static final QName t_parameter = new QName(NS_TEST, "parameter");
    public static final QName t_option = new QName(NS_TEST, "option");
    public static final QName t_pipeline = new QName(NS_TEST, "pipeline");
    public static final QName t_compare_pipeline = new QName(NS_TEST, "compare-pipeline");
    public static final QName t_document = new QName(NS_TEST, "document");

    private static boolean debug = false;
    private static boolean schemaAware = false;
    private static XdmNode prettyPrint = null;
    private static String defaultLog = null;

    private XProcRuntime runtime = null;
    private TestReporter reporter = null;
    protected Logger logger = LoggerFactory.getLogger(RunTestReport.class);


    /* Creates a new instance of RunTest */
    public RunTestReport(XProcRuntime runtime, TestReporter reporter) {
        this.runtime = runtime;
        this.reporter = reporter;
    }

    public void setDebug(boolean dbg) {
        debug = dbg;
    }

    public void setSchemaAware(boolean aware) {
        schemaAware = aware;
    }

    public void setDefaultLog(String deflog) {
        defaultLog = deflog;
    }

    public static void main(String[] args) throws SaxonApiException, IOException, URISyntaxException {
        String usage = "RunTests [-D] [-d directory] [-a] test.xml";
        Vector<String> tests = new Vector<String> ();

        for (int pos = 0; pos < args.length; pos++) {
            if ("-D".equals(args[pos])) {
                debug = true;
            } else if ("-L".equals(args[pos])) {
                defaultLog = args[pos+1];
                pos++;
            } else if ("-a".equals(args[pos])) {
                schemaAware = true;
            } else if ("-d".equals(args[pos])) {
                int count = 0;
                File dir = new File(args[++pos]);
                String dirname = null;

                try {
                    dir = dir.getCanonicalFile();
                    dirname = dir.getName();
                } catch (IOException ioe) {
                    throw new XProcException(ioe);
                }

                for (File file : dir.listFiles()) {
                    if (!file.isDirectory() && file.getName().endsWith(".xml")) {
                        count++;
                        System.err.println("Test: " + file.getAbsolutePath());
                        tests.add(file.getAbsolutePath());
                    }
                }

                if (count == 0) {
                    System.err.println("No tests found in " + dirname);
                }
            } else {
                System.err.println("Test: " + args[pos]);
                tests.add(args[pos]);
            }
        }

        if (tests.size() == 0) {
            System.err.println(usage);
            System.exit(1);
        }

        XProcConfiguration config = new XProcConfiguration("ee", schemaAware);
        XProcRuntime runtime = new XProcRuntime(config);

        RunTestReport test = new RunTestReport(runtime, new DefaultTestReporter(runtime));
        test.runTests(tests);
        runtime.close();
    }

    public void runTests(Vector<String> tests) {
        startReport();

        for (String testfile : tests) {
            reportRun(testfile);
        }

        endReport();
    }

    public TestSuiteResults run(String testfile) {
        Vector<TestSuiteResult> results = new Vector<TestSuiteResult> ();

        /*
        XProcConfiguration config = new XProcConfiguration("ee", schemaAware);
        runtime = new XProcRuntime(config);
        runtime.getConfiguration().debug = debug;
        */

        XdmNode doc, root;
        try {
            // Logically, we shouldn't have to do this, but ... we do so that prettyPrint
            // is always made with the same processor as all the other nodes
            InputStream instream = getClass().getResourceAsStream("/etc/prettyprint.xpl");
            if (instream == null) {
                throw new UnsupportedOperationException("Failed to load prettyprint stylesheet from resources.");
            }
            XdmNode ppd = runtime.parse(new InputSource(instream));
            prettyPrint = S9apiUtils.getDocumentElement(ppd);

            InputSource isource = new InputSource(testfile);
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setEntityResolver(runtime.getResolver());
            SAXSource source = new SAXSource(reader,isource);
            DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
            builder.setLineNumbering(true);
            builder.setDTDValidation(false);

            doc = builder.build(source);
            root = S9apiUtils.getDocumentElement(doc);
            assert root != null;
        } catch (Exception sae) {
            TestSuiteResult result = new TestSuiteResult(testfile);
            result.catchException(sae);
            results.add(result);
            return new TestSuiteResults(results);
        }

        if (t_test.equals(root.getNodeName())) {
            TestSuiteResult result = runTest(root);
            results.add(result);
            return new TestSuiteResults(results);
        } else {
            StringBuilder title = new StringBuilder();
            XdmSequenceIterator<XdmNode> iter = root.axisIterator(Axis.CHILD, t_title);
            while (iter.hasNext()) {
                XdmNode test = iter.next();
                title.append(test.getStringValue());
            }

            iter = root.axisIterator(Axis.CHILD, t_test);
            while (iter.hasNext()) {
                XdmNode test = iter.next();
                TestSuiteResult result = runTest(test);
                results.add(result);
            }

            if (!"".equals(title.toString())) {
                return new TestSuiteResults(title.toString(), results);
            } else {
                return new TestSuiteResults(results);
            }
        }
    }

    public void reportRun(String testfile) {
        TestSuiteResults results = run(testfile);
        results.reportResults();
    }

    public TestSuiteResult runTest(XdmNode testNode) {
        TestSuiteResult result;

        if (testNode.getAttributeValue(_href) != null) {
            URI turi = testNode.getBaseURI().resolve(testNode.getAttributeValue(_href));
            try {
                InputSource isource = new InputSource(turi.toASCIIString());
                XMLReader reader = XMLReaderFactory.createXMLReader();
                reader.setEntityResolver(runtime.getResolver());
                SAXSource source = new SAXSource(reader, isource);
                DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
                builder.setLineNumbering(true);
                builder.setDTDValidation(false);
                XdmNode doc = builder.build(source);
                XdmNode root = S9apiUtils.getDocumentElement(doc);
                result = runTest(root);
            } catch (Exception sae) {
                result = new TestSuiteResult(turi.toASCIIString());
                result.catchException(sae);
            }

            return result;
        } else {
            result = new TestSuiteResult(testNode.getBaseURI().toASCIIString());
        }

        reporter.runningTest(testNode.getBaseURI());

        XProcTest t = new XProcTest(testNode);

        result.setTitle(t.title);
        result.setDescription(t.description);

        if (t.pipeline == null) {
            result.catchException(new UnsupportedOperationException("Pipeline must be provided."));
            return result;
        }

        HashMap<String, ReadablePipe> pipeoutputs = new HashMap<String, ReadablePipe>();
        try {
            pipeoutputs = runPipe(t.pipeline.pipeline, t.inputs, t.outputs, t.parameters, t.options);
        } catch (XProcException xprocex) {
            if (t.error == null) {
                result.fail(xprocex);
                return result;
            } else if (xprocex.getErrorCode() != null) {
                result.catchException(xprocex);
                result.success(t.error, xprocex.getErrorCode());
                return result;
            }
            result.success(t.error, null);
            return result;
        } catch (Exception err) {
            result.catchException(err);
            return result;
        }

        if (t.error != null) {
            result.fail(t.error);
            return result;
        }

        if (t.comparepipeline != null) {
            XProcPipeline compare = t.comparepipeline;
            HashMap<String, Vector<XdmNode>> cinputs = new HashMap<String, Vector<XdmNode>> ();
            for (String port : pipeoutputs.keySet()) {
                if (compare.inputPorts.contains(port)) {
                    ReadablePipe pipe = pipeoutputs.get(port);
                    while (pipe.moreDocuments()) {
                        XdmNode p = pipe.read();
                        if (!cinputs.containsKey(port)) {
                            cinputs.put(port, new Vector<>());
                        }
                        cinputs.get(port).add(p);
                    }
                }
            }

            try {
                pipeoutputs = runPipe(compare.pipeline, cinputs, t.outputs, null, null);
            } catch (Exception err) {
                result.fail(err, "Compare pipelines failed: this shouldn't happen.");
            }
        }

        HashMap<String,Vector<XdmNode>> results = new HashMap<String,Vector<XdmNode>> ();
        HashMap<String,Vector<XdmNode>> expects = new HashMap<String,Vector<XdmNode>> ();

        try {
            for (String port : pipeoutputs.keySet()) {
                Vector<XdmNode> touts = t.outputs.get(port);
                ReadablePipe pipe = pipeoutputs.get(port);

                if (!results.containsKey(port)) {
                    results.put(port,new Vector<XdmNode>());
                }

                if (!expects.containsKey(port)) {
                    expects.put(port,new Vector<XdmNode>());
                }

                Vector<XdmNode> pres = results.get(port);
                Vector<XdmNode> pexp = expects.get(port);

                while (pipe.moreDocuments()) {
                    if (touts.size() > 0) {
                        XdmNode tdoc = touts.remove(0);
                        XdmNode pdoc = pipe.read();

                        if (t.ignoreWS) {
                            XPipeline pppipe = runtime.use(prettyPrint);
                            pppipe.writeTo("source", tdoc);
                            pppipe.run();
                            ReadablePipe rpipe = pppipe.readFrom("result");
                            tdoc = rpipe.read();

                            pppipe.reset();
                            pppipe.writeTo("source", pdoc);
                            pppipe.run();
                            rpipe = pppipe.readFrom("result");
                            pdoc = rpipe.read();
                        }

                        pres.add(pdoc);
                        pexp.add(tdoc);
                    } else {
                        XdmNode pdoc = pipe.read();
                        pres.add(pdoc);
                    }
                }
            }

            // Now lets see if we got the right results, 'k?
            QName doca = new QName("","doca");
            QName docb = new QName("","docb");

            for (String port : pipeoutputs.keySet()) {
                Vector<XdmNode> pres = results.get(port);
                Vector<XdmNode> pexp = expects.get(port);

                // FIXME: what about the case where they're different lengths?

                if (pres.size() == 0 && pexp.size() == 0) {
                    result.success();
                }

                for (int pos = 0; pos < pres.size() || pos < pexp.size(); pos++) {
                    if (pos >= pres.size()) {
                        XdmNode tdoc = pexp.get(pos);
                        result.fail(tdoc, null);
                    } else if (pos >= pexp.size()) {
                        XdmNode pdoc = pres.get(pos);
                        result.fail(null, pdoc);
                    } else {
                        XdmNode tdoc = pexp.get(pos);
                        XdmNode pdoc = pres.get(pos);

                        XPathCompiler xcomp = runtime.newXPathCompiler(testNode.getBaseURI());
                        xcomp.declareVariable(doca);
                        xcomp.declareVariable(docb);

                        XPathExecutable xexec = xcomp.compile("deep-equal($doca,$docb)");
                        XPathSelector selector = xexec.load();

                        selector.setVariable(doca,tdoc);
                        selector.setVariable(docb,pdoc);

                        Iterator<XdmItem> values = selector.iterator();
                        XdmAtomicValue item = (XdmAtomicValue) values.next();
                        boolean same = item.getBooleanValue();
                        if (same) {
                            result.success();
                        } else {
                            result.fail(tdoc, pdoc);
                        }
                    }
                }
            }
        } catch (SaxonApiException sae) {
            result.fail(sae, "Error comparing results: this shouldn't happen");
        }

        if (pipeoutputs.size() == 0) {
            if (expects.size() == 0) {
                result.success();
            }
        }

        return result;
    }

    private HashMap<String,ReadablePipe> runPipe(XdmNode pipeline,
                                                   HashMap<String, Vector<XdmNode>> inputs,
                                                   HashMap<String, Vector<XdmNode>> outputs,
                                                   HashMap<QName, String> parameters,
                                                   HashMap<QName, String> options) throws SaxonApiException {

        XPipeline xpipeline = runtime.use(pipeline);

        if (inputs != null) {
            for (String port : inputs.keySet()) {
                if (!xpipeline.getInputs().contains(port)) {
                    throw new UnsupportedOperationException("Error: Test sets input port that doesn't exist: " + port);
                }
                xpipeline.clearInputs(port);
                for (XdmNode node : inputs.get(port)) {
                    xpipeline.writeTo(port, node);
                }
            }
        }

        if (parameters != null) {
            for (QName name : parameters.keySet()) {
                xpipeline.setParameter(name, new RuntimeValue(parameters.get(name)));
            }
        }

        if (options != null) {
            for (QName name : options.keySet()) {

                // HACK HACK HACK!
                RuntimeValue v;
                if (_path.equals(name)) {
                    v = new RuntimeValue("file:///home/www/tests.xproc.org/tests/required/" + options.get(name));
                } else {
                    v = new RuntimeValue(options.get(name));
                }

                xpipeline.passOption(name, v);
            }
        }

        try {
            xpipeline.run();
        } catch (XProcException e) {
            System.err.println(e);
            logger.debug(e.getMessage(), e);
            throw e;
        } catch (Throwable e) {
            System.err.println(e);
            logger.debug(e.getMessage(), e);
            throw new XProcException(e);
        }

        HashMap<String, ReadablePipe> pipeoutputs = new HashMap<String, ReadablePipe> ();

        Set<String> pipeouts = xpipeline.getOutputs();
        for (String port : pipeouts) {
            if (!port.startsWith("!")) {
                ReadablePipe rpipe = xpipeline.readFrom(port);
                rpipe.canReadSequence(true);
                pipeoutputs.put(port, rpipe);
            }
        }

        return pipeoutputs;
    }

    private void makeReport(Vector<TestSuiteResult> results) {
        for (TestSuiteResult result : results) {
            result.report();
        }
    }

    private void startReport() {
        HashMap<String,String> props = new HashMap<String,String> ();
        props.put("name", runtime.getProductName());
        props.put("vendor", runtime.getVendor());
        props.put("vendor-uri", runtime.getVendorURI());
        props.put("version", runtime.getProductVersion());
        props.put("language", runtime.getLanguage());
        props.put("xproc-version", runtime.getXProcVersion());
        props.put("xpath-version", runtime.getXPathVersion());
        props.put("psvi-supported", ""+runtime.getPSVISupported());

        reporter.startReport(props);

    }

    private void endReport() {
        reporter.endReport();
    }

    public String serializeAsXML(XdmNode node) {
        try {
            Serializer serializer = runtime.getProcessor().newSerializer();

            serializer.setOutputProperty(Serializer.Property.BYTE_ORDER_MARK, "no");
            serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8");
            serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
            serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
            serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            serializer.setOutputStream(os);

            S9apiUtils.serialize(runtime, node, serializer);
            String result = os.toString();

            return result;
        } catch (SaxonApiException sae) {
            logger.warn("Failed to serialize node: " + node);
            logger.debug(sae.getMessage(), sae);
            return "";
        }
    }

    public String serialize(XdmNode node) {
        String result = serializeAsXML(node);

        result = result.replace("&","&amp;");
        result = result.replace("<","&lt;");
        result = result.replace(">","&gt;");
        return result;
    }

    private class XProcTest {
        private final QName _error = new QName("error");
        private final QName _ignoreWS = new QName("ignore-whitespace-differences");
        public HashMap<String, Vector<XdmNode>> inputs = new HashMap<String, Vector<XdmNode>>();
        public HashMap<String, Vector<XdmNode>> outputs = new HashMap<String, Vector<XdmNode>>();
        public HashMap<QName, String> parameters = new HashMap<QName, String>();
        public HashMap<QName, String> options = new HashMap<QName, String>();
        public XProcPipeline pipeline = null;
        public XProcPipeline comparepipeline = null;
        public XdmNode title = null;
        public XdmNode description = null;
        public QName error = null;
        public boolean ignoreWS = false;

        public XProcTest(XdmNode root) {
            if (!t_test.equals(root.getNodeName())) {
                throw new XProcException(root, "Test must have t:test as root element.");
            }

            if (root.getAttributeValue(_error) != null) {
                String errString = root.getAttributeValue(_error);
                error = new QName(errString, root);
            }

            ignoreWS = true;
            if (root.getAttributeValue(_ignoreWS) != null) {
                String ignore = root.getAttributeValue(_ignoreWS);
                ignoreWS = !"false".equals(ignore);
            }

            try {
                scan(root);
            } catch (Exception e) {
                throw new XProcException(e);
            }
        }

        private void scan(XdmNode pipeline) throws SaxonApiException {
            for (XdmNode node : new AxisNodes(runtime, pipeline,Axis.CHILD, AxisNodes.PIPELINE)) {
                if (t_title.equals(node.getNodeName())) {
                    title = node;
                    continue;
                }

                if (t_description.equals(node.getNodeName())) {
                    description = node;
                    continue;
                }

                if (t_input.equals(node.getNodeName()) || t_output.equals(node.getNodeName())) {
                    scanio(node);
                    continue;
                }

                if (t_parameter.equals(node.getNodeName()) || t_option.equals(node.getNodeName())) {
                    scanop(node);
                    continue;
                }

                if (t_pipeline.equals(node.getNodeName()) || t_compare_pipeline.equals(node.getNodeName())) {
                    scanpipe(node);
                    continue;
                }

                throw new XProcException(pipeline, "Not a valid test: " + node.getNodeName());
            }
        }

        private void scanio(XdmNode input) throws SaxonApiException {
            String port = input.getAttributeValue(_port);

            if (port == null) {
                throw new IllegalArgumentException("Each input and output must specify a port");
            }

            if (t_output.equals(input.getNodeName()) && outputs.containsKey(port)) {
                throw new IllegalArgumentException("Attempt to redefine output port: " + port);
            }

            String href = input.getAttributeValue(_href);
            if (href != null) {
                add(input, port, href);
            } else {
                for (XdmNode node : new AxisNodes(input, Axis.CHILD, AxisNodes.ALL)) {
                    if (node.getNodeKind() != XdmNodeKind.ELEMENT) {
                        if (node.getNodeKind() == XdmNodeKind.TEXT) {
                            if ("".equals(node.getStringValue().trim())) {
                                continue;
                            } else {
                                throw new IllegalArgumentException("Content of t:input/t:output must be well-formed XML");
                            }
                        } else {
                            continue;
                        }
                    }

                    if (t_document.equals(node.getNodeName())) {
                        href = node.getAttributeValue(_href);
                        if (href != null) {
                            add(input, port, href);
                        } else {
                            // Make sure that we have a document
                            Vector<XdmValue> nodes = new Vector<XdmValue>();
                            XdmSequenceIterator iter = node.axisIterator(Axis.CHILD);
                            while (iter.hasNext()) {
                                nodes.add(iter.next());
                            }
                            XdmDestination dest = new XdmDestination();
                            S9apiUtils.writeXdmValue(runtime, nodes, dest, node.getBaseURI());

                            add(input, port, RebasedDocument.makeUniqueDocumentId(dest.getXdmNode()));
                        }
                    } else {
                        // Make sure that we have a document
                        XdmDestination dest = new XdmDestination();
                        dest.setBaseURI(node.getBaseURI());
                        S9apiUtils.writeXdmValue(runtime, node, dest, node.getBaseURI());
                        XdmNode newNode = dest.getXdmNode();
                        add(input, port, newNode);
                    }
                }
            }
        }

        private void scanpipe(XdmNode input) throws SaxonApiException {
            URI baseURI = input.getBaseURI();
            String href = input.getAttributeValue(_href);

            if (href != null) {
                add(input, null, baseURI.resolve(href).toASCIIString());
            } else {
                XdmNode docroot = null;
                for (XdmNode node : new AxisNodes(runtime, input, Axis.CHILD, AxisNodes.PIPELINE)) {
                    if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                        docroot = node;
                    }
                }

                if (t_document.equals(docroot.getNodeName())) {
                    href = docroot.getAttributeValue(_href);
                    if (href != null) {
                        add(input, null, docroot.getBaseURI().resolve(href).toASCIIString());
                    } else {
                        XdmNode root = S9apiUtils.getDocumentElement(docroot);
                        add(input, null, root);
                    }
                } else {
                    add(input, null, docroot);
                }
            }
        }

        private void scanop(XdmNode input) {
            String namestr = input.getAttributeValue(_name);
            String value = input.getAttributeValue(_value);

            if (namestr == null || value == null) {
                throw new IllegalArgumentException("Each option and parameter must specify a name and a value");
            }

            QName name = new QName(namestr, input);

            if (t_option.equals(input.getNodeName())) {
                if (options.containsKey(name)) {
                    throw new IllegalArgumentException("Attempt to redefine option: " + name);
                } else {
                    options.put(name, value);
                }
            } else {
                if (parameters.containsKey(name)) {
                    throw new IllegalArgumentException("Attempt to redefine parameter: " + name);
                } else {
                    parameters.put(name, value);
                }
            }

            for (XdmNode node : new AxisNodes(runtime, input, Axis.CHILD, AxisNodes.PIPELINE)) {
                throw new IllegalArgumentException("Options and parameters must be empty.");
            }
        }

        private void add(XdmNode node, String port, String href) throws SaxonApiException {
            String rhref = node.getBaseURI().resolve(href).toASCIIString();
            add(node, port, runtime.parse(new InputSource(rhref)));
        }

        private void add(XdmNode node, String port, XdmNode root) {
            String type = node.getNodeName().getLocalName();
            if ("input".equals(type)) {
                if (!inputs.containsKey(port)) {
                    inputs.put(port, new Vector<XdmNode> ());
                }

                inputs.get(port).add(root);
             } else if ("output".equals(type)) {
                if (!outputs.containsKey(port)) {
                    outputs.put(port, new Vector<XdmNode> ());
                }

                outputs.get(port).add(root);
            } else if ("pipeline".equals(type)) {
                if (pipeline != null) {
                    throw new UnsupportedOperationException("Only one pipeline can be defined.");
                }

                pipeline = new XProcPipeline(root);
            } else if ("compare-pipeline".equals(type)) {
                if (comparepipeline != null) {
                    throw new UnsupportedOperationException("Only one compare pipeline can be defined.");
                }

                comparepipeline = new XProcPipeline(root);
            } else {
                throw new UnsupportedOperationException("Unexpected type: " + type);
            }
        }
    }

    private class XProcPipeline {
        public HashSet<String> inputPorts = new HashSet<String> ();
        public HashSet<String> outputPorts = new HashSet<String> ();
        public XdmNode pipeline = null;

        public XProcPipeline (XdmNode root) {
            pipeline = root;

            for (XdmNode node : new AxisNodes(runtime, root, Axis.CHILD, AxisNodes.PIPELINE)) {
                if (XProcConstants.p_input.equals(node.getNodeName())) {
                    inputPorts.add(node.getAttributeValue(_port));
                }
                if (XProcConstants.p_output.equals(node.getNodeName())) {
                    outputPorts.add(node.getAttributeValue(_port));
                }
            }
        }
    }

    public class TestSuiteResults {
        String title = null;
        Vector<TestSuiteResult> results = null;

        public TestSuiteResults(String title, Vector<TestSuiteResult> results) {
            this.title = title;
            this.results = results;
        }

        public TestSuiteResults(Vector<TestSuiteResult> results) {
            this.results = results;
        }

        public String getTitle() {
            return title;
        }

        public Vector<TestSuiteResult> getResults() {
            return results;
        }

        public void reportResults() {
            if (title == null) {
                makeReport(results);
            } else {
                reporter.startTestSuite();
                makeReport(results);
                reporter.endTestSuite();
            }
        }
    }

    public class TestSuiteResult {
        public String testfile = null;
        public String title = "";
        public Vector<XdmNode> description = null;
        public boolean passed = false;
        public QName expectedError = null;
        public QName actualError = null;
        public Vector<String> errorMessages = new Vector<String> ();
        public XdmNode expected = null;
        public XdmNode actual = null;

        public TestSuiteResult(String testfile) {
            this.testfile = testfile;
        }

        public void setTitle(XdmNode title) {
            if (title != null) {
                this.title = title.getStringValue();
            }
        }

        public void setDescription(XdmNode desc) {
            if (desc != null) {
                XdmSequenceIterator<XdmNode> iter = desc.axisIterator(Axis.CHILD);
                description = new Vector<XdmNode> ();
                while (iter.hasNext()) {
                    XdmNode node = iter.next();
                    HashSet<NamespaceUri> prefixes = new HashSet<> ();
                    XdmSequenceIterator<XdmNode> nsiter = node.axisIterator(Axis.NAMESPACE);
                    while (nsiter.hasNext()){
                        XdmNode nsnode = nsiter.next();
                        if (nsnode.getNodeName() != null) {
                            String prefix = nsnode.getNodeName().getLocalName();
                            String uri = nsnode.getStringValue();
                            if (!"http://www.w3.org/1999/xhtml".equals(uri)
                                && !"xml".equals(prefix)) {
                                prefixes.add(NamespaceUri.of(uri));
                            }
                        }
                    }
                    node = S9apiUtils.removeNamespaces(runtime, node, prefixes, true);
                    description.add(node);
                }
            }
        }

        public boolean passed() {
            return passed;
        }

        public void success() {
            passed = true;
        }

        public void success(QName expected, QName actual) {
            expectedError = expected;
            actualError = actual;
            passed = true;
        }

        public void fail(QName expectedError) {
            errorMessages.add("Test passed, but should have raised " + expectedError);
        }

        public void fail(Exception e) {
            catchException(e);
        }

        public void fail(Exception e, String message) {
            catchException(e);
            if (message != null) {
                errorMessages.add(message);
            }
        }

        public void fail(XdmNode expected, XdmNode actual) {
            this.expected = expected;
            this.actual = actual;
        }

        public void report() {
            reporter.startTestResults(passed, testfile, title);

            if ((actualError != null && expectedError == null)
                || (actualError == null && expectedError != null)
                || (actualError != null && expectedError != null && !actualError.equals(expectedError))) {
                reporter.testError(expectedError, actualError);
            }

            reporter.testErrorMessages(errorMessages);
            reporter.testExpected(expected);
            reporter.testActual(actual);
            reporter.endTestResults(passed);
        }

        public void catchException(Throwable t) {
            while (t != null) {
                if (t.getMessage() != null) {
                    errorMessages.add(t.getMessage());
                } else {
                    errorMessages.add(t.toString());
                }
                if (t instanceof XProcException) {
                    XProcException xe = (XProcException) t;
                    actualError = xe.getErrorCode();
                    return;
                } else {
                    t = t.getCause();
                }
            }
        }

    }
}