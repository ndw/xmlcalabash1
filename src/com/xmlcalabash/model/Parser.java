package com.xmlcalabash.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import java.util.Stack;
import java.util.HashSet;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.xml.transform.sax.SAXSource;
import javax.xml.XMLConstants;

import com.xmlcalabash.extensions.UntilUnchanged;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.xml.sax.InputSource;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.util.TypeUtils;
import com.xmlcalabash.util.URIUtils;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author ndw
 */
public class Parser {
    // TODO: Make new QName() values throughout static

    private static QName px_name = new QName(XProcConstants.NS_CALABASH_EX,"name");
    private static QName _name = new QName("name");
    private static QName _href = new QName("href");
    private static QName _type = new QName("type");
    private static QName _version = new QName("version");
    private static QName err_XS0063 = new QName(XProcConstants.NS_XPROC_ERROR, "XS0063");
    private static QName p_use_when = new QName(XProcConstants.NS_XPROC, "use-when");
    private static QName _use_when = new QName("use-when");
    private static QName _exclude_inline_prefixes = new QName("exclude-inline-prefixes");

    private XProcRuntime runtime = null;
    private Stack<DeclareStep> declStack = null;
    protected HashSet<String> topLevelImports = new HashSet<String> ();
    private boolean loadingStandardLibrary = false;                                                             
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public Parser(XProcRuntime runtime) {
        this.runtime = runtime;
        declStack = new Stack<DeclareStep> ();
    }
    
    public DeclareStep loadPipeline(String uri) throws SaxonApiException {
        XdmNode doc = runtime.parse(uri, URIUtils.cwdAsURI().toASCIIString());
        XdmNode root = S9apiUtils.getDocumentElement(doc);

        if (!XProcConstants.p_declare_step.equals(root.getNodeName())
            && !XProcConstants.p_pipeline.equals(root.getNodeName())) {
            throw new UnsupportedOperationException("Pipelines must be p:pipeline or p:declare-step documents");
        }

        return usePipeline(root);
    }

    public DeclareStep usePipeline(XdmNode node) {
        if (node.getNodeKind() == XdmNodeKind.DOCUMENT) {
            node = S9apiUtils.getDocumentElement(node);
        }
        DeclareStep decl = readDeclareStep(node);
        parseDeclareStepBodyPassTwo(decl);
        return decl;
    }


    public PipelineLibrary loadStandardLibrary() throws FileNotFoundException, URISyntaxException, SaxonApiException {
        URI home = URIUtils.homeAsURI();
        URI cwd = URIUtils.cwdAsURI();
        URI puri = home;

        InputStream instream = getClass().getResourceAsStream("/etc/pipeline-library.xml");
        if (instream == null) {
            throw new UnsupportedOperationException("Failed to load standard pipeline library from JAR file");
        }

        XdmNode doc = parse(instream, puri);
        XdmNode root = S9apiUtils.getDocumentElement(doc);

        if (!XProcConstants.p_library.equals(root.getNodeName())) {
            throw new UnsupportedOperationException("Pipeline libraries must be p:library documents");
        }

        loadingStandardLibrary = true;
        PipelineLibrary library = readLibrary(null, root);
        loadingStandardLibrary = false;

        return library;
    }

    private XdmNode loadExtensionLibrary() throws FileNotFoundException, URISyntaxException, SaxonApiException {
        URI home = URIUtils.homeAsURI();
        URI puri = home;

        InputStream instream = getClass().getResourceAsStream("/etc/extension-library.xml");
        if (instream == null) {
            throw new UnsupportedOperationException("Failed to load XProc pipeline library from JAR file");
        }

        XdmNode doc = parse(instream, puri);
        XdmNode root = S9apiUtils.getDocumentElement(doc);

        if (!XProcConstants.p_library.equals(root.getNodeName())) {
            throw new UnsupportedOperationException("Pipeline libraries must be p:library documents");
        }

        return doc;
    }

    public PipelineLibrary loadLibrary(String libraryURI) throws SaxonApiException {
        XdmNode doc = runtime.parse(libraryURI, URIUtils.cwdAsURI().toASCIIString());
        XdmNode root = S9apiUtils.getDocumentElement(doc);
        return useLibrary(root);
    }

    public PipelineLibrary useLibrary(XdmNode root) throws SaxonApiException {
        if (!XProcConstants.p_library.equals(root.getNodeName())) {
            throw new UnsupportedOperationException("Pipelines libraries must be p:library documents");
        }

        if (declStack.isEmpty()) {
            topLevelImports.add(root.getBaseURI().toASCIIString());
        } else {
            declStack.peek().addImport(root.getBaseURI().toASCIIString());
        }

        return readLibrary(null, root);
    }

    private XdmNode parse(InputStream instream, URI baseURI) throws SaxonApiException {
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setEntityResolver(runtime.getResolver());
            SAXSource source = new SAXSource(reader, new InputSource(instream));
            DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
            builder.setLineNumbering(true);
            builder.setDTDValidation(false);
            builder.setBaseURI(baseURI);
            return builder.build(source);
        } catch (SAXException se) {
            throw new XProcException(se);
        }
    }

    private PipelineLibrary readLibrary(Step parent, XdmNode node) {
        if (!XProcConstants.p_library.equals(node.getNodeName())
                && !XProcConstants.p_pipeline.equals(node.getNodeName())
                && !XProcConstants.p_declare_step.equals(node.getNodeName())) {
            runtime.error(null, node,"Not a pipeline or library: " + node.getNodeName(), XProcConstants.staticError(52));
            return null;
        }

        PipelineLibrary library = new PipelineLibrary(runtime, node);

        if (XProcConstants.p_library.equals(node.getNodeName())) {
            checkAttributes(node, new String[] { "xpath-version", "psvi-required", "version", "exclude-inline-prefixes"}, false);

            library.setVersion(inheritedVersion(node));

            // Read all the steps and make them available
            for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
                if (snode.getNodeName().equals(XProcConstants.p_import)) {
                    // skip it
                } else {
                    Step substep = readStep(library, snode);

                    if (substep instanceof DeclareStep) {
                        library.addStep((DeclareStep) substep);
                    } else {
                        throw new UnsupportedOperationException("A p:library must contain only p:pipeline and p:declare-steps.");
                    }
                }
            }

            for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
                if (snode.getNodeName().equals(XProcConstants.p_import)) {
                    Step substep = readStep(library, snode);
                    Import importElem = (Import) substep;
                    XdmNode root = importElem.getRoot();
                    // root will be null if the library has already been imported
                    if (root != null) {
                        importElem.setLibrary(readLibrary(library, root));
                    }
                }
            }
        } else {
            Step substep = readStep(library, node);

            if (XProcConstants.NS_CALABASH_EX.equals(substep.getDeclaredType().getNamespaceURI())
                    && (substep.getDeclaredType().getLocalName().startsWith("anonymousType"))) {
                throw XProcException.staticError(53, node, "No type attribute on imported pipeline.");
            }

            if (substep instanceof DeclareStep) {
                library.addStep((DeclareStep) substep);
            } else {
                throw new UnsupportedOperationException("A p:library must contain only p:pipeline and p:declare-steps.");
            }
        }

        checkExtensionAttributes(node, library);

        return library;
    }

    private Vector<XdmNode> readSignature(Step step) {
        Vector<XdmNode> rest = new Vector<XdmNode> ();
        boolean allowPrimary = false;
        boolean allowVariables = false;
        int primaryParamInputCount = 0;
        int primaryDocInputCount = 0;
        int primaryOutputCount = 0;

        HashSet<QName> sig = new HashSet<QName> ();
        if (XProcConstants.p_pipeline.equals(step.getType())
                || XProcConstants.p_declare_step.equals(step.getType())) {
            sig.add(XProcConstants.p_import);
            sig.add(XProcConstants.p_input);
            sig.add(XProcConstants.p_output);
            sig.add(XProcConstants.p_log);
            sig.add(XProcConstants.p_option);
            sig.add(XProcConstants.p_serialization);
            allowVariables = true;
            allowPrimary = XProcConstants.p_declare_step.equals(step.getType()); // Not on p:pipeline
        } else if (XProcConstants.p_for_each.equals(step.getType())) {
            sig.add(XProcConstants.p_iteration_source);
            sig.add(XProcConstants.p_output);
            sig.add(XProcConstants.p_log);
            allowVariables = true;
            allowPrimary = true;
        } else if (XProcConstants.cx_until_unchanged.equals(step.getType())) {
            sig.add(XProcConstants.p_iteration_source);
            sig.add(XProcConstants.p_output);
            sig.add(XProcConstants.p_log);
            allowVariables = true;
            allowPrimary = true;
        } else if (XProcConstants.p_viewport.equals(step.getType())) {
            sig.add(XProcConstants.p_viewport_source);
            sig.add(XProcConstants.p_output);
            sig.add(XProcConstants.p_log);
            allowVariables = true;
            allowPrimary = true;
        } else if (XProcConstants.p_choose.equals(step.getType())) {
            sig.add(XProcConstants.p_xpath_context);
            allowVariables = true;
            allowPrimary = true;
        } else if (XProcConstants.p_when.equals(step.getType())) {
            sig.add(XProcConstants.p_xpath_context);
            sig.add(XProcConstants.p_output);
            sig.add(XProcConstants.p_log);
            allowVariables = true;
            allowPrimary = true;
        } else if (XProcConstants.p_group.equals(step.getType())
                   || XProcConstants.p_catch.equals(step.getType())
                   || XProcConstants.p_otherwise.equals(step.getType())) {
            sig.add(XProcConstants.p_output);
            sig.add(XProcConstants.p_log);
            allowVariables = true;
            allowPrimary = true;
        } else if (XProcConstants.p_try.equals(step.getType())) {
            allowVariables = true;
        } else {
            sig.add(XProcConstants.p_input);
            sig.add(XProcConstants.p_log);
            sig.add(XProcConstants.p_with_option);
            sig.add(XProcConstants.p_with_param);
        }

        Hashtable<String,Serialization> serializations = new Hashtable<String,Serialization> ();
        Hashtable<String,Log> loggers = new Hashtable<String,Log> ();

        int pos = 1;
        boolean done = false;
        for (XdmNode node : new RelevantNodes(runtime, step.getNode(), Axis.CHILD)) {
            if (done) {
                if (node.getNodeKind() == XdmNodeKind.TEXT) {
                    throw XProcException.staticError(37, node, "Unexpected text: " + node.getStringValue());
                }
                
                rest.add(node);
                continue;
            }

            if (sig.contains(node.getNodeName())) {
                QName nodeName = node.getNodeName();
                
                if (XProcConstants.p_input.equals(nodeName)
                    || XProcConstants.p_iteration_source.equals(nodeName)
                    || XProcConstants.p_viewport_source.equals(nodeName)
                    || XProcConstants.p_xpath_context.equals(nodeName)) {
                    Input input = readInput(step, node);

                    if (input.getPrimarySet() && input.getPrimary()) {
                        if (!allowPrimary) {
                            throw XProcException.staticError(8, node, "The \"primary\" attribute is not allowed in this context.");
                        }

                        if (input.getParameterInput()) {
                            primaryParamInputCount++;
                            if (primaryParamInputCount > 1) {
                                throw XProcException.staticError(30, node, "You cannot have more than one primary parameter input port.");
                            }
                        } else {
                            primaryDocInputCount++;
                            if (primaryDocInputCount > 1) {
                                throw XProcException.staticError(30, node, "You cannot have more than one primary input port.");
                            }
                        }
                    }

                    input.setPosition(pos++);
                    if (step.getInput(input.getPort()) != null || step.getOutput(input.getPort()) != null) {
                        runtime.error(null, node, "Duplicate port name: " + input.getPort(), XProcConstants.staticError(11));
                    } else {
                        step.addInput(input);
                    }
                } else if (XProcConstants.p_output.equals(nodeName)) {
                    Output output = readOutput(step, node);

                    if (output.getPrimarySet() && output.getPrimary()) {
                        if (!allowPrimary) {
                            throw XProcException.staticError(8, node, "The \"primary\" attribute is not allowed in this context.");
                        }
                        primaryOutputCount++;
                        if (primaryOutputCount > 1) {
                            throw XProcException.staticError(14, node, "You cannot have more than one primary output port.");
                        }
                    }

                    // Output bindings are really *input* bindings. They're where the processor
                    // has to *read from* to get the output...
                    if (output.getBinding().size() != 0) {
                        Input input = new Input(runtime, output.getNode());
                        input.setPort("|" + output.getPort());
                        for (Binding binding : output.getBinding()) {
                            input.addBinding(binding);
                        }
                        output.clearBindings();

                        input.setPrimary(output.getPrimary());
                        input.setSequence(output.getSequence());
                        step.addInput(input);
                    }

                    if (step.getInput(output.getPort()) != null || step.getOutput(output.getPort()) != null) {
                        runtime.error(null, node, "Duplicate port name: " + output.getPort(), XProcConstants.staticError(11));
                    } else {
                        step.addOutput(output);
                    }
                } else if (XProcConstants.p_option.equals(nodeName)
                           || XProcConstants.p_with_option.equals(nodeName)) {

                    if (XProcConstants.p_pipeline.equals(step.getType())
                            || XProcConstants.p_declare_step.equals(step.getType())) {
                        if (XProcConstants.p_with_option.equals(nodeName)) {
                            throw XProcException.staticError(44, node, "Can't use p:with-option here.");
                        }
                    } else {
                        if (XProcConstants.p_option.equals(nodeName)) {
                            throw XProcException.staticError(44, node, "Can't use p:option here.");
                        }
                    }

                    Option option = readOption(step, node);
                    option.setStep(step);
                    step.addOption(option);
                } else if (XProcConstants.p_with_param.equals(nodeName)) {
                    Parameter param = readParameter(step, node);
                    param.setStep(step);
                    param.setPosition(pos++);
                    step.addParameter(param);
                } else if (XProcConstants.p_log.equals(nodeName)) {
                    Log log = readLog(node);
                    if (log.getPort() == null) {
                        throw XProcException.staticError(26, node, "A p:log must specify a port.");
                    }
                    if (log.getPort() != null && loggers.containsKey(log.getPort())) {
                        throw XProcException.staticError(26, node, "A p:log was specified more than once for the same port: " + log.getPort());
                    }
                    loggers.put(log.getPort(), log);
                } else if (XProcConstants.p_import.equals(nodeName)) {
                    rest.add(node);
                } else if (XProcConstants.p_serialization.equals(nodeName)) {
                    Serialization ser = readSerialization(node);
                    String port = ser.getPort();
                    if (port == null || serializations.containsKey(port)) {
                        throw XProcException.staticError(39, node, "A p:serialization must specify a port and can only be specified once.");
                    }

                    serializations.put(port, ser);
                } else {
                    throw XProcException.staticError(44, node, "Unexpected element: " + nodeName);
                }
            } else {
                if (node.getNodeKind() == XdmNodeKind.TEXT) {
                    throw XProcException.staticError(37, node, "Unexpected text: " + node.getStringValue());
                }
                
                done = true;
                rest.add(node);
            }
        }

        // Make sure single inputs and outputs are primary by default
        int inputCount = 0;
        Input maybePrimaryInput = null;
        for (Input input : step.inputs()) {
            if (!input.getParameterInput()) {
                inputCount++;
                maybePrimaryInput = input;
            }
        }

        if (inputCount == 1 && !maybePrimaryInput.getPrimary() && !maybePrimaryInput.getPrimarySet()) {
            maybePrimaryInput.setPrimary(true);
        }

        inputCount = 0;
        maybePrimaryInput = null;
        for (Input input : step.inputs()) {
            if (input.getParameterInput()) {
                inputCount++;
                maybePrimaryInput = input;
            }
        }

        if (inputCount == 1 && !maybePrimaryInput.getPrimary() && !maybePrimaryInput.getPrimarySet()) {
            maybePrimaryInput.setPrimary(true);
        }

        int outputCount = 0;
        Output maybePrimaryOutput = null;
        for (Output output : step.outputs()) {
            outputCount++;
            maybePrimaryOutput = output;
        }

        if (outputCount == 1 && !maybePrimaryOutput.getPrimary() && !maybePrimaryOutput.getPrimarySet()) {
            maybePrimaryOutput.setPrimary(true);
        }

        if (XProcConstants.p_declare_step.equals(step.getType())) {
            // We have to check for output serializations here.
            for (Output output : step.outputs()) {
                String port = output.getPort();
                if (serializations.containsKey(port)) {
                    Serialization serial = serializations.get(port);
                    output.setSerialization(serial);
                    serializations.remove(port);
                }
            }
        
            if (serializations.size() != 0) {
                // We know this is true here and now because p:pipeline can't have defaulted inputs/outputs
                throw XProcException.staticError(39, step.getNode(), "A p:serialization specifies a non-existant port.");
            }
        }

        for (String port : loggers.keySet()) {
            step.addLog(loggers.get(port));
        }

        boolean vars = false;
        for (XdmNode node : rest) {
            vars = vars || XProcConstants.p_variable.equals(node.getNodeName());
        }

        if (vars) {
            if (!allowVariables) {
                throw XProcException.staticError(44, step.getNode(), "Variables are not allowed here");
            }

            // p:declare-step/p:pipeline is a special case, see readDeclareStep
            if (!XProcConstants.p_pipeline.equals(step.getType())
                    && !XProcConstants.p_declare_step.equals(step.getType())) {
                while (rest.size() > 0 && XProcConstants.p_variable.equals(rest.get(0).getNodeName())) {
                    Variable var = readVariable(step, rest.remove(0));
                    step.addVariable(var);
                }
            }
        }

        return rest.size() == 0 ? null : rest;
    }    

    private Input readInput(Step parent, XdmNode node) {
        QName nodeName = node.getNodeName();
        
        if (XProcConstants.p_input.equals(nodeName)) {
            checkAttributes(node, new String[] { "kind", "port", "primary", "sequence", "select" }, false);
        } else if (XProcConstants.p_iteration_source.equals(nodeName)) {
            checkAttributes(node, new String[] { "select" }, false);
        } else if (XProcConstants.p_viewport_source.equals(nodeName)
                   || XProcConstants.p_xpath_context.equals(nodeName)) {
            checkAttributes(node, null, false);
        } else {
            throw new UnsupportedOperationException("Unexpected name in readInput: " + nodeName);
        }

        String kind = node.getAttributeValue(new QName("kind"));
        String port = checkNCName(node.getAttributeValue(new QName("port")));
        String primary = node.getAttributeValue(new QName("primary"));
        String sequence = node.getAttributeValue(new QName("sequence"));
        String select = node.getAttributeValue(new QName("select"));

        if (port == null && XProcConstants.p_input.equals(node.getNodeName())) {
            throw XProcException.staticError(38, node, "You must specify a port name for all p:input ports.");
        }

        if (kind == null) {
            kind = "document";
        }
        
        if (!"document".equals(kind) && !"parameter".equals(kind)) {
            runtime.error(null, node, "Kind must be document or parameter", XProcConstants.staticError(33));
        }

        if (primary != null &&  !"true".equals(primary) && !"false".equals(primary)) {
            runtime.error(null, node, "Primary must be 'true' or 'false'", XProcConstants.staticError(40));
        }
        
        if (sequence != null) {
            if ("parameter".equals(kind)) {
                if (!"true".equals(sequence)) {
                    runtime.error(null, node, "Sequence cannot be 'false' on a parameter input", XProcConstants.staticError(40));
                }
            } else {
                if (!"true".equals(sequence) && !"false".equals(sequence)) {
                    runtime.error(null, node, "Sequence must be 'true' or 'false'", XProcConstants.staticError(40));
                }                
            }
        }
        
        if (XProcConstants.p_iteration_source.equals(nodeName)) {
            port = "#iteration-source";
            sequence = "true";
            primary = "true";
        } else if (XProcConstants.p_viewport_source.equals(nodeName)) {
            port = "#viewport-source";
            sequence = "false";
            primary = "true";
        } else if (XProcConstants.p_xpath_context.equals(nodeName)) {
            port = "#xpath-context";
            sequence = "false";
            primary = "true";
        } else if (port == null) {
            throw XProcException.staticError(44, node, "No port name specified on input.");
        }

        Input input = new Input(runtime, node);
        input.setPort(port);
        input.setSequence(sequence);
        input.setPrimary(primary);
        
        if ("parameter".equals(kind)) {
            input.setParameterInput();
            input.setSequence(true);
        }
        
        input.setDebugReader(node.getAttributeValue(new QName(XProcConstants.NS_CALABASH_EX, "debug-reader")) != null);
        input.setDebugWriter(node.getAttributeValue(new QName(XProcConstants.NS_CALABASH_EX, "debug-writer")) != null);
        
        if (select != null) {
            input.setSelect(select);
        }

        for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
            Binding binding = readBinding(parent, snode);
            if (binding != null) {
                input.addBinding(binding);
            }
        }

        checkExtensionAttributes(node, input);

        return input;
    }

    private Output readOutput(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "port", "primary", "sequence" }, false);

        String port = checkNCName(node.getAttributeValue(new QName("port")));

        if (port == null) {
            throw XProcException.staticError(38, node, "You must specify a port name for all p:output ports.");
        }

        String primary = node.getAttributeValue(new QName("primary"));
        String sequence = node.getAttributeValue(new QName("sequence"));

        Output output = new Output(runtime, node);
        output.setPort(port);
        output.setSequence(sequence);
        output.setPrimary(primary);

        for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
            Binding binding = readBinding(parent, snode);
            if (binding != null) {
                output.addBinding(binding);
            }
        }

        checkExtensionAttributes(node, output);

        return output;
    }
    
    private Binding readBinding(Step parent, XdmNode node) {
        Binding binding = null;

        QName nodeName = node.getNodeName();
        if (XProcConstants.p_pipe.equals(nodeName)) {
            binding = readPipe(node);
        } else if (XProcConstants.p_document.equals(nodeName)) {
            binding = readDocument(node);
        } else if (XProcConstants.p_inline.equals(nodeName)) {
            binding = readInline(parent, node);
        } else if (XProcConstants.p_empty.equals(nodeName)) {
            binding = readEmpty(node);
        } else if (XProcConstants.p_data.equals(nodeName)) {
            binding = readData(node);
        } else {
            throw XProcException.staticError(44, node, "Unexpected in input: " + nodeName);
        }

        checkExtensionAttributes(node, binding);

        return binding;
    }
    
    private PipeNameBinding readPipe(XdmNode node) {
        checkAttributes(node, new String[] { "port", "step" }, false);
        
        String step = checkNCName(node.getAttributeValue(new QName("step")));
        String port = checkNCName(node.getAttributeValue(new QName("port")));

        if (step == null || port == null) {
            if (step == null) {
                throw XProcException.staticError(38, node, "Missing step attribute.");
            } else {
                throw XProcException.staticError(38, node, "Missing port attribute.");
            }
        }

        PipeNameBinding pipe = new PipeNameBinding(runtime, node);

        pipe.setStep(step);
        pipe.setPort(port);

        RelevantNodes rnodes = new RelevantNodes(runtime, node, Axis.CHILD);
        Iterator<XdmNode> iter = rnodes.iterator();
        while (iter.hasNext()) {
            XdmNode snode = iter.next();
            throw new IllegalArgumentException("Unexpected in pipe: " + snode.getNodeName());
        }

        checkExtensionAttributes(node, pipe);

        return pipe;
    }
    
    private DocumentBinding readDocument(XdmNode node) {
        checkAttributes(node, new String[] { "href" }, false);

        String href = node.getAttributeValue(new QName("href"));
        
        DocumentBinding doc = new DocumentBinding(runtime, node);
        doc.setHref(href);

        for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
            throw new IllegalArgumentException("Unexpected in document: " + snode.getNodeName());
        }

        checkExtensionAttributes(node, doc);

        return doc;
    }

    private DataBinding readData(XdmNode node) {
        checkAttributes(node, new String[] { "href", "wrapper", "wrapper-namespace", "wrapper-prefix", "content-type" }, false);

        String href = node.getAttributeValue(new QName("href"));
        String wrapstr = node.getAttributeValue(new QName("wrapper"));
        String wrappfx = node.getAttributeValue(new QName("wrapper-prefix"));
        String wrapns  = node.getAttributeValue(new QName("wrapper-namespace"));
        String contentType = node.getAttributeValue(new QName("content-type"));

        if (wrappfx != null && wrapns == null) {
            throw XProcException.dynamicError(34, node, "You cannot specify a prefix without a namespace.");
        }

        if (wrapns != null && wrapstr == null) {
            throw XProcException.dynamicError(34, node, "You cannot specify a namespace without a wrapper.");
        }

        if (wrapns != null && wrapstr != null && wrapstr.indexOf(":") >= 0) {
            throw XProcException.dynamicError(34, node, "You cannot specify a namespace if the wrapper name contains a colon.");
        }

        if (wrapns == null && wrapstr != null && wrapstr.indexOf(":") <= 0) {
            throw XProcException.dynamicError(25, node, "FIXME: what error is this?");
        }
        
        DataBinding doc = new DataBinding(runtime, node);
        doc.setHref(href); // FIXME: what about making it absolute?

        if (wrapstr != null) {
            if (wrapstr.indexOf(":") > 0) {
                doc.setWrapper(new QName(wrapstr, node));
            } else if (wrappfx != null) {
                doc.setWrapper(new QName(wrappfx, wrapns, wrapstr));
            } else {
                doc.setWrapper(new QName(wrapns, wrapstr));
            }
        }

        if (contentType != null) {
            doc.setContentType(contentType);
        }

        for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
            throw new IllegalArgumentException("Unexpected in document: " + snode.getNodeName());
        }

        checkExtensionAttributes(node, doc);

        return doc;
    }

    private EmptyBinding readEmpty(XdmNode node) {
        checkAttributes(node, new String[] { }, false);

        EmptyBinding empty = new EmptyBinding(runtime, node);

        for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
            throw new IllegalArgumentException("Unexpected in empty: " + snode.getNodeName());
        }

        checkExtensionAttributes(node, empty);

        return empty;
    }
    
    private InlineBinding readInline(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "exclude-inline-prefixes" }, false);

        InlineBinding inline = new InlineBinding(runtime, node);

        boolean seenelem = false;
        XdmSequenceIterator iter = node.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                if (seenelem) {
                    throw new IllegalArgumentException("Not a well-formed inline document");
                }
                seenelem = true;
            }
            inline.addNode(child);
        }

        HashSet<String> excludeURIs = S9apiUtils.excludeInlinePrefixes(node, node.getAttributeValue(_exclude_inline_prefixes));
        while (!(parent instanceof DeclareStep)) {
            parent = parent.parent;
        }

        if (parent instanceof DeclareStep) {
            HashSet<String> excluded = ((DeclareStep) parent).getExcludeInlineNamespaces();
            if (excluded != null) {
                for (String uri : excluded) {
                    excludeURIs.add(uri);
                }
            }
        } else {
            throw new UnsupportedOperationException("This can't happen: parent of inline is not a step!?");
        }
        
        checkExtensionAttributes(node, inline);

        inline.excludeNamespaces(excludeURIs);

        return inline;
    }

    private Option readOption(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "name", "required", "select" }, false);

        String name = node.getAttributeValue(new QName("name"));
        String required = node.getAttributeValue(new QName("required"));
        String select = node.getAttributeValue(new QName("select"));
        String type = node.getAttributeValue(XProcConstants.cx_type);

        if (name == null) {
            throw XProcException.staticError(38, node, "Attribute \"name\" required on p:with-option");
        }

        QName oname;
        if (name.contains(":")) {
            oname = new QName(name, node);
        } else {
            oname = new QName(name);
        }
        
        if (XProcConstants.NS_XPROC.equals(oname.getNamespaceURI())) {
            throw XProcException.staticError(28, node, "You cannot specify an option in the p: namespace.");
        }

        if (required != null && !"false".equals(required) && !"true".equals(required)) {
            throw XProcException.staticError(19, node, "The required attribute must be 'true' or 'false'.");
        }
        
        Option option = new Option(runtime, node);
        option.setName(oname);
        option.setRequired(required);
        option.setSelect(select);
        option.setType(type, node);

        readNamespaceBindings(parent, option, node, select);

        checkExtensionAttributes(node, option);

        return option;
    }

    private Parameter readParameter(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "port", "name", "select" }, false);

        String name = node.getAttributeValue(new QName("name"));
        String select = node.getAttributeValue(new QName("select"));
        String port = checkNCName(node.getAttributeValue(new QName("port")));

        if (name == null) {
            runtime.error(null, node, "Attribute \"name\" required on p:with-param", XProcConstants.staticError(38));
        }

        Parameter parameter = new Parameter(runtime, node);
        parameter.setPort(port);

        // If the name contains a colon, get the namespace from the node, otherwise it's in no namespace
        if (name.contains(":")) {
            parameter.setName(new QName(name, node));
        } else {
            parameter.setName(new QName("", name));
        }

        parameter.setSelect(select);

        readNamespaceBindings(parent, parameter, node, select);

        checkExtensionAttributes(node, parameter);

        return parameter;
    }

    private Variable readVariable(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "name", "select" }, false);

        String name = node.getAttributeValue(new QName("name"));
        String select = node.getAttributeValue(new QName("select"));

        QName oname = new QName(name, node);

        if (XProcConstants.NS_XPROC.equals(oname.getNamespaceURI())) {
            throw XProcException.staticError(28, node, "You cannot specify a variable in the p: namespace.");
        }

        Variable variable = new Variable(runtime, node);
        variable.setName(oname);
        variable.setSelect(select);

        readNamespaceBindings(parent, variable, node, select);

        checkExtensionAttributes(node, variable);

        return variable;
    }

    private void readNamespaceBindings(Step parent, EndPoint endpoint, XdmNode node, String select) {
        boolean hadNamespaceBinding = false;
        for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
            QName nodeName = snode.getNodeName();

            if (XProcConstants.p_namespaces.equals(nodeName)) {
                hadNamespaceBinding = true;
                NamespaceBinding nsbinding = new NamespaceBinding(runtime, snode);
                checkAttributes(snode, new String[]{"binding", "element", "except-prefixes"}, false);

                String value = snode.getAttributeValue(new QName("binding"));
                if (value != null) {
                    nsbinding.setBinding(value);
                }

                value = snode.getAttributeValue(new QName("element"));
                if (value != null) {
                    nsbinding.setXPath(value);
                }

                value = snode.getAttributeValue(new QName("except-prefixes"));
                if (value != null) {
                    for (String pfx : value.split("\\s+")) {
                        // Is this really the best way?
                        try {
                            QName n = new QName(pfx+":localName",snode);
                            nsbinding.addExcludedNamespace(n.getNamespaceURI());
                        } catch (IllegalArgumentException iae) {
                            // Bad prefix
                            throw XProcException.staticError(51, node, "Unbound prefix in except-prefixes: " + pfx);
                        }
                    }
                }

                // FIXME: ? HACK!
                ((ComputableValue) endpoint).addNamespaceBinding(nsbinding);

                for (XdmNode tnode : new RelevantNodes(runtime, snode, Axis.CHILD)) {
                    throw XProcException.staticError(44, snode, "p:namespaces must be empty");
                }
            } else {
                Binding binding = readBinding(parent, snode);
                if (binding != null) {
                    if (XProcConstants.p_option.equals(node.getNodeName())) {
                        throw XProcException.staticError(44, node, "No bindings allowed.");
                    }
                    endpoint.addBinding(binding);
                }
            }
        }

        if (!hadNamespaceBinding) {
            if (select != null && select.matches("^\\$[a-zA-Z_][-a-zA-Z0-9_]*$")) {
                String varname = select.substring(1);
                // FIXME: need to work out the nsbindings later
            }
            // Hack: Rely on the fact that getVariables don't have binding, element, or except-prefixes attributes...
            NamespaceBinding nsbinding = new NamespaceBinding(runtime, node);
            ((ComputableValue) endpoint).addNamespaceBinding(nsbinding);
        }
    }

    private Serialization readSerialization(XdmNode node) {
        checkAttributes(node, new String[] { "port", "byte-order-mark", "cdata-section-elements",
                "doctype-public", "doctype-system", "encoding", "escape-uri-attributes",
                "include-content-type", "indent", "media-type", "method", "normalization-form",
                "omit-xml-declaration", "standalone", "undeclare-prefixes", "version"}, false);

        Serialization serial = new Serialization(runtime, node);

        String value = node.getAttributeValue(new QName("port"));
        serial.setPort(value);
        
        value = node.getAttributeValue(new QName("byte-order-mark"));
        if (value != null) {
            checkBoolean(node, "byte-order-mark", value);
            serial.setByteOrderMark("true".equals(value));
        }
        
        value = node.getAttributeValue(new QName("cdata-section-elements"));
        if (value != null) {
            throw new UnsupportedOperationException("cdata-section-elements not yet supported");
        }

        value = node.getAttributeValue(new QName("doctype-public"));
        serial.setDoctypePublic(value);

        value = node.getAttributeValue(new QName("doctype-system"));
        serial.setDoctypeSystem(value);
        
        value = node.getAttributeValue(new QName("encoding"));
        serial.setEncoding(value);
        
        value = node.getAttributeValue(new QName("escape-uri-attributes"));
        if (value != null) {
            checkBoolean(node, "escape-uri-attributes", value);
            serial.setEscapeURIAttributes("true".equals(value));
        }
        
        value = node.getAttributeValue(new QName("include-content-type"));
        if (value != null) {
            checkBoolean(node, "include-content-type", value);
            serial.setIncludeContentType("true".equals(value));
        }

        value = node.getAttributeValue(new QName("indent"));
        if (value != null) {
            checkBoolean(node, "indent", value);
            serial.setIndent("true".equals(value));
        }

        value = node.getAttributeValue(new QName("media-type"));
        serial.setMediaType(value);
        
        value = node.getAttributeValue(new QName("method"));
        if (value != null) {
            QName name = new QName(value, node);
            if ("".equals(name.getPrefix())) {
                String method = name.getLocalName();
                if ("html".equals(method) || "xhtml".equals(method) || "text".equals(method) || "xml".equals(method)) {
                    serial.setMethod(name);
                } else {
                    runtime.error(null, node,
                            "Only the xml, xhtml, html, and text serialization methods are supported.",
                            XProcConstants.stepError(1));
                }
            } else {
                runtime.error(null, node,
                        "Only the xml, xhtml, html, and text serialization methods are supported.",
                        XProcConstants.stepError(1));
            }
        }
        
        value = node.getAttributeValue(new QName("normalization-form"));
        serial.setNormalizationForm(value);
        
        value = node.getAttributeValue(new QName("omit-xml-declaration"));
        if (value != null) {
            checkBoolean(node, "omit-xml-declaration", value);
            serial.setOmitXMLDeclaration("true".equals(value));
        }

        value = node.getAttributeValue(new QName("standalone"));
        serial.setStandalone(value);
        
        value = node.getAttributeValue(new QName("undeclare-prefixes"));
        if (value != null) {
            checkBoolean(node, "undeclare-prefixes", value);
            serial.setUndeclarePrefixes("true".equals(value));
        }

        value = node.getAttributeValue(new QName("version"));
        serial.setVersion(value);

        for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
            throw XProcException.staticError(44, node, "p:serialization must be empty.");
        }

        checkExtensionAttributes(node, serial);

        return serial;
    }

    private void checkBoolean(XdmNode node, String name, String value) {
        if (value != null && !"true".equals(value) && !"false".equals(value)) {
            runtime.error(null, node, name + " on serialization must be 'true' or 'false'", XProcConstants.staticError(40));
        }
    }
    
    private Log readLog(XdmNode node) {
        checkAttributes(node, new String[] { "port", "href" }, false);
        String port = checkNCName(node.getAttributeValue(new QName("port")));
        String href = node.getAttributeValue(new QName("href"));

        URI hrefURI = null;
        if (href != null) {
            hrefURI = node.getBaseURI().resolve(href);
        }

        Log log = new Log(runtime, node);
        log.setPort(port);
        log.setHref(hrefURI);

        checkExtensionAttributes(node, log);

        for (XdmNode snode : new RelevantNodes(runtime, node, Axis.CHILD)) {
            throw XProcException.staticError(44, node, "p:log must be empty");
        }

        checkExtensionAttributes(node, log);

        return log;
    }

    private Step readStep(Step parent, XdmNode node) {
        QName stepType = node.getNodeName();

        if (XProcConstants.p_declare_step.equals(stepType)
                || XProcConstants.p_pipeline.equals(stepType)) {
            return readDeclareStep(node);
        } else if (XProcConstants.p_import.equals(stepType)) {
            return readImport(node);
        } else if (XProcConstants.p_for_each.equals(stepType)) {
            return readForEach(parent, node);
        } else if (XProcConstants.p_viewport.equals(stepType)) {
            return readViewport(parent, node);
        } else if (XProcConstants.p_choose.equals(stepType)) {
            return readChoose(parent, node);
        } else if (XProcConstants.p_when.equals(stepType)) {
            return readWhen(parent, node);
        } else if (XProcConstants.p_otherwise.equals(stepType)) {
            return readOtherwise(parent, node);
        } else if (XProcConstants.p_group.equals(stepType)) {
            return readGroup(parent, node);
        } else if (XProcConstants.p_try.equals(stepType)) {
            return readTry(parent, node);
        } else if (XProcConstants.p_catch.equals(stepType)) {
            return readCatch(parent, node);
        } else if (XProcConstants.cx_until_unchanged.equals(stepType)) {
            return readUntilUnchanged(parent, node);
        }

        DeclareStep decl= null;
        if (parent == null) {
            decl = runtime.getBuiltinDeclaration(stepType);
        } else {
            decl = ((DeclareStep) parent).getStepDeclaration(stepType);
        }
        
        /*
        if (declStack.isEmpty()) {
            decl = runtime.getBuiltinDeclaration(stepType);
        } else {
            try {
                decl = declStack.peek().getStepDeclaration(stepType);
            } catch (Exception ex) {
                throw new XProcException(node, ex.getMessage(), ex);
            }
        }
        */

        if (decl == null) {
            throw XProcException.staticError(44, node, "Not a step: " + stepType);
        }

        // Must be an atomic step in a subpipeline
        checkAttributes(node, new String[] { "name" }, true);

        String stepName = checkNCName(node.getAttributeValue(new QName("name")));

        Step step = new Step(runtime, node, stepType, stepName);
        step.setDeclaration(decl);
        step.parent = parent;

        boolean pStep = XProcConstants.NS_XPROC.equals(node.getNodeName().getNamespaceURI());

        // Store extension attributes and convert any option shortcut attributes into options
        for (XdmNode attr : new RelevantNodes(runtime, node, Axis.ATTRIBUTE)) {
            QName aname = attr.getNodeName();

            if ((pStep && aname.equals(_use_when))
                || (!pStep && aname.equals(p_use_when))) {
                continue;
            }
            
            if (XMLConstants.NULL_NS_URI.equals(aname.getNamespaceURI())) {
                if (!"name".equals(aname.getLocalName())) {
                    Option option = new Option(runtime, node);
                    option.setName(new QName("", aname.getLocalName()));
                    option.setSelect("'" + attr.getStringValue().replace("'", "''") + "'");
                    option.addNamespaceBinding(new NamespaceBinding(step.getXProc(),node));

                    step.addOption(option);
                }
            } else {
                step.addExtensionAttribute(attr);
            }
        }

        Vector<XdmNode> rest = readSignature(step);
        if (rest != null) {
            String message = "A " + stepType + " step must contain only a signature.";
            if (XProcConstants.p_option.equals(rest.get(0).getNodeName())) {
                message += " p:option is not allowed, did you mean p:with-option instead?";
            } else if (XProcConstants.p_parameter.equals(rest.get(0).getNodeName())) {
                message += " p:parameter is not allowed, did you mean p:with-param instead?";
            } else {
                message += " " + rest.get(0).getNodeName() + " not allowed.";
            }
            throw XProcException.staticError(44, rest.get(0), message);
        }

        return step;
    }

    private DeclareStep readDeclareStep(XdmNode node) {
        QName name = node.getNodeName();

        if (!name.equals(XProcConstants.p_declare_step) && !name.equals(XProcConstants.p_pipeline)) {
            throw XProcException.staticError(59, node, "Expected p:declare-step or p:pipeline, got " + name);
        }

        checkAttributes(node, new String[] { "type", "name", "version", "psvi-required", "xpath-version", "exclude-inline-prefixes"}, false);

        String stepName = checkNCName(node.getAttributeValue(_name));
        String typeName = node.getAttributeValue(_type);

        QName type = null;
        if (typeName == null) {
            type = TypeUtils.generateUniqueType();
        } else {
            if (typeName.indexOf(":") <= 0) {
                throw XProcException.staticError(25, node, "Type must be in a namespace.");
            }
            type = new QName(typeName, node);

            if (!loadingStandardLibrary && XProcConstants.NS_XPROC.equals(type.getNamespaceURI())) {
                throw XProcException.staticError(25, node, "Type cannot be in the p: namespace.");
            }
        }

        if (XProcConstants.NS_XPROC.equals(type.getNamespaceURI())) {
            // If declStack is empty, then this is ok.
            if (!declStack.isEmpty()) {
                throw XProcException.staticError(25, node, "Additional steps must not be declared in the XProc namespace.");
            }
        }

        DeclareStep step = new DeclareStep(runtime, node, stepName);
        step.setVersion(inheritedVersion(node));

        boolean psviRequired = booleanAttr(node.getAttributeValue(new QName("psvi-required")));
        String xpathVersion = node.getAttributeValue(new QName("xpath-version"));

        if ("1.0".equals(xpathVersion)) {
            // FIXME: Warn about v2!
        } else if (xpathVersion != null && !"2.0".equals(xpathVersion)) {
            throw XProcException.dynamicError(27, node, "XPath version must be 1.0 or 2.0.");
        }

        // Store extension attributes and convert any option shortcut attributes into options
        for (XdmNode attr : new RelevantNodes(runtime, node, Axis.ATTRIBUTE)) {
            QName aname = attr.getNodeName();
            if (XMLConstants.NULL_NS_URI.equals(aname.getNamespaceURI())) {
                if (!"type".equals(aname.getLocalName()) && !"name".equals(aname.getLocalName())
                        && !"version".equals(aname.getLocalName()) && !"use-when".equals(aname.getLocalName())
                        && !"psvi-required".equals(aname.getLocalName()) && !"xpath-version".equals(aname.getLocalName())
                        && !"exclude-inline-prefixes".equals(aname.getLocalName())) {
                    throw XProcException.staticError(44, node, "Attribute not allowed: " + aname.getLocalName());
                }
            } else {
                step.addExtensionAttribute(attr);
            }
        }

        step.setDeclaredType(type);
        step.setPsviRequired(psviRequired);
        step.setXPathVersion(xpathVersion);

        HashSet<String> excludeURIs = S9apiUtils.excludeInlinePrefixes(node, node.getAttributeValue(_exclude_inline_prefixes));
        if (!declStack.isEmpty()) {
            DeclareStep parent = declStack.peek();
            for (String uri : parent.getExcludeInlineNamespaces()) {
                excludeURIs.add(uri);
            }
        }
        // Special case: if your parent is a p:library, get the exclusions from there too...
        if (node.getParent() != null) {
            XdmNode parent = node.getParent();
            if (XProcConstants.p_library.equals(parent.getNodeName()) && parent.getAttributeValue(_exclude_inline_prefixes) != null) {
                HashSet<String> pexcl = S9apiUtils.excludeInlinePrefixes(parent, parent.getAttributeValue(_exclude_inline_prefixes));
                for (String uri : pexcl) {
                    excludeURIs.add(uri);
                }
            }
        }

        step.setExcludeInlineNamespaces(excludeURIs);

        if (name.equals(XProcConstants.p_pipeline)) {
            Input input = new Input(runtime, node);
            input.setPort("source");
            input.setPrimary(true);
            input.setSequence(false);
            step.addInput(input);

            input = new Input(runtime, node);
            input.setPort("parameters");
            input.setParameterInput(true);
            input.setPrimary(true);
            input.setSequence(true);
            step.addInput(input);

            Output output = new Output(runtime, node);
            output.setPort("result");
            output.setPrimary(true);
            output.setSequence(false);
            step.addOutput(output);
        }

        Vector<XdmNode> rest = readSignature(step);
        step.setAtomic(rest == null);

        if (declStack.isEmpty()) {
            runtime.declareStep(step.getDeclaredType(), step);
        } else {
            declStack.peek().declareStep(step.getDeclaredType(), step);
        }

        if (!declStack.isEmpty()) {
            step.setParentDecl(declStack.peek());
        }
        
        declStack.push(step);

        // Check that we have legitimate bindings
        for (Input input : step.inputs()) {
            if (step.isAtomic()) {
                if (input.getBinding().size() != 0) {
                    runtime.error(null,input.getNode(),"Input bindings are not allowed on an atomic step",XProcConstants.staticError(42));
                }
            } else {
                if (!input.getPort().startsWith("|")) {
                    for (Binding binding : input.getBinding()) {
                        if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                            runtime.error(null,input.getNode(),"Default input bindings cannot use p:pipe",XProcConstants.staticError(44));
                        }
                    }
                }
            }
        }

        // No output bindings are allowed!
        for (Output output : step.outputs()) {
            Input input = step.getInput("|" + output.getPort());
            if (step.isAtomic() && input != null) {
                runtime.error(null,output.getNode(),"Output bindings are not allowed on an atomic step",XProcConstants.staticError(29));
            }
        }

        Vector<XdmNode> steps = new Vector<XdmNode>();

        if (rest != null) {
            for (XdmNode substepNode : rest) {
                if (XProcConstants.p_variable.equals(substepNode.getNodeName())) {
                    Variable var = readVariable(step, substepNode);
                    step.addVariable(var);
                } else {
                    if ((XProcConstants.p_declare_step.equals(substepNode.getNodeName()))
                            || XProcConstants.p_pipeline.equals(substepNode.getNodeName())) {
                        DeclareStep dstep = (DeclareStep) readStep(step, substepNode);
                        // It's not really part of the pipeline, but we need to parse it
                        // to make sure it gets added to the available steps
                    } else if (XProcConstants.p_import.equals(substepNode.getNodeName())) {
                        Import importElem = (Import) readStep(step, substepNode);
                        XdmNode root = importElem.getRoot();
                        // root will be null if the library has already been imported
                        if (root != null) {
                            importElem.setLibrary(readLibrary(step, root));
                        }
                    } else {
                        steps.add(substepNode);
                    }
                }
            }

            step.checkPrimaryIO();
            rest = steps;
        }

        step.setXmlContent(rest);

        declStack.pop();

        return step;
    }

    private void parseDeclareStepBodyPassTwo(DeclareStep step) {
        step.setBodyParsed(true);

        for (DeclareStep substep : step.getStepDeclarations()) {
            parseDeclareStepBodyPassTwo(substep);
        }
        
        Vector<XdmNode> rest = step.getXmlContent();

        if (rest != null) {
            for (XdmNode substepNode : rest) {
                Step substep = readStep(step, substepNode);
                step.addStep(substep);
            }
        }
    }

    private Double inheritedVersion(XdmNode node) {
        XdmNode parent = node.getParent();

        if (XProcConstants.p_declare_step.equals(node.getNodeName())
                || XProcConstants.p_pipeline.equals(node.getNodeName())
                || XProcConstants.p_library.equals(node.getNodeName())) {
            String version = node.getAttributeValue(_version);
            if (version != null) {
                TypeUtils.checkType(runtime, version, XProcConstants.xs_decimal, node, err_XS0063);
                return Double.parseDouble(version);
            }
        }

        if (parent == null) {
            throw XProcException.staticError(62, node, "Version attribute is required.");
        } else {
            return inheritedVersion(parent);
        }
    }

    private Import readImport(XdmNode node) {
        checkAttributes(node, new String[] { "href" }, false);

        String href = node.getAttributeValue(_href);
        URI importURI = node.getBaseURI().resolve(href);

        XdmNode child = null;
        for (XdmNode cnode : new RelevantNodes(runtime, node, Axis.CHILD)) {
            child = cnode;
        }

        if (child != null) {
            throw new UnsupportedOperationException("p:import must be empty.");
        }

        Import importElem = new Import(runtime, node);
        importElem.setHref(importURI);

        XdmNode doc;
        try {
            if (importURI.toASCIIString().equals(XProcConstants.CALABASH_EXTENSION_LIBRARY_1_0)) {
                doc = loadExtensionLibrary();
            } else {
                doc = runtime.parse(href, node.getBaseURI().toASCIIString());
            }
        } catch (XProcException xe) {
            if (XProcConstants.dynamicError(11).equals(xe.getErrorCode())) {
                throw XProcException.staticError(52, node, xe.getCause(), "Cannot import: " + importURI.toASCIIString());
            } else {
                throw xe;
            }
        } catch (Exception e) {
            throw new XProcException(e);
        }

        //System.err.println("BASE: " + doc.getBaseURI());
        importURI = doc.getBaseURI();

        XdmNode root = S9apiUtils.getDocumentElement(doc);

        boolean imported = topLevelImports.contains(importURI.toASCIIString());
        if (!imported && !declStack.isEmpty()) {
            imported = declStack.peek().imported(importURI.toASCIIString());
        }

        if (imported) {
            return importElem;
        }
        
        if (!declStack.isEmpty()) {
            declStack.peek().addImport(importURI.toASCIIString());
        } else {
            topLevelImports.add(importURI.toASCIIString());
        }

        importElem.setRoot(root);
        checkExtensionAttributes(node, importElem);

        return importElem;
    }

    private ForEach readForEach(Step parent, XdmNode node) {
        QName name = node.getNodeName();
        if (!XProcConstants.p_for_each.equals(name)) {
            throw new UnsupportedOperationException("Can't parse " + name + " as a pipeline!");
        }

        checkAttributes(node, new String[] { "name" }, false);

        String stepName = checkNCName(node.getAttributeValue(_name));

        ForEach step = new ForEach(runtime, node, stepName);
        checkExtensionAttributes(node, step);
        // FIXME: Do I really need parentDecl and parent?
        step.setParentDecl((DeclareStep) parent);
        step.parent = parent;

        Vector<XdmNode> rest = readSignature(step);

        if (rest == null) {
            throw XProcException.staticError(15, node, "A p:for-each must contain a subpipeline.");
        }

        for (XdmNode substepNode : rest) {
            Step substep = readStep(step, substepNode);
            step.addStep(substep);
        }

        step.checkPrimaryIO();
        return step;
    }


    private UntilUnchanged readUntilUnchanged(Step parent, XdmNode node) {
        QName name = node.getNodeName();
        if (!XProcConstants.cx_until_unchanged.equals(name)) {
            throw new UnsupportedOperationException("Can't parse " + name + " as a cx:until-unchanged!");
        }

        checkAttributes(node, new String[] { "name" }, false);

        String stepName = checkNCName(node.getAttributeValue(_name));

        UntilUnchanged step = new UntilUnchanged(runtime, node, stepName);
        checkExtensionAttributes(node, step);
        step.setParentDecl((DeclareStep) parent);
        step.parent = parent;

        Vector<XdmNode> rest = readSignature(step);

        if (rest == null) {
            throw XProcException.staticError(15, node, "A cx:until-unchanged must contain a subpipeline.");
        }

        for (XdmNode substepNode : rest) {
            Step substep = readStep(step, substepNode);
            step.addStep(substep);
        }

        step.checkPrimaryIO();
        return step;
    }

    private Viewport readViewport(Step parent, XdmNode node) {
        QName name = node.getNodeName();
        if (!XProcConstants.p_viewport.equals(name)) {
            throw new UnsupportedOperationException("Can't parse " + name + " as a pipeline!");
        }

        checkAttributes(node, new String[] { "name", "match" }, false);

        String stepName = checkNCName(node.getAttributeValue(_name));
        RuntimeValue match = new RuntimeValue(node.getAttributeValue(new QName("match")), node);

        Viewport step = new Viewport(runtime, node, stepName);
        checkExtensionAttributes(node, step);
        step.setParentDecl((DeclareStep) parent);
        step.parent = parent;

        step.setMatch(match);

        Vector<XdmNode> rest = readSignature(step);

        if (rest == null) {
            throw XProcException.staticError(15, node, "A p:viewport must contain a subpipeline.");
        }

        for (XdmNode substepNode : rest) {
            Step substep = readStep(step, substepNode);
            step.addStep(substep);
        }

        step.checkPrimaryIO();
        return step;
    }

    private Choose readChoose(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "name" }, false);
        String stepName = checkNCName(node.getAttributeValue(_name));

        Choose step = new Choose(runtime, node, stepName);
        checkExtensionAttributes(node, step);
        step.setParentDecl((DeclareStep) parent);
        step.parent = parent;

        Vector<XdmNode> rest = readSignature(step);

        if (rest == null) {
            throw XProcException.staticError(15, node, "A p:choose must contain at least one p:when.");
        }

        for (XdmNode child : rest) {
            if (XProcConstants.p_when.equals(child.getNodeName())) {
                When substep = readWhen(step, child);
                step.addStep(substep);
            } else if (XProcConstants.p_otherwise.equals(child.getNodeName())) {
                Otherwise substep = readOtherwise(step, child);
                step.addStep(substep);
            } else {
                throw new UnsupportedOperationException("Not valid in a choose: " + child.getNodeName());
            }
        }

        step.checkPrimaryIO();
        return step;
    }

    private When readWhen(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "test" }, false);

        String stepName = checkNCName(node.getAttributeValue(px_name));
        String testExpr = node.getAttributeValue(new QName("test"));

        When step = new When(runtime, node, stepName);
        checkExtensionAttributes(node, step);
        step.setTest(testExpr);
        step.setParentDecl((DeclareStep) parent);
        step.parent = parent;

        Vector<XdmNode> rest = readSignature(step);

        if (rest == null) {
            throw XProcException.staticError(15, node, "A p:when must contain a subpipeline.");
        }

        for (XdmNode substepNode : rest) {
            Step substep = readStep(step, substepNode);
            step.addStep(substep);
        }

        step.checkPrimaryIO();
        return step;
    }

    private Otherwise readOtherwise(Step parent, XdmNode node) {
        checkAttributes(node, null, false);

        String stepName = checkNCName(node.getAttributeValue(px_name));

        Otherwise step = new Otherwise(runtime, node, stepName);
        checkExtensionAttributes(node, step);
        step.setParentDecl((DeclareStep) parent);
        step.parent = parent;

        Vector<XdmNode> rest = readSignature(step);

        if (rest == null) {
            throw XProcException.staticError(15, node, "A p:otherwise must contain a subpipeline.");
        }

        for (XdmNode substepNode : rest) {
            Step substep = readStep(step, substepNode);
            step.addStep(substep);
        }

        step.checkPrimaryIO();
        return step;
    }

    private Group readGroup(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "name" }, false);

        String stepName = checkNCName(node.getAttributeValue(_name));

        Group step = new Group(runtime, node, stepName);
        checkExtensionAttributes(node, step);
        step.setParentDecl((DeclareStep) parent);
        step.parent = parent;

        Vector<XdmNode> rest = readSignature(step);

        if (rest == null) {
            throw XProcException.staticError(15, node, "A p:group must contain a subpipeline.");
        }

        for (XdmNode substepNode : rest) {
            Step substep = readStep(step, substepNode);
            step.addStep(substep);
        }

        step.checkPrimaryIO();
        return step;
    }

    private Try readTry(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "name" }, false);

        String stepName = checkNCName(node.getAttributeValue(_name));

        Try step = new Try(runtime, node, stepName);
        checkExtensionAttributes(node, step);
        step.setParentDecl((DeclareStep) parent);
        step.parent = parent;

        Vector<XdmNode> rest = readSignature(step);

        if (rest == null) {
            throw XProcException.staticError(15, node, "A p:try must contain a subpipeline.");
        }

        for (XdmNode substepNode : rest) {
            Step substep = readStep(step, substepNode);
            step.addStep(substep);
        }

        step.checkPrimaryIO();
        return step;
    }

    private Catch readCatch(Step parent, XdmNode node) {
        checkAttributes(node, new String[] { "name" }, false);

        String stepName = checkNCName(node.getAttributeValue(_name));

        Catch step = new Catch(runtime, node, stepName);
        checkExtensionAttributes(node, step);
        step.setParentDecl((DeclareStep) parent);
        step.parent = parent;

        Vector<XdmNode> rest = readSignature(step);

        Input input = new Input(runtime, step.getNode());
        input.setPort("error");
        input.addBinding(new ErrorBinding(runtime, step.getNode()));
        input.setPrimary(false);
        input.setSequence(true);
        step.addInput(input);

        if (rest == null) {
            throw XProcException.staticError(15, node, "A p:catch must contain a subpipeline.");
        }

        for (XdmNode substepNode : rest) {
            Step substep = readStep(step, substepNode);
            step.addStep(substep);
        }

        step.checkPrimaryIO();

        return step;
    }

    // ================================================================================================
    
    private HashSet<String> checkAttributes(XdmNode node, String[] attrs, boolean optionShortcutsOk) {
        HashSet<String> hash = null;
        if (attrs != null) {
            hash = new HashSet<String> ();
            for (String attr : attrs) {
                hash.add(attr);
            }
        }
        HashSet<String> options = null;

        Double version = inheritedVersion(node);

        for (XdmNode attr : new RelevantNodes(runtime, node, Axis.ATTRIBUTE)) {
            QName aname = attr.getNodeName();

            // The use-when attribute is always ok
            if ((XProcConstants.NS_XPROC.equals(node.getNodeName().getNamespaceURI())
                    && aname.equals(_use_when))
                || (!XProcConstants.NS_XPROC.equals(node.getNodeName().getNamespaceURI())
                    && aname.equals(p_use_when))) {
                continue;
            }

            if ("".equals(aname.getNamespaceURI())) {
                if (hash.contains(aname.getLocalName())) {
                // ok
                } else if (optionShortcutsOk) {
                    if (options == null) {
                        options = new HashSet<String> ();
                    }
                    options.add(aname.getLocalName());
                } else if (version > 1.0) {
                    // Ok, then, we'll just ignore it...
                } else {
                    runtime.error(null, node, "Attribute \"" + aname + "\" not allowed on " + node.getNodeName(), XProcConstants.staticError(8));
                }
            } else if (XProcConstants.NS_XPROC.equals(aname.getNamespaceURI())) {
                runtime.error(null, node, "Attribute \"" + aname + "\" not allowed on " + node.getNodeName(), XProcConstants.staticError(8));
                return null;
            }
            // Everything else is ok
        }
        
        return options;
    }

    // Can't be used for steps because it doesn't handle option shortcut attributes
    private void checkExtensionAttributes(XdmNode node, SourceArtifact src) {
        for (XdmNode attr : new RelevantNodes(runtime, node, Axis.ATTRIBUTE)) {
            QName aname = attr.getNodeName();
            if ("".equals(aname.getNamespaceURI())) {
                // nop
            } else if (XProcConstants.NS_XPROC.equals(aname.getNamespaceURI())) {
                runtime.error(null, node, "Attribute \"" + aname + "\" not allowed on " + node.getNodeName(), XProcConstants.staticError(8));
            } else {
                src.addExtensionAttribute(attr);
            }
        }
    }

    private boolean booleanAttr(String value) {
        return booleanAttr(value, false);
    }

    private boolean booleanAttr(String value, boolean defval) {
        if (value == null) {
            return defval;
        }
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("Boolean value must be 'true' or 'false'.");
    }

    private String checkNCName(String name) {
        if (name != null) {
            try {
                TypeUtils.checkType(runtime, name, XProcConstants.xs_NCName,null);
            } catch (XProcException xe) {
                throw new XProcException("Invalid name: \"" + name + "\". Step and port names must be NCNames.", xe.getCause());
            }
        }
        return name;
    }
}
