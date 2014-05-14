package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcData;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadableEmpty;
import com.xmlcalabash.model.*;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 5:26:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class XCompoundStep extends XAtomicStep {
    protected Hashtable<QName, RuntimeValue> variables = new Hashtable<QName,RuntimeValue> ();
    protected Vector<XStep> subpipeline = new Vector<XStep> ();

    public XCompoundStep(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step, parent);
    }

    /*
    public void addVariable(QName name, RuntimeValue value) {
        variables.put(name, value);
    }
    */

    public boolean hasInScopeVariableBinding(QName name) {
        if (variables.containsKey(name) || inScopeOptions.containsKey(name)) {
            return true;
        }

        return getParent() == null ? false : getParent().hasInScopeVariableBinding(name);
    }

    public boolean hasInScopeVariableValue(QName name) {
        if (variables.containsKey(name) || inScopeOptions.containsKey(name)) {
            RuntimeValue v = getVariable(name);
            return v != null &&  v.initialized();
        }

        return getParent() == null ? false : getParent().hasInScopeVariableValue(name);
    }

    public RuntimeValue getVariable(QName name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else {
            if (inScopeOptions.containsKey(name)) {
                return inScopeOptions.get(name);
            } else {
                return null;
            }
        }
    }

    public ReadablePipe getBinding(String stepName, String portName) {
        if (name.equals(stepName)) {
            XInput input = getInput(portName);
            return input.getReader();
        }

        for (XStep step : subpipeline) {
            if (stepName.equals(step.getName())) {
                XOutput output = step.getOutput(portName);
                if (output == null) {
                    return new ReadableEmpty();
                } else {
                    ReadablePipe rpipe = output.getReader();
                    return rpipe;
                }
            }
        }
        return parent.getBinding(stepName, portName);
    }

    protected void addStep(XStep step) {
        subpipeline.add(step);
    }

    public void instantiate(Step step) {
        finest(step.getNode(), "--> instantiate " + step);
        
        instantiateReaders(step);
        parent.addStep(this);

        DeclareStep decl = step.getDeclaration();

        for (Step substep : decl.subpipeline()) {
            if (XProcConstants.p_choose.equals(substep.getType())) {
                XChoose newstep = new XChoose(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_group.equals(substep.getType())) {
                XGroup newstep = new XGroup(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_try.equals(substep.getType())) {
                XTry newstep = new XTry(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_catch.equals(substep.getType())) {
                XCatch newstep = new XCatch(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_for_each.equals(substep.getType())) {
                XForEach newstep = new XForEach(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_viewport.equals(substep.getType())) {
                XViewport newstep = new XViewport(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.cx_until_unchanged.equals(substep.getType())) {
                XUntilUnchanged newstep = new XUntilUnchanged(runtime,substep,this);
                newstep.instantiate(substep);
            } else if (substep.isPipelineCall()) {
                DeclareStep subdecl = substep.getDeclaration();
                XPipelineCall newstep = new XPipelineCall(runtime, substep, this);
                newstep.setDeclaration(subdecl);
                newstep.instantiate(substep);

                /*
                // Make sure the caller's inputs and outputs have the right sequence values
                for (Input input : subdecl.inputs()) {
                    String port = input.getPort();
                    for (ReadablePipe rpipe : inputs.get(port)) {
                        rpipe.canReadSequence(input.getSequence());
                    }
                }

                for (Output output : subdecl.outputs()) {
                    String port = output.getPort();
                    WritablePipe wpipe = outputs.get(port);
                    wpipe.canWriteSequence(output.getSequence());
                }
                */
            } else {
                XAtomicStep newstep = new XAtomicStep(runtime, substep, this);
                newstep.instantiate(substep);
            }
        }

        for (Input input : step.inputs()) {
            String port = input.getPort();
            if (port.startsWith("|")) {
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
                    pipe.setReader(step);
                    readers.add(pipe);
                    finest(step.getNode(), step.getName() + " reads from " + pipe + " for " + port);
                    
                    /* Attempted fix by ndw on 7 Dec...seems to work
                    if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                        PipeNameBinding pnbinding = (PipeNameBinding) binding;
                        ReadablePipe pipe = getBinding(pnbinding.getStep(), pnbinding.getPort());
                        pipe.canReadSequence(input.getSequence());
                        pipe.setReader(step);
                        readers.add(pipe);
                        finest(step.getNode(), step.getName() + " reads from " + pipe + " for " + port);
                    } else {
                        throw new XProcException("Don't know how to handle binding " + binding.getBindingType());
                    }
                    */
                }

                XInput xinput = new XInput(runtime, input);
                addInput(xinput);
            }
        }

        for (Output output : step.outputs()) {
            String port = output.getPort();
            if (port.endsWith("|")) {
                String rport = port.substring(0,port.length()-1);
                XInput xinput = getInput(rport);
                WritablePipe wpipe = xinput.getWriter();
                wpipe.setWriter(step);
                wpipe.canWriteSequence(true); // Let the other half work it out
                outputs.put(port, wpipe);
                finest(step.getNode(), step.getName() + " writes to " + wpipe + " for " + port);
            } else {
                XOutput xoutput = new XOutput(runtime, output);
                xoutput.setLogger(step.getLog(port));
                addOutput(xoutput);
                WritablePipe wpipe = xoutput.getWriter();
                wpipe.setWriter(step);
                wpipe.canWriteSequence(output.getSequence());
                outputs.put(port, wpipe);
                finest(step.getNode(), step.getName() + " writes to " + wpipe + " for " + port);
            }
        }
    }

    protected void copyInputs() throws SaxonApiException {
        for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments()) {
                        XdmNode doc = reader.read();
                        pipe.write(doc);
                        finest(step.getNode(), "Compound input copy from " + reader + " to " + pipe);
                    }
                }
            }
        }
    }

    public void reset() {
        super.reset();
        for (XStep step : subpipeline) {
            step.reset();
        }
    }

    public void run() throws SaxonApiException {
        XProcData data = runtime.getXProcData();
        data.openFrame(this);
        
        copyInputs();

        // N.B. At this time, there are no compound steps that accept parameters or options,
        // so the order in which we calculate them doesn't matter. That will change if/when
        // there are such compound steps.

        // Calculate all the options
        inScopeOptions = parent.getInScopeOptions();
        for (QName name : step.getOptions()) {
            Option option = step.getOption(name);
            RuntimeValue value = computeValue(option);
            setOption(name, value);
            inScopeOptions.put(name, value);
        }

        for (Variable var : step.getVariables()) {
            RuntimeValue value = computeValue(var);
            inScopeOptions.put(var.getName(), value);
        }

        runtime.start(this);
        for (XStep step : subpipeline) {
            step.run();
        }
        runtime.finish(this);

        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments()) {
                        XdmNode doc = reader.read();
                        pipe.write(doc);
                        finest(step.getNode(), "Compound output copy from " + reader + " to " + pipe);
                    }
                }
            }
        }

        data.closeFrame();
    }
}
