package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcData;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.Option;
import com.xmlcalabash.model.Output;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Variable;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 10, 2008
 * Time: 7:22:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class XPipeline extends XCompoundStep {
    private static final QName c_param_set = new QName("c", XProcConstants.NS_XPROC_STEP, "param-set");
    private static final QName c_param = new QName("c", XProcConstants.NS_XPROC_STEP, "param");
    private static final QName _name = new QName("name");
    private static final QName _namespace = new QName("namespace");
    private static final QName _value = new QName("value");

    private Hashtable<QName, RuntimeValue> optionsPassedIn = null;

    public XPipeline(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step, parent);
    }

    public DeclareStep getDeclareStep() {
        return step.getDeclaration();
    }

    public void passOption(QName name, RuntimeValue value) {
        if (optionsPassedIn == null) {
            optionsPassedIn = new Hashtable<QName,RuntimeValue> ();
        }
        optionsPassedIn.put(name,value);
    }

    public Hashtable<QName,RuntimeValue> getInScopeOptions() {
        // We make a copy so that what our children do can't effect us
        Hashtable<QName,RuntimeValue> globals = new Hashtable<QName,RuntimeValue> ();
        if (inScopeOptions != null) {
            for (QName name : inScopeOptions.keySet()) {
                globals.put(name,inScopeOptions.get(name));
            }
        }

        // We also need to pass through any options passed in...
        if (optionsPassedIn != null) {
            for (QName name : optionsPassedIn.keySet()) {
                globals.put(name,optionsPassedIn.get(name));
            }
        }

        return globals;
    }

    public Set<String> getInputs() {
        HashSet<String> ports = new HashSet<String> ();
        for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                ports.add(port);
            }
        }
        return ports;
    }

    public void clearInputs(String port) {
        Vector<ReadablePipe> v = inputs.get(port);
        v.clear();
    }

    public void writeTo(String port, XdmNode node) {
        WritablePipe pipe = outputs.get(port+"|");
        finest(step.getNode(), "writesTo " + pipe + " for " + port);
        pipe.write(node);
    }

    public Set<String> getOutputs() {
        HashSet<String> ports = new HashSet<String> ();
        for (String port : outputs.keySet()) {
            if (!port.endsWith("|")) {
                ports.add(port);
            }
        }
        return ports;
    }

    public ReadablePipe readFrom(String port) {
        ReadablePipe rpipe = null;
        XOutput output = getOutput(port);
        rpipe = output.getReader();
        rpipe.canReadSequence(true); // FIXME: I should be able to set this correctly!
        return rpipe;
    }

    public Serialization getSerialization(String port) {
        Output output = step.getOutput(port);
        return output.getSerialization();
    }

    public void run() throws SaxonApiException {
        QName infoName = XProcConstants.p_pipeline;
        /*
        if (!step.isAnonymous()) {
            infoName = step.getDeclaredType();
        }
        */

        fine(null, "Running " + infoName + " " + step.getName());
        if (runtime.getAllowGeneralExpressions()) {
            fine(step.getNode(), "Running with the 'general-values' extension enabled.");
        }

        XProcData data = runtime.getXProcData();
        data.openFrame(this);

        runtime.start(this);
        try {
            doRun();
        } catch (XProcException ex) {
            runtime.error(ex);
            throw ex;
        } catch (SaxonApiException ex) {
            runtime.error(ex);
            throw ex;
        } finally {
            runtime.finish(this);
            data.closeFrame();
        }
    }

    private void setupParameters() {
        Vector<String> ports = new Vector<String> ();
        Iterator<String> portIter = getParameterPorts().iterator();
        while (portIter.hasNext()) {
            ports.add(portIter.next());
        }

        for (String port : ports) {
            TreeWriter tree = new TreeWriter(runtime);

            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(c_param_set);
            tree.startContent();

            Iterator<QName> paramIter = getParameters(port).iterator();
            while (paramIter.hasNext()) {
                QName name = paramIter.next();

                String value = getParameter(port, name).getString();
                tree.addStartElement(c_param);
                tree.addAttribute(_name, name.getLocalName());
                if (name.getNamespaceURI() != null) {
                    tree.addAttribute(_namespace, name.getNamespaceURI());
                }
                tree.addAttribute(_value, value);
                tree.startContent();
                tree.addEndElement();
            }

            tree.addEndElement();
            tree.endDocument();

            writeTo(port,tree.getResult());
        }
    }

    private void doRun() throws SaxonApiException {
        for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);

                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments()) {
                        XdmNode doc = reader.read();
                        pipe.write(doc);
                        finest(step.getNode(), "Pipeline input copy from " + reader + " to " + pipe);
                    }
                }
            }
        }

        setupParameters();

        // N.B. At this time, there are no compound steps that accept parameters or options,
        // so the order in which we calculate them doesn't matter. That will change if/when
        // there are such compound steps.

        // Calculate all the options
        inScopeOptions = parent.getInScopeOptions();
        for (QName name : step.getOptions()) {
            Option option = step.getOption(name);
            RuntimeValue value = null;
            if (optionsPassedIn != null && optionsPassedIn.containsKey(name)) {
                value = optionsPassedIn.get(name);
            } else {
                if (option.getRequired() && option.getSelect() == null) {
                    throw XProcException.staticError(18, option.getNode(), "No value provided for required option \"" + option.getName() + "\"");
                }

                if (option.getSelect() == null) {
                    value = new RuntimeValue();
                } else {
                    value = computeValue(option);
                }
            }

            setOption(name, value);
            inScopeOptions.put(name, value);
        }

        for (Variable var : step.getVariables()) {
            RuntimeValue value = computeValue(var);
            inScopeOptions.put(var.getName(), value);
        }

        for (XStep step : subpipeline) {
            step.run();
        }

        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    // Check for the case where there are no documents, but a sequence is not allowed
                    if (!reader.moreDocuments() && !pipe.writeSequence()) {
                        throw XProcException.dynamicError(7);
                    }


                    while (reader.moreDocuments()) {
                        XdmNode doc = reader.read();
                        pipe.write(doc);
                        finest(step.getNode(), "Pipeline output copy from " + reader + " to " + pipe);
                    }
                }
                pipe.close(); // Indicate that we're done writing to it
            }
        }
    }
}
