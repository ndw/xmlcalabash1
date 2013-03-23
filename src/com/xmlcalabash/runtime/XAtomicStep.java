package com.xmlcalabash.runtime;

import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TypeUtils;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcStep;
import com.xmlcalabash.core.XProcData;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadableInline;
import com.xmlcalabash.io.ReadableDocument;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Binding;
import com.xmlcalabash.model.PipeNameBinding;
import com.xmlcalabash.model.InlineBinding;
import com.xmlcalabash.model.DocumentBinding;
import com.xmlcalabash.model.DataBinding;
import com.xmlcalabash.model.Input;
import com.xmlcalabash.model.Output;
import com.xmlcalabash.model.Parameter;
import com.xmlcalabash.model.ComputableValue;
import com.xmlcalabash.model.NamespaceBinding;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.Option;
import net.sf.saxon.om.InscopeNamespaceResolver;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.SaxonApiUncheckedException;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.HashSet;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 5:25:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class XAtomicStep extends XStep {
    private final static QName _name = new QName("", "name");
    private final static QName _namespace = new QName("", "namespace");
    private final static QName _value = new QName("", "value");
    private final static QName _type = new QName("", "type");
    private final static QName cx_item = new QName("cx", XProcConstants.NS_CALABASH_EX, "item");

    protected Hashtable<String, Vector<ReadablePipe>> inputs = new Hashtable<String, Vector<ReadablePipe>> ();
    protected Hashtable<String, WritablePipe> outputs = new Hashtable<String, WritablePipe> ();

    public XAtomicStep(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step);
        this.parent = parent;
    }

    public XCompoundStep getParent() {
        return parent;
    }

    public boolean hasReadablePipes(String port) {
        if (inputs.containsKey(port)) {
            return inputs.get(port).size() > 0;
        } else {
            return false;
        }
    }

    public boolean hasWriteablePipe(String port) {
        return outputs.containsKey(port);
    }

    public RuntimeValue optionAvailable(QName optName) {
        if (!inScopeOptions.containsKey(optName)) {
            return null;
        }
        return inScopeOptions.get(optName);
    }

    protected ReadablePipe getPipeFromBinding(Binding binding) {
        ReadablePipe pipe = null;
        if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
            PipeNameBinding pnbinding = (PipeNameBinding) binding;

            // Special case, if we're in a compound step (e.g., if we're getting a
            // binding for a variable in a pipeline), then we can read from ourself.
            XCompoundStep start = parent;
            if (this instanceof XCompoundStep) {
                start = (XCompoundStep) this;
            }

            pipe = start.getBinding(pnbinding.getStep(), pnbinding.getPort());
        } else if (binding.getBindingType() == Binding.INLINE_BINDING) {
            InlineBinding ibinding = (InlineBinding) binding;
            pipe = new ReadableInline(runtime, ibinding.nodes(), ibinding.getExcludedNamespaces());
        } else if (binding.getBindingType() == Binding.EMPTY_BINDING) {
            pipe = new ReadableDocument(runtime);
        } else if (binding.getBindingType() == Binding.DOCUMENT_BINDING) {
            DocumentBinding dbinding = (DocumentBinding) binding;
            pipe = runtime.getConfigurer().getXMLCalabashConfigurer().makeReadableDocument(runtime, dbinding);
        } else if (binding.getBindingType() == Binding.DATA_BINDING) {
            DataBinding dbinding = (DataBinding) binding;
            pipe = runtime.getConfigurer().getXMLCalabashConfigurer().makeReadableData(runtime, dbinding);
        } else if (binding.getBindingType() == Binding.ERROR_BINDING) {
            XCompoundStep step = parent;
            while (! (step instanceof XCatch)) {
                step = step.getParent();
            }
            pipe = step.getBinding(step.getName(), "error");
        } else {
            throw new XProcException(binding.getNode(), "Unknown binding type: " + binding.getBindingType());
        }

        pipe.setReader(step);
        return pipe;
    }

    protected void instantiateReaders(Step step) {
        for (Input input : step.inputs()) {
            String port = input.getPort();
            if (!port.startsWith("|")) {
                Vector<ReadablePipe> readers = null;
                if (inputs.containsKey(port)) {
                    readers = inputs.get(port);
                } else {
                    readers = new Vector<ReadablePipe> ();
                    inputs.put(port, readers);
                }
                for (Binding binding : input.getBinding()) {
                    ReadablePipe pipe = getPipeFromBinding(binding);
                    pipe.canReadSequence(input.getSequence());

                    if (input.getSelect() != null) {
                        finest(step.getNode(), step.getName() + " selects from " + pipe + " for " + port);
                        pipe = new XSelect(runtime, this, pipe, input.getSelect(), input.getNode());
                    }

                    readers.add(pipe);
                    finest(step.getNode(), step.getName() + " reads from " + pipe + " for " + port);
                }

                XInput xinput = new XInput(runtime, input);
                addInput(xinput);
            }
        }
    }

    public void instantiate(Step step) {
        instantiateReaders(step);

        for (Output output : step.outputs()) {
            String port = output.getPort();
            XOutput xoutput = new XOutput(runtime, output);
            xoutput.setLogger(step.getLog(port));
            addOutput(xoutput);
            WritablePipe wpipe = xoutput.getWriter();
            wpipe.canWriteSequence(output.getSequence());
            outputs.put(port, wpipe);
            finest(step.getNode(), step.getName() + " writes to " + wpipe + " for " + port);
        }

        parent.addStep(this);
    }

    protected void computeParameters(XProcStep xstep) throws SaxonApiException {
        // N.B. At this time, there are no compound steps that accept parameters or options,
        // so the order in which we calculate them doesn't matter. That will change if/when
        // there are such compound steps.

        // Loop through all the with-params and parameter input ports, adding
        // them to the xstep..

        // First, are there any parameters at all?
        Vector<String> paramPorts = new Vector<String> ();
        boolean primaryParamPort = false;
        for (Input input : step.inputs()) {
            if (input.getParameterInput()) {
                primaryParamPort = primaryParamPort | input.getPrimary();
                paramPorts.add(input.getPort());
            }
        }

        int position = 0;
        boolean loopdone = false;
        while (!loopdone) {
            position++;
            loopdone = true;

            for (Parameter p : step.parameters()) {
                if (XProcConstants.NS_XPROC.equals(p.getName().getNamespaceURI())) {
                    throw XProcException.dynamicError(31);
                }

                loopdone = (p.getPosition() <= position);
                if (p.getPosition() == position) {
                    loopdone = false;
                    if (!primaryParamPort) {
                        String port = p.getPort();
                        if (port == null) {
                            throw XProcException.staticError(34, step.getNode(), "No parameter input port.");
                        }
                        xstep.setParameter(p.getPort(), p.getName(), computeValue(p));
                    } else {
                        xstep.setParameter(p.getName(), computeValue(p));
                    }
                }
            }

            for (String port : paramPorts) {
                Input input = step.getInput(port);
                loopdone = loopdone && (input.getPosition() <= position);
                if (input.getPosition() == position) {
                    for (ReadablePipe source : inputs.get(port)) {
                        while (source.moreDocuments()) {
                            XdmNode node = source.read();
                            XdmNode docelem = S9apiUtils.getDocumentElement(node);

                            if (XProcConstants.c_param_set.equals(docelem.getNodeName())) {
                                // Check the attributes...
                                for (XdmNode attr : new RelevantNodes(runtime, docelem, Axis.ATTRIBUTE)) {
                                    QName aname = attr.getNodeName();
                                    if ("".equals(aname.getNamespaceURI())
                                        || XProcConstants.NS_XPROC.equals(aname.getNamespaceURI())) {
                                        throw XProcException.dynamicError(14, step.getNode(), "Attribute not allowed");
                                    }
                                }

                                for (XdmNode child : new RelevantNodes(runtime, docelem, Axis.CHILD)) {
                                    if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                                        if (!child.getNodeName().equals(XProcConstants.c_param)) {
                                            throw XProcException.dynamicError(18, step.getNode(), "Element not allowed: " + child.getNodeName());
                                        }
                                        parseParameterNode(xstep,child);
                                    }
                                }
                            } else if (XProcConstants.c_param.equals(docelem.getNodeName())) {
                                parseParameterNode(xstep,docelem);
                            } else {
                                throw new XProcException(step.getNode(), docelem.getNodeName() + " found where c:param or c:param-set expected");
                            }
                        }
                    }
                }
            }
        }
    }

    public void reset() {
        for (String port : inputs.keySet()) {
            for (ReadablePipe rpipe : inputs.get(port)) {
                rpipe.resetReader();
            }
        }

        for (String port : outputs.keySet()) {
            WritablePipe wpipe = outputs.get(port);
            wpipe.resetWriter();
        }

        clearOptions();

        clearParameters();
    }

    public void run() throws SaxonApiException {
        XProcStep xstep = runtime.getConfiguration().newStep(runtime, this);

        // If there's more than one reader, collapse them all into a single reader
        for (String port : inputs.keySet()) {
            int totalDocs = 0; // FIXME: this will be more complicated when multiple threads are involved
            Input input = step.getInput(port);
            if (!input.getParameterInput()) {
                int readerCount = inputs.get(port).size();
                if (readerCount > 1) {
                    Pipe pipe = new Pipe(runtime);
                    pipe.setWriter(step);
                    pipe.setReader(step);
                    pipe.canWriteSequence(true);
                    pipe.canReadSequence(input.getSequence());
                    for (ReadablePipe reader : inputs.get(port)) {
                        if (reader.moreDocuments()) {
                            while (reader.moreDocuments()) {
                                XdmNode doc = reader.read();
                                pipe.write(doc);
                                totalDocs++;
                            }
                        } else if (reader instanceof ReadableDocument) {
                            // HACK: We haven't necessarily read the document yet
                            totalDocs++;
                        }
                    }
                    xstep.setInput(port, pipe);
                } else if (readerCount == 1) {
                    ReadablePipe pipe = inputs.get(port).firstElement();
                    pipe.setReader(step);
                    if (pipe.moreDocuments()) {
                        totalDocs += pipe.documentCount();
                    } else if (pipe instanceof ReadableDocument) {
                        totalDocs++;
                    }
                    xstep.setInput(port, pipe);
                }
            }

            if (totalDocs != 1 && !input.getSequence()) {
                throw XProcException.dynamicError(6, step.getNode(), totalDocs + " documents appear on the '" + port + "' port.");
            }
        }

        for (String port : outputs.keySet()) {
            xstep.setOutput(port, outputs.get(port));
        }

        // N.B. At this time, there are no compound steps that accept parameters or options,
        // so the order in which we calculate them doesn't matter. That will change if/when
        // there are such compound steps.

        // Calculate all the options
        DeclareStep decl = step.getDeclaration();
        inScopeOptions = parent.getInScopeOptions();
        for (QName name : step.getOptions()) {
            Option option = step.getOption(name);
            RuntimeValue value = computeValue(option);

            Option optionDecl = decl.getOption(name);
            String typeName = optionDecl.getType();
            XdmNode declNode = optionDecl.getNode();
            if (typeName != null && declNode != null) {
                if (typeName.contains("|")) {
                    TypeUtils.checkLiteral(value.getString(), typeName);
                } else {
                    QName type = new QName(typeName, declNode);
                    TypeUtils.checkType(runtime, value.getString(),type,option.getNode());
                }
            }

            xstep.setOption(name, value);
            inScopeOptions.put(name, value);
        }

        xstep.reset();
        computeParameters(xstep);

        // HACK HACK HACK!
        if (XProcConstants.p_in_scope_names.equals(step.getType())) {
            for (QName name : inScopeOptions.keySet()) {
                xstep.setParameter(name, inScopeOptions.get(name));
            }
        }
        
        // Make sure we do this *after* calculating any option/parameter values...
        XProcData data = runtime.getXProcData();
        data.openFrame(this);

        runtime.start(this);
        xstep.run();
        runtime.finish(this);

        // FIXME: Is it sufficient to only do this for atomic steps?
        String cache = getInheritedExtensionAttribute(XProcConstants.cx_cache);
        if ("true".equals(cache)) {
            for (String port : outputs.keySet()) {
                WritablePipe wpipe = outputs.get(port);
                // FIXME: Hack. There should be a better way...
                if (wpipe instanceof Pipe) {
                    ReadablePipe rpipe = new Pipe(runtime, ((Pipe) wpipe).documents());
                    rpipe.canReadSequence(true);
                    rpipe.setReader(step);
                    while (rpipe.moreDocuments()) {
                        XdmNode doc = rpipe.read();
                        runtime.cache(doc, step.getNode().getBaseURI());
                    }
                }
            }
        } else if (!"false".equals(cache) && cache != null) {
            throw XProcException.dynamicError(19);
        }

        for (String port : outputs.keySet()) {
            WritablePipe wpipe = outputs.get(port);
            wpipe.close(); // Indicate we're done
        }

        data.closeFrame();
    }

    public void reportError(XdmNode doc) {
        parent.reportError(doc);
    }

    private void parseParameterNode(XProcStep impl, XdmNode pnode) {
        String value = pnode.getAttributeValue(_value);

        if (value == null && runtime.getAllowGeneralExpressions()) {
            parseParameterValueNode(impl, pnode);
            return;
        }

        Parameter p = new Parameter(step.getXProc(),pnode);
        String port = p.getPort();
        String name = pnode.getAttributeValue(_name);
        String ns = pnode.getAttributeValue(_namespace);

        QName pname = null;
        if (ns == null) {
            pname = new QName(name,pnode);
        } else {
            int pos = name.indexOf(":");
            if (pos > 0) {
                name = name.substring(pos);

                QName testNode = new QName(name,pnode);
                if (!ns.equals(testNode.getNamespaceURI())) {
                    throw XProcException.dynamicError(25);
                }

            }
            pname = new QName(ns,name);
        }

        if (XProcConstants.NS_XPROC.equals(pname.getNamespaceURI())) {
            throw XProcException.dynamicError(31);
        }

        p.setName(pname);

        for (XdmNode attr : new RelevantNodes(runtime, pnode, Axis.ATTRIBUTE)) {
            QName aname = attr.getNodeName();
            if ("".equals(aname.getNamespaceURI())) {
                if (!aname.equals(_name) && !aname.equals(_namespace) && !aname.equals(_value)) {
                    throw XProcException.dynamicError(14);
                }
            }
        }

        if (port != null) {
            impl.setParameter(port,pname,new RuntimeValue(value,pnode));
        } else {
            impl.setParameter(pname,new RuntimeValue(value,pnode));
        }
    }

    private void parseParameterValueNode(XProcStep impl, XdmNode pnode) {
        Parameter p = new Parameter(step.getXProc(),pnode);
        String port = p.getPort();
        String name = pnode.getAttributeValue(_name);
        String ns = pnode.getAttributeValue(_namespace);

        QName pname = null;
        if (ns == null) {
            pname = new QName(name,pnode);
        } else {
            int pos = name.indexOf(":");
            if (pos > 0) {
                name = name.substring(pos);

                QName testNode = new QName(name,pnode);
                if (!ns.equals(testNode.getNamespaceURI())) {
                    throw XProcException.dynamicError(25);
                }

            }
            pname = new QName(ns,name);
        }

        p.setName(pname);

        for (XdmNode attr : new RelevantNodes(runtime, pnode, Axis.ATTRIBUTE)) {
            QName aname = attr.getNodeName();
            if ("".equals(aname.getNamespaceURI())) {
                if (!aname.equals(_name) && !aname.equals(_namespace)) {
                    throw XProcException.dynamicError(14);
                }
            }
        }

        String stringValue = "";
        Vector<XdmItem> items = new Vector<XdmItem> ();
        for (XdmNode child : new RelevantNodes(runtime, pnode, Axis.CHILD)) {
            if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
               if (!child.getNodeName().equals(cx_item)) {
                    throw XProcException.dynamicError(18, step.getNode(), "Element not allowed: " + child.getNodeName());
                }

                String type = child.getAttributeValue(_type);
                if (type == null) {
                    Vector<XdmValue> nodes = new Vector<XdmValue> ();
                    URI baseURI = null;

                    XdmSequenceIterator iter = child.axisIterator(Axis.CHILD);
                    while (iter.hasNext()) {
                        XdmNode gchild = (XdmNode) iter.next();

                        if (baseURI == null && gchild.getNodeKind() == XdmNodeKind.ELEMENT) {
                            baseURI = gchild.getBaseURI();
                        }

                        nodes.add(gchild);
                    }

                    XdmDestination dest = new XdmDestination();

                    try {
                        if (baseURI == null) {
                            baseURI = new URI("http://example.com/"); // FIXME: do I need this?
                        }
                        S9apiUtils.writeXdmValue(runtime.getProcessor(), nodes, dest, baseURI);
                        XdmNode doc = dest.getXdmNode();
                        stringValue += doc.getStringValue();
                        items.add(doc);
                    } catch (URISyntaxException use) {
                        throw new XProcException(use);
                    } catch (SaxonApiException sae) {
                        throw new XProcException(sae);
                    }
                } else {
                    stringValue += child.getStringValue();
                    items.add(new XdmAtomicValue(child.getStringValue()));
                }
            }
        }

        RuntimeValue value = new RuntimeValue(stringValue, items, pnode, new Hashtable<String,String> ());

        if (port != null) {
            impl.setParameter(port,pname,value);
        } else {
            impl.setParameter(pname,value);
        }
    }

    protected RuntimeValue computeValue(ComputableValue var) {
        Hashtable<String,String> nsBindings = new Hashtable<String,String> ();
        Hashtable<QName,RuntimeValue> globals = inScopeOptions;
        XdmNode doc = null;

        try {
            if (var.getBinding().size() > 0) {
                Binding binding = var.getBinding().firstElement();

                ReadablePipe pipe = null;
                if (binding.getBindingType() == Binding.ERROR_BINDING) {
                    pipe = ((XCatch) this).errorPipe;
                } else {
                    pipe = getPipeFromBinding(binding);
                }
                doc = pipe.read();
                if (pipe.moreDocuments()) {
                    throw XProcException.dynamicError(step, 8, "More than one document in context for parameter '" + var.getName() + "'");
                }
            }
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        for (NamespaceBinding nsbinding : var.getNamespaceBindings()) {
            Hashtable<String,String> localBindings = new Hashtable<String,String> ();

            // Compute the namespaces associated with this binding
            if (nsbinding.getBinding() != null) {
                QName binding = new QName(nsbinding.getBinding(), nsbinding.getNode());
                RuntimeValue nsv = globals.get(binding);
                if (nsv == null) {
                    throw new XProcException(var.getNode(), "No in-scope option or variable named: " + binding);
                }

                localBindings = nsv.getNamespaceBindings();
            } else if (nsbinding.getXPath() != null) {
                try {
                    XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
                    xcomp.setBaseURI(step.getNode().getBaseURI());

                    for (QName varname : globals.keySet()) {
                        xcomp.declareVariable(varname);
                    }

                    // Make sure the namespace bindings for evaluating the XPath expr are correct
                    // FIXME: Surely there's a better way to do this?
                    Hashtable<String,String> lclnsBindings = new Hashtable<String, String>();
                    NodeInfo inode = nsbinding.getNode().getUnderlyingNode();
                    NamePool pool = inode.getNamePool();
                    InscopeNamespaceResolver inscopeNS = new InscopeNamespaceResolver(inode);
                    Iterator<String> pfxiter = inscopeNS.iteratePrefixes();
                    while (pfxiter.hasNext()) {
                        String nspfx = pfxiter.next();
                        String nsuri = inscopeNS.getURIForPrefix(nspfx, "".equals(nspfx));
                        lclnsBindings.put(nspfx, nsuri);
                    }

                    for (String prefix : lclnsBindings.keySet()) {
                        xcomp.declareNamespace(prefix, lclnsBindings.get(prefix));
                    }

                    XPathExecutable xexec = xcomp.compile(nsbinding.getXPath());
                    XPathSelector selector = xexec.load();

                    for (QName varname : globals.keySet()) {
                        XdmAtomicValue avalue = new XdmAtomicValue(globals.get(varname).getString());
                        selector.setVariable(varname,avalue);
                    }

                    if (doc != null) {
                        selector.setContextItem(doc);
                    }

                    XdmNode element = null;
                    Iterator<XdmItem> values = selector.iterator();
                    while (values.hasNext()) {
                        XdmItem item = values.next();
                        if (element != null || item.isAtomicValue()) {
                            throw XProcException.dynamicError(9);
                        }
                        element = (XdmNode) item;
                        if (element.getNodeKind() != XdmNodeKind.ELEMENT) {
                            throw XProcException.dynamicError(9);
                        }
                    }

                    if (element == null) {
                        throw XProcException.dynamicError(9);
                    }

                    XdmSequenceIterator nsIter = element.axisIterator(Axis.NAMESPACE);
                    while (nsIter.hasNext()) {
                        XdmNode ns = (XdmNode) nsIter.next();
                        localBindings.put(ns.getNodeName().getLocalName(),ns.getStringValue());
                    }
                } catch (SaxonApiException sae) {
                    throw new XProcException(sae);
                }
            } else if (nsbinding.getNamespaceBindings() != null) {
                localBindings = nsbinding.getNamespaceBindings();
            }

            // Remove the excluded ones
            HashSet<String> prefixes = new HashSet<String> ();
            for (String uri : nsbinding.getExcludedNamespaces()) {
                for (String prefix : localBindings.keySet()) {
                    if (uri.equals(localBindings.get(prefix))) {
                        prefixes.add(prefix);
                    }
                }
            }
            for (String prefix : prefixes) {
                localBindings.remove(prefix);
            }

            // Add them to the bindings for this value, making sure there are no errors...
            for (String pfx : localBindings.keySet()) {
                if (nsBindings.containsKey(pfx) && !nsBindings.get(pfx).equals(localBindings.get(pfx))) {
                    throw XProcException.dynamicError(13);
                }
                nsBindings.put(pfx,localBindings.get(pfx));
            }
        }

        String select = var.getSelect();
        Vector<XdmItem> results = evaluateXPath(doc, nsBindings, select, globals);
        String value = "";

        try {
            for (XdmItem item : results) {
                if (item.isAtomicValue()) {
                    value += item.getStringValue();
                } else {
                    XdmNode node = (XdmNode) item;
                    if (node.getNodeKind() == XdmNodeKind.ATTRIBUTE
                            || node.getNodeKind() == XdmNodeKind.NAMESPACE) {
                        value += node.getStringValue();
                    } else {
                        XdmDestination dest = new XdmDestination();
                        S9apiUtils.writeXdmValue(runtime,item,dest,null);
                        value += dest.getXdmNode().getStringValue();
                    }
                }
            }
        } catch (SaxonApiUncheckedException saue) {
            Throwable sae = saue.getCause();
            if (sae instanceof XPathException) {
                XPathException xe = (XPathException) sae;
                if ("http://www.w3.org/2005/xqt-errors".equals(xe.getErrorCodeNamespace()) && "XPDY0002".equals(xe.getErrorCodeLocalPart())) {
                    throw XProcException.dynamicError(26, step.getNode(), "The expression for $" + var.getName() + " refers to the context item.");
                } else {
                    throw saue;
                }
            } else {
                throw saue;
            }
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        // Now test to see if the option is a reasonable value
        if (var.getType() != null) {
            String type = var.getType();
            if (type.contains("|")) {
                TypeUtils.checkLiteral(value, type);
            } else if (type.contains(":")) {
                TypeUtils.checkType(runtime, value, var.getTypeAsQName(), var.getNode());
            }
        }

        // Section 5.7.5 Namespaces on variables, options, and parameters
        //
        // If the select attribute was used to specify the value and it consisted of a single VariableReference
        // (per [XPath 1.0] or [XPath 2.0], as appropriate), then the namespace bindings from the referenced
        // option or variable are used.
        Pattern varrefpat = Pattern.compile("^\\s*\\$([^\\s=]+)\\s*$");
        Matcher varref = varrefpat.matcher(select);
        if (varref.matches()) {
            String varrefstr = varref.group(1);
            QName varname = null;
            if (varrefstr.contains(":")) {
                String vpfx = varrefstr.substring(0, varrefstr.indexOf(":"));
                String vlocal = varrefstr.substring(varrefstr.indexOf(":")+1);
                String vns = nsBindings.get(vpfx);
                varname = new QName(vpfx, vns, vlocal);
            } else {
                varname = new QName("", varrefstr);
            }
            RuntimeValue val = globals.get(varname);
            nsBindings = val.getNamespaceBindings();
        }

        // Section 5.7.5 Namespaces on variables, options, and parameters
        //
        // If the select attribute was used to specify the value and it evaluated to a node-set, then the in-scope
        // namespaces from the first node in the selected node-set (or, if it's not an element, its parent) are used.
        if (results.size() > 0 && results.get(0) instanceof XdmNode) {
            XdmNode node = (XdmNode) results.get(0);
            nsBindings.clear();

            XdmSequenceIterator nsIter = node.axisIterator(Axis.NAMESPACE);
            while (nsIter.hasNext()) {
                XdmNode ns = (XdmNode) nsIter.next();
                nsBindings.put((ns.getNodeName()==null ? "" : ns.getNodeName().getLocalName()),ns.getStringValue());
            }
        }

        if (runtime.getAllowGeneralExpressions()) {
            return new RuntimeValue(value,results,var.getNode(),nsBindings);
        } else {
            return new RuntimeValue(value,var.getNode(),nsBindings);
        }
    }

    protected Vector<XdmItem> evaluateXPath(XdmNode doc, Hashtable<String,String> nsBindings, String xpath, Hashtable<QName,RuntimeValue> globals) {
        Vector<XdmItem> results = new Vector<XdmItem> ();
        Hashtable<QName,RuntimeValue> boundOpts = new Hashtable<QName,RuntimeValue> ();

        for (QName name : globals.keySet()) {
            RuntimeValue v = globals.get(name);
            if (v.initialized()) {
                boundOpts.put(name, v);
            }
        }

        try {
            XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
            xcomp.setBaseURI(step.getNode().getBaseURI());

            for (QName varname : boundOpts.keySet()) {
                xcomp.declareVariable(varname);
            }

            for (String prefix : nsBindings.keySet()) {
                xcomp.declareNamespace(prefix, nsBindings.get(prefix));
            }
            XPathExecutable xexec = null;
            try {
                xexec = xcomp.compile(xpath);
            } catch (SaxonApiException sae) {
                Throwable t = sae.getCause();
                if (t instanceof XPathException) {
                    XPathException xe = (XPathException) t;
                    if (xe.getMessage().contains("Undeclared (or unbound?) variable")) {
                        throw XProcException.dynamicError(26, step.getNode(), xe.getMessage());
                    }
                }
                throw sae;
            }

            XPathSelector selector = xexec.load();

            for (QName varname : boundOpts.keySet()) {
                XdmValue value = null;
                RuntimeValue rval = boundOpts.get(varname);
                if (runtime.getAllowGeneralExpressions() && rval.hasGeneralValue()) {
                    value = rval.getValue();
                } else {
                    value = rval.getUntypedAtomic(runtime);
                }
                selector.setVariable(varname,value);
            }

            if (doc != null) {
                selector.setContextItem(doc);
            }

            try {
                Iterator<XdmItem> values = selector.iterator();
                while (values.hasNext()) {
                    results.add(values.next());
                }
            } catch (SaxonApiUncheckedException saue) {
                Throwable sae = saue.getCause();
                if (sae instanceof XPathException) {
                    XPathException xe = (XPathException) sae;
                    if ("http://www.w3.org/2005/xqt-errors".equals(xe.getErrorCodeNamespace()) && "XPDY0002".equals(xe.getErrorCodeLocalPart())) {
                        throw XProcException.dynamicError(26, step.getNode(), "Expression refers to context when none is available: " + xpath);
                    } else {
                        throw saue;
                    }

                } else {
                    throw saue;
                }
            }
        } catch (SaxonApiException sae) {
            if (S9apiUtils.xpathSyntaxError(sae)) {
                throw XProcException.dynamicError(23, step.getNode(), sae.getCause().getMessage());
            } else {
                throw new XProcException(sae);
            }
        }

        return results;
    }
}
