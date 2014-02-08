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

import java.io.IOException;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.RelevantNodes;
import org.xml.sax.InputSource;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.Serializer;

import javax.xml.transform.sax.SAXSource;

import com.xmlcalabash.runtime.XPipeline;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


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

    /** Creates a new instance of RunTest */
    public RunTestReport() {
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

        RunTestReport test = new RunTestReport();
        test.runTests(tests);
    }

    public void runTests(Vector<String> tests) {
        // We create this runtime for startReport(), I know it never actually gets used...
        XProcConfiguration config = new XProcConfiguration("ee", schemaAware);
        runtime = new XProcRuntime(config);

        startReport();

        for (String testfile : tests) {
            run(testfile);
        }

        endReport();
    }

    public void run(String testfile) {
        Vector<TestResult> results = new Vector<TestResult> ();

        XProcConfiguration config = new XProcConfiguration("ee", schemaAware);
        runtime = new XProcRuntime(config);
        runtime.getConfiguration().debug = debug;

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
        } catch (Exception sae) {
            TestResult result = new TestResult(testfile);
            result.catchException(sae);
            results.add(result);
            makeReport(results);
            return;
        }

        if (t_test.equals(root.getNodeName())) {
            TestResult result = runTest(root);
            results.add(result);
            makeReport(results);
        } else {
            String title = "";
            XdmSequenceIterator iter = root.axisIterator(Axis.CHILD, t_title);
            while (iter.hasNext()) {
                XdmNode test = (XdmNode) iter.next();
                title += test.getStringValue();
            }

            iter = root.axisIterator(Axis.CHILD, t_test);
            while (iter.hasNext()) {
                XdmNode test = (XdmNode) iter.next();
                TestResult result = runTest(test);
                results.add(result);
            }

            System.out.println("<test-suite>");
            if (!"".equals(title)) {
                System.out.println("<title>" + title + "</title>");
            }
            makeReport(results);
            System.out.println("</test-suite>");
        }
    }

    public TestResult runTest(XdmNode testNode) {
        TestResult result;

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
                result = new TestResult(turi.toASCIIString());
                result.catchException(sae);
            }

            return result;
        } else {
            result = new TestResult(testNode.getBaseURI().toASCIIString());
        }

        System.err.println("Running test: " + testNode.getBaseURI());

        XProcTest t = new XProcTest(testNode);

        result.setTitle(t.title);
        result.setDescription(t.description);

        if (t.pipeline == null) {
            result.catchException(new UnsupportedOperationException("Pipeline must be provided."));
            return result;
        }

        Hashtable<String, ReadablePipe> pipeoutputs = new Hashtable<String, ReadablePipe>();
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
            Hashtable<String, Vector<XdmNode>> cinputs = new Hashtable<String, Vector<XdmNode>> ();
            for (String port : pipeoutputs.keySet()) {
                if (compare.inputPorts.contains(port)) {
                    ReadablePipe pipe = pipeoutputs.get(port);
                    while (pipe.moreDocuments()) {
                        try {
                            XdmNode p = pipe.read();
                            if (!cinputs.containsKey(port)) {
                                cinputs.put(port, new Vector<XdmNode> ());
                            }
                            cinputs.get(port).add(p);
                        } catch (SaxonApiException sae) {
                            result.catchException(sae);
                            return result;
                        }
                    }
                }
            }

            try {
                pipeoutputs = runPipe(compare.pipeline, cinputs, t.outputs, null, null);
            } catch (Exception err) {
                result.fail(err, "Compare pipelines failed: this shouldn't happen.");
            }
        }

        Hashtable<String,Vector<XdmNode>> results = new Hashtable<String,Vector<XdmNode>> ();
        Hashtable<String,Vector<XdmNode>> expects = new Hashtable<String,Vector<XdmNode>> ();

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

                        XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
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

private Hashtable<String,ReadablePipe> runPipe(XdmNode pipeline,
                                               Hashtable<String, Vector<XdmNode>> inputs,
                                               Hashtable<String, Vector<XdmNode>> outputs,
                                               Hashtable<QName, String> parameters,
                                               Hashtable<QName, String> options) throws SaxonApiException {

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
        if (debug) {
            e.printStackTrace();
        }
        throw e;
    } catch (Throwable e) {
        if (debug) {
            e.printStackTrace();
        }
        throw new XProcException(e);
    }

    Hashtable<String, ReadablePipe> pipeoutputs = new Hashtable<String, ReadablePipe> ();
    /* WTF?
    Set<String> pipeouts = xpipeline.getOutputs();
    for (String port : outputs.keySet()) {
        if (pipeouts.contains(port)) {
            ReadablePipe rpipe = xpipeline.readFrom(port);
            rpipe.canReadSequence(true);
            pipeoutputs.put(port, rpipe);
        }
    }
    */

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

    private void makeReport(Vector<TestResult> results) {
        for (TestResult result : results) {
            result.report();
        }
    }

    private void startReport() {
        GregorianCalendar cal = new GregorianCalendar();

        System.out.println("<test-report xmlns='http://xproc.org/ns/testreport'>");
        System.out.println("<title>XProc Test Results for XML Calabash</title>");
        System.out.print("<date>");
        System.out.print(cal.get(Calendar.YEAR));
        System.out.print("-");
        if (cal.get(Calendar.MONTH)+1 < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.MONTH)+1);
        System.out.print("-");
        if (cal.get(Calendar.DAY_OF_MONTH) < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.DAY_OF_MONTH));
        System.out.print("T");
        if (cal.get(Calendar.HOUR_OF_DAY) < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.HOUR_OF_DAY));
        System.out.print(":");
        if (cal.get(Calendar.MINUTE) < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.MINUTE));
        System.out.print(":");
        if (cal.get(Calendar.SECOND) < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.SECOND));
        System.out.println("</date>");

        System.out.println("<processor>");
        System.out.println("<name>" + runtime.getProductName() + "</name>");
        System.out.println("<vendor>" + runtime.getVendor() + "</vendor>");
        System.out.println("<vendor-uri>" + runtime.getVendorURI() + "</vendor-uri>");
        System.out.println("<version>" + runtime.getProductVersion() + "</version>");
        System.out.println("<language>" + runtime.getLanguage() + "</language>");
        System.out.println("<xproc-version>" + runtime.getXProcVersion() + "</xproc-version>");
        System.out.println("<xpath-version>" + runtime.getXPathVersion() + "</xpath-version>");
        System.out.println("<psvi-supported>" + runtime.getPSVISupported() + "</psvi-supported>");
        System.out.println("</processor>");
    }

    private void endReport() {
        System.out.println("</test-report>");
    }

    /*
    public void dump(XdmNode node, int depth) {
        XdmSequenceIterator iter = null;

        for (int i = 0; i < depth; i++) {
            System.err.print(" ");
        }

        if (node.getNodeKind() == XdmNodeKind.DOCUMENT) {
            System.err.println("D: " + node.getBaseURI());
            iter = node.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                dump((XdmNode) iter.next(), depth+1);
            }
            return;
        }

        if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
            System.err.println("E: " + node.getNodeName());
            iter = node.axisIterator(Axis.ATTRIBUTE);
            while (iter.hasNext()) {
                dump((XdmNode) iter.next(), depth+1);
            }
            iter = node.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                dump((XdmNode) iter.next(), depth+1);
            }
            return;
        }

        if (node.getNodeKind() == XdmNodeKind.ATTRIBUTE) {
            System.err.println("A: " + node.getNodeName() + " = \"" + node.getStringValue() + "\"");
            return;
        }

        if (node.getNodeKind() == XdmNodeKind.TEXT) {
            System.err.println("T: \"" + node.getStringValue() + "\"");
            return;
        }

        if (node.getNodeKind() == XdmNodeKind.COMMENT) {
            System.err.println("C: \"" + node.getStringValue() + "\"");
            return;
        }

        if (node.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION) {
            System.err.println("P: " + node.getNodeName() + " \"" + node.getStringValue() + "\"");
            return;
        }

        System.err.print("WTF: " + node.getNodeKind());
    }
    */

    public String serializeAsXML(XdmNode node) {
        try {
            Serializer serializer = new Serializer();

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
            sae.printStackTrace();
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
        public Hashtable<String, Vector<XdmNode>> inputs = new Hashtable<String, Vector<XdmNode>>();
        public Hashtable<String, Vector<XdmNode>> outputs = new Hashtable<String, Vector<XdmNode>>();
        public Hashtable<QName, String> parameters = new Hashtable<QName, String>();
        public Hashtable<QName, String> options = new Hashtable<QName, String>();
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
            for (XdmNode node : new RelevantNodes(runtime, pipeline,Axis.CHILD)) {
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
                for (XdmNode node : new RelevantNodes(input,Axis.CHILD,false)) {
                    if (node.getNodeKind() != XdmNodeKind.ELEMENT) {
                        continue;
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
                            add(input, port, dest.getXdmNode());
                        }
                    } else {
                        // Make sure that we have a document
                        XdmDestination dest = new XdmDestination();
                        S9apiUtils.writeXdmValue(runtime, node, dest, node.getBaseURI());
                        XdmNode newNode = dest.getXdmNode();
                        add(input, port, dest.getXdmNode());
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
                for (XdmNode node : new RelevantNodes(input,Axis.CHILD,true)) {
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

            for (XdmNode node : new RelevantNodes(runtime, input,Axis.CHILD)) {
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

            for (XdmNode node : new RelevantNodes(runtime, root,Axis.CHILD)) {
                if (XProcConstants.p_input.equals(node.getNodeName())) {
                    inputPorts.add(node.getAttributeValue(_port));
                }
                if (XProcConstants.p_output.equals(node.getNodeName())) {
                    outputPorts.add(node.getAttributeValue(_port));
                }
            }
        }
    }

    private class TestResult {
        public String testfile = null;
        public String title = "";
        public Vector<XdmNode> description = null;
        public boolean passed = false;
        public QName expectedError = null;
        public QName actualError = null;
        public Vector<String> errorMessages = new Vector<String> ();
        public XdmNode expected = null;
        public XdmNode actual = null;

        public TestResult(String testfile) {
            this.testfile = testfile;
        }

        public void setTitle(XdmNode title) {
            if (title != null) {
                this.title = title.getStringValue();
            }
        }

        public void setDescription(XdmNode desc) {
            if (desc != null) {
                XdmSequenceIterator iter = desc.axisIterator(Axis.CHILD);
                description = new Vector<XdmNode> ();
                while (iter.hasNext()) {
                    XdmNode node = (XdmNode) iter.next();
                    HashSet<String> prefixes = new HashSet<String> ();
                    XdmSequenceIterator nsiter = node.axisIterator(Axis.NAMESPACE);
                    while (nsiter.hasNext()){
                        XdmNode nsnode = (XdmNode) nsiter.next();
                        if (nsnode.getNodeName() != null) {
                            String prefix = nsnode.getNodeName().getLocalName();
                            String uri = nsnode.getStringValue();
                            if (!"http://www.w3.org/1999/xhtml".equals(uri)
                                && !"xml".equals(prefix)) {
                                prefixes.add(uri);
                            }
                        }
                    }
                    node = S9apiUtils.removeNamespaces(runtime, node, prefixes, true);
                    description.add(node);
                }
            }
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
            String gi = "pass";
            if (!passed) {
                gi = "fail";
            }

            System.out.println("<" + gi + " uri='" + testfile + "'>");

            if (title != null) {
                System.out.println("<title>" + title + "</title>");
            }

            /*
            if (description != null) {
                System.out.println("<description>");
                for (XdmNode node : description) {
                    System.out.println(serializeAsXML(node));
                }
                System.out.println("</description>");
            }
            */

            if ((actualError != null && expectedError == null)
                || (actualError == null && expectedError != null)
                || (actualError != null && expectedError != null && !actualError.equals(expectedError))) {
                System.out.print("<error");
                if (expectedError != null) {
                    System.out.print(" expected='" + expectedError + "'");
                }
                System.out.println(">" + actualError + "</error>");
            }

            for (String message : errorMessages) {
                System.out.println("<message>" + xmlEscape(message) + "</message>");
            }

            if (expected != null) {
                System.out.print("<expected>");
                System.out.print(serialize(expected));
                System.out.println("</expected>");
            }

            if (actual != null) {
                System.out.print("<actual>");
                System.out.print(serialize(actual));
                System.out.println("</actual>");
            }

            System.out.println("</" + gi + ">");
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

        private String xmlEscape(String str) {
            str = str.replaceAll("&", "&amp;");
            str = str.replaceAll("<", "&lt;");
            str = str.replaceAll(">", "&gt;");
            return str;
        }
    }
}