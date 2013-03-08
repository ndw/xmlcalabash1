/*
 * XProcStep.java
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

package com.xmlcalabash.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;
import java.util.List;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

/**
 *
 * @author ndw
 */
public class Step extends SourceArtifact {
    private static final QName cx_depend = new QName("cx",XProcConstants.NS_CALABASH_EX,"depend");
    private static final QName cx_depends = new QName("cx",XProcConstants.NS_CALABASH_EX,"depends");
    private static final QName cx_dependson = new QName("cx",XProcConstants.NS_CALABASH_EX,"dependson");

    protected QName stepType = null;
    protected String stepName = null;
    private boolean anonymous = false;
    protected Vector<Input> inputs = new Vector<Input> ();
    protected Vector<Output> outputs = new Vector<Output> ();
    private Vector<Option> options = new Vector<Option> ();
    private Vector<Log> logs = new Vector<Log> ();
    private Vector<Parameter> params = new Vector<Parameter> ();
    private HashSet<String> dependsOn = new HashSet<String> ();
    protected Environment env = null;
    protected Step parent = null;
    private int depth = -1;
    private boolean ordered = false;
    private static HashSet<String> anonNames = new HashSet<String> ();
    // FIXME: This should only be in compoundstep!
    Vector<Step> subpipeline = new Vector<Step>();
    protected DeclareStep declaration = null;
    protected Double version = null;

    /** Creates a new instance of Step */
    public Step(XProcRuntime xproc, XdmNode node, QName type) {
        super(xproc, node);
        stepType = type;
        stepName = anonymousName(node);
    }

    public Step(XProcRuntime xproc, XdmNode node, QName type, String name) {
        super(xproc, node);
        stepType = type;
        stepName = name;
        if (stepName == null) {
            stepName = anonymousName(node);
        }
    }

    public boolean isPipeline() {
        return false;
    }

    private synchronized String anonymousName(XdmNode node) {
        String stepName = recursiveAnonymousName(node);
        anonNames.add(stepName);
        return stepName;
    }

    private String recursiveAnonymousName(XdmNode node) {
        if (node.getParent().getNodeKind() == XdmNodeKind.DOCUMENT) {
            return "!1";
        } else {
            XdmSequenceIterator iter = node.axisIterator(Axis.PRECEDING_SIBLING);
            int count = 1;
            while (iter.hasNext()) {
                XdmNode pnode = (XdmNode) iter.next();
                if (pnode.getNodeKind() == XdmNodeKind.ELEMENT) {
                    count++;
                }
            }
            return anonymousName(node.getParent()) + "." + count;
        }
    }

    /*
    private String anonymousName(XdmNode node) {
        int count = 1;
        XdmSequenceIterator iter = node.axisIterator(Axis.PRECEDING_SIBLING);
        while (iter.hasNext()) {
            XdmNode pnode = (XdmNode) iter.next();
            if (pnode.getNodeKind() == XdmNodeKind.ELEMENT) {
                count++;
            }
        }
        XdmNode parent = node.getParent();
        if (parent != null && parent.getNodeKind() == XdmNodeKind.ELEMENT) {
            return anonymousName(parent) + "." + count;
        } else {
            return "!" + count;
        }
    }
    */

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setDeclaration(DeclareStep decl) {
        declaration = decl;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    public boolean isPipelineCall() {
        return !declaration.isAtomic();
    }

    public QName getType() {
        return stepType;
    }

    public Step getStep() {
        return this;
    }

    protected void setVersion(Double version) {
        this.version = version;
    }

    public Double getVersion() {
        if (this.version == null) {
            if (parent != null) {
                return parent.getVersion();
            } else
                throw new UnsupportedOperationException("Step with no version or inherited version!?");
        } else {
            return version;
        }
    }

    public QName getDeclaredType() {
        // For p:declare-step and p:pipeline, this is different from the type.
        return stepType;
    }

    public String getName() {
        return stepName;
    }

    public XdmNode getNode() {
        return node;
    }
    
    public Step getPipeline() {
        Step parent = this;
        while (parent != null
               && parent.stepType != XProcConstants.p_declare_step) {
            parent = parent.parent;
        }
        return parent;
    }

    public boolean containsStep(String stepName) {
        return false; // atomic steps have no subpipelines
    }

    public void addStep(Step step) {
        step.parent = this;
        subpipeline.add(step);
    }

    public void setSubpipeline(Vector<Step> pipeline) {
        subpipeline = pipeline;
    }

    public Vector<Step> subpipeline() {
        return subpipeline;
    }

    public void addVariable(Variable variable) {
        throw new UnsupportedOperationException("You can only call addVariable() on a compound step");
    }

    public Collection<Variable> getVariables() {
        // Atomic steps have no getVariables
        return new Vector<Variable> ();
    }

    public void addInput(Input input) {
        input.setStep(this);
        
        for (Input current : inputs) {
            if (current.getPort().equals(input.getPort())) {
                throw XProcException.staticError(11, input.getNode(), "Input port name '" + input.getPort() + "' appears more than once.");
            }
        }
        
        inputs.add(input);
    }

    public Vector<Input> inputs() {
        return inputs;
    }
    
    public Input getInput(String portName) {
        for (Input input : inputs) {
            if (portName.equals(input.getPort())) {
                return input;
            }
        }
        return null;
    }
    
    public void addOutput(Output output) {
        output.setStep(this);

        for (Output current : outputs) {
            if (current.getPort().equals(output.getPort())) {
                throw XProcException.staticError(11, output.getNode(), "Output port name '" + output.getPort() + "' appears more than once.");
            }
        }

        outputs.add(output);
    }

    public Vector<Output> outputs() {
        return outputs;
    }

    public Output getPrimaryOutput() {
        int count = 0;
        Output defPrimary = null;
        for (Output output : outputs) {
            if ("#current".equals(output.getPort())) {
                // nop; this one never counts
            } else {
                if (output.getPrimary() || !output.getPrimarySet()) {
                    defPrimary = output;
                }
                count++;
                if (output.getPrimary()) {
                    return output;
                }
            }
        }

        if (count == 1) {
            return defPrimary;
        } else {
            return null;
        }
    }

    public Output getOutput(String portName) {
        for (Output output : outputs) {
            if (portName.equals(output.getPort())) {
                return output;
            }
        }
        return null;
    }
    
    public void addOption(Option option) {
        // FIXME: Is it worth making a hash for this?
        QName optName = option.getName();
        for (Option exoption : options) {
            if (optName.equals(exoption.getName())) {
                error(option.getNode(),"Duplication option name: " + optName,XProcConstants.staticError(4));
            }
        }
        options.add(option);
    }

    public Vector<Option> options() {
        return options;
    }

    public Option getOption(QName name) {
        for (Option option : options) {
            if (name.equals(option.getName())) {
                return option;
            }
        }
        return null;
    }

    public List<QName> getOptions() {
        Vector<QName> names = new Vector<QName> ();
        for (Option option : options) {
            names.add(option.getName());
        }
        return names;
    }
    
    public void addLog(Log log) {
        logs.add(log);
    }
    
    public Log getLog(String port) {
        for (Log log : logs)  {
            if (port.equals(log.getPort())) {
                return log;
            }
        }
        return null;
    }
    
    public void addParameter(Parameter param) {
        params.add(param);
    }

    public Parameter getParameter(QName name) {
        for (Parameter param : params) {
            if (name.equals(param.getName())) {
                return param;
            }
        }
        return null;
    }

    public List<QName> getParameters() {
        Vector<QName> names = new Vector<QName> ();
        for (Parameter param : params) {
            names.add(param.getName());
        }
        return names;
    }
    
    public Vector<Parameter> parameters() {
        return params;
    }
    
    public boolean loops() {
        return false;
    }

    public boolean insideALoop() {
        if (parent == null) {
            return false;
        }

        return parent.loops() || parent.insideALoop();
    }
    
    public Output getDefaultOutput() {
        // See if there's exactly one "ordinary" output
        int count = 0;
        Output defout = null;
        for (Output output : outputs) {
            if (!output.getPort().startsWith("#")) {
                if (output.getPrimary()) {
                    return output;
                }
                if (!output.getPort().endsWith("|")) {
                    count++;
                    defout = output;
                }
            }
        }
        
        if (count == 1 && (defout.getPrimary() || !defout.getPrimarySet())) {
            return defout;
        }
        
        return null;
    }
    
    protected void addDependency(String stepName) {
        dependsOn.add(stepName);
    }

    protected HashSet<String> getDependencies() {
        return dependsOn;
    }

    protected boolean dependsOn(String stepName) {
        return dependsOn.contains(stepName);
    }
    
    protected boolean matchesDeclaration() {
        boolean valid = true;
        Step decl = declaration;

        // FIXME: is this still true
        if (decl == null) { // must be the root pipeline
            return true;
        }
        
        Hashtable<String,Input> declInputs = new Hashtable<String,Input> ();
        for (Input input : decl.inputs()) {
            declInputs.put(input.getPort(), input);
        }
        
        for (Input input : inputs()) {
            String port = input.getPort();
            if (!port.startsWith("|")) {
                if (!declInputs.containsKey(port)) {
                    if (getVersion() == 1.0) {
                      error("Undeclared input port '" + port + "' on " + this, XProcConstants.staticError(10));
                      valid = false;
                    }
                } else {
                    input.setPrimary(declInputs.get(port).getPrimary());
                }
            }
        }

        Hashtable<String,Output> declOutputs = new Hashtable<String,Output>();
        for (Output output : decl.outputs()) {
            declOutputs.put(output.getPort(), output);
        }

        for (Output output : outputs()) {
            String port = output.getPort();
            if (!port.endsWith("|")) {
                if (!declOutputs.containsKey(port) && !declOutputs.containsKey("*")) {
                    error("Undeclared output port: " + port, XProcConstants.staticError(10));
                    valid = false;
                } else {
                    output.setPrimary(declOutputs.get(port).getPrimary());
                }
            }
        }

        return valid;
    }

    protected boolean validOptions() {
        HashSet<QName> names = new HashSet<QName> ();
        boolean valid = true;
        
        for (Option p : options) {
            valid = valid && p.valid(env);
            QName pName = p.getName();
            if (pName == null) {
                valid = false;
                error("Option without name", XProcConstants.staticError(38));
            } else {
                if (names.contains(pName)) {
                    valid = false;
                    error("Duplicate option name: " + pName, XProcConstants.staticError(4));
                } else {
                    names.add(pName);
                }
            }
        }

        // Do we have valid options?
        Step decl = declaration;
        if (decl != null) {
            // Do we have all the required options?
            for (Option doption : decl.options()) {
                if (doption.getRequired()) {
                    if (getOption(doption.getName()) == null) {
                        valid = false;
                        error("Required option not specified: " + doption.getName(), XProcConstants.staticError(18));
                    }
                }
            }

            // Are all the options we have allowed?
            Vector<Option> okOpts = new Vector<Option> ();
            for (Option option : options()) {
                Option doption = decl.getOption(option.getName());
                if (doption == null) {
                    if (getVersion() > 1.0) {
                        // nop
                    } else {
                        valid = false;
                        error("Undeclared option specified: " + option.getName(), XProcConstants.staticError(10));
                    }
                } else {
                    okOpts.add(option);
                }
            }
            // Make sure bogus 2.0 options are removed, otherwise we have to test for them elsewhere
            options = okOpts;
        }
        
        return valid;
    }

    protected boolean validParams() {
        HashSet<QName> names = new HashSet<QName> ();
        boolean valid = true;
        
        for (Parameter p : params) {
            valid = valid && p.valid(env);
            QName pName = p.getName();
            if (pName == null) {
                valid = false;
                error("Parameter without name", XProcConstants.staticError(38));
            } else {
                if (names.contains(pName)) {
                    valid = false;
                    error("Duplicate parameter name: " + pName, XProcConstants.staticError(4));
                } else {
                    names.add(pName);
                }
            }
            
            String port = p.getPort();
            if (port == null) {
                for (Input input : inputs()) {
                    if (input.getParameterInput() && input.getPrimary()) {
                        port = input.getPort();
                    }
                }
                if (port == null) {
                    for (Input input : inputs()) {
                        if (input.getParameterInput()) {
                            if (port != null) {
                                error("Port not specified and multiple parameter input ports", XProcException.err_E0001);
                            }
                            port = input.getPort();
                        }
                    }
                }
            }

            if (port == null) {
                valid = false;
                error("Port not specified and no primary parameter input port", XProcException.err_E0001);
            } else {
                Input input = getInput(port);
                if (input == null || !input.getParameterInput()) {
                    valid = false;
                    error("Port is not a parameter input port: " + port, XProcException.err_E0001);
                }
            }
        }
        
        return valid;
    }

    protected boolean validBindings() {
        boolean valid = true;
        boolean seenPrimaryDoc = false;
        boolean seenPrimaryParam = false;
       
        for (Input input : inputs()) {
            if (!input.getPort().startsWith("|") && input.getPrimary()) {
                if (input.getParameterInput()) {
                    if (seenPrimaryParam) {
                        error("At most one primary parameter input port is allowed", XProcConstants.staticError(30));
                    }
                    seenPrimaryParam = true;
                } else {
                    if (seenPrimaryDoc) {
                        error("At most one primary input port is allowed", XProcConstants.staticError(30));
                    }
                    seenPrimaryDoc = true;
                }
            }
            
            if (!checkBinding(input)) {
                valid = false;
            }
        }

        for (Option option: options()) {
            // p:option elements aren't allowed to have bindings
            if (XProcConstants.p_with_option.equals(option.getNode().getNodeName())) {
                if (!checkOptionBinding(option, true)) {
                    valid = false;
                }
            }
        }

        for (Parameter param: parameters()) {
            if (!checkOptionBinding(param, true)) {
                valid = false;
            }
        }

        return valid;
    }

    protected void checkDuplicateVars(HashSet<QName> vars) {
        for (Variable var : getVariables()) {
            if (vars.contains(var.getName())) {
                throw XProcException.staticError(4, getNode(), "Duplicate variable name: " + var.getName());
            } else {
                vars.add(var.getName());
            }
        }
    }

    protected boolean checkBinding(Input input) {
        boolean valid = true;

        runtime.finest(null, node, "Check bindings for " + input.getPort() + " on " + getName());
        
        if (input.getBinding().size() == 0) {
            if (input.getParameterInput()) {
                if (input.getPrimary()) {
                    // We need to bind to the primary parameter input of our ancestor pipeline
                    Step pipeline = getPipeline();
                    Input paramsin = null;
                    int count = 0;
                    for (Input pinput : pipeline.inputs()) {
                        if (pinput.getParameterInput()) {
                            count++;
                            if (paramsin == null || pinput.getPrimary()) {
                                paramsin = pinput;
                            }
                        }
                    }
                
                    if (paramsin == null || (!paramsin.getPrimary() && count > 1)) {
                        if (params.size() > 0) {
                            // There's at least one explicit with-param, so unbound is ok.
                            EmptyBinding binding = new EmptyBinding(runtime, node);
                            input.addBinding(binding);
                        } else {
                            valid = false;
                            error("Parameter input " + input.getPort() + " unbound on " + getType() + " step named " + getName() + " and no default binding available.", XProcConstants.staticError(55));
                        }
                    } else {
                        PipeNameBinding binding = new PipeNameBinding(runtime, node);
                        binding.setStep(pipeline.getName());
                        binding.setPort(paramsin.getPort());
                        input.addBinding(binding);
                    }
                } else {
                    EmptyBinding binding = new EmptyBinding(runtime, node);
                    input.addBinding(binding);
                }
            } else {
                Port port = env.getDefaultReadablePort();
            
                if (input.getPort().startsWith("|")) {
                    if (subpipeline.size() > 0) {
                        // This needs to be bound to the output of the last step.
                        Step substep = subpipeline.get(subpipeline.size()-1);
                        port = substep.getDefaultOutput();
                    } else if (isPipelineCall()) {
                        // This doesn't need/shouldn't have a binding...the pipeilne call will take care of it.
                        return true;
                    }
                }

                if (input.getPrimary() && port != null) {
                    String stepName = port.getStep().getName();
                    String portName = port.getPort();

                    PipeNameBinding binding = new PipeNameBinding(runtime, node);
                    binding.setStep(stepName);
                    binding.setPort(portName);

                    //errhandler.warning(node, "Manufactured binding for " + input.getPort() + " to " + portName + " on " + stepName);
                    input.addBinding(binding);
                } else {
                    // If there's a default binding, use it. FIXME: Is it safe to copy the binding like this?
                    Input declIn = declaration.getInput(input.getPort());
                    if (declIn.getBinding().size() != 0) {
                        input.setSelect(declIn.getSelect());
                        for (Binding binding : declIn.getBinding()) {
                            input.addBinding(binding);
                        }
                    } else {
                        valid = false;
                        error("Input " + input.getPort() + " unbound on " + getType() + " step named " + getName() + " and no default binding available.", XProcConstants.staticError(32));
                    }
                }
            }
        }

        boolean catchErrors = false;
        for (Binding binding : input.getBinding()) {
            if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                PipeNameBinding pipe = (PipeNameBinding) binding;
                Output output = env.readablePort(pipe.getStep(), pipe.getPort());
                if (output == null) {
                    Step fromstep = env.visibleStep(pipe.getStep());

                    if (fromstep == null) {
                        throw XProcException.staticError(22,binding.getNode(),"No step named \"" + pipe.getStep() + "\" is visible here.");
                    }

                    if ("error".equals(pipe.getPort()) && XProcConstants.p_catch.equals(fromstep.getType())) {
                        catchErrors = true;
                    } else {
                        if (XProcConstants.NS_XPROC.equals(fromstep.getType().getNamespaceURI())
                                && getVersion() > 1.0) {
                            // Nevermind, it's ok to bind to unknown ports in this case
                            input.setSequence(true);
                        } else {
                            error(binding.getNode(),"No port named \"" + pipe.getPort() + "\" on step named \"" + pipe.getStep() + "\"", XProcConstants.staticError(22));
                            valid = false;
                        }
                    }
                } else if (!pipe.getPort().equals(output.getPort())) {
                    Step step = env.visibleStep(pipe.getStep());
                    if (XProcConstants.p_viewport.equals(step.getType())) {
                        // FIXME: This hack is just for p:viewport, do it somewhere else and better
                        pipe.setPort(output.getPort());
                    }
                }
            }
        }

        if (catchErrors) {
            Vector<Binding> newBindings = new Vector<Binding> ();
            Step fromstep = null;
            for (Binding binding : input.getBinding()) {
                catchErrors = false;
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding pipe = (PipeNameBinding) binding;
                    Output output = env.readablePort(pipe.getStep(), pipe.getPort());
                    if (output == null) {
                        fromstep = env.visibleStep(pipe.getStep());
                        if ("error".equals(pipe.getPort()) && XProcConstants.p_catch.equals(fromstep.getType())) {
                            catchErrors = true;
                        }
                    }
                }
                if (catchErrors) {
                    newBindings.add(new ErrorBinding(fromstep.getXProc(), fromstep.getNode()));
                } else {
                    newBindings.add(binding);
                }
            }
            input.clearBindings();
            for (Binding binding : newBindings) {
                input.addBinding(binding);
            }
        }

        return valid;
    }

    protected boolean checkOptionBinding(EndPoint endpoint) {
        return checkOptionBinding(endpoint, false);
    }

    protected boolean checkOptionBinding(EndPoint endpoint, boolean defEmpty) {
        boolean valid = true;

        runtime.finest(null, node, "Check bindings for " + endpoint + " on " + getName());

        if (endpoint.getBinding().size() == 0) {
            Port port = env.getDefaultReadablePort();

            if (port == null) {
                if (defEmpty) {
                    EmptyBinding empty = new EmptyBinding(runtime, node);
                    endpoint.addBinding(empty);
                } else {
                    valid = false;
                    error("" + endpoint + " unbound on " + getType() + " step named " + getName() + " and no default readable port.", XProcConstants.staticError(32));
                }
            } else {
                String stepName = port.getStep().getName();
                String portName = port.getPort();

                PipeNameBinding binding = new PipeNameBinding(runtime, node);
                binding.setStep(stepName);
                binding.setPort(portName);

                //errhandler.warning(node, "Manufactured binding for " + input.getPort() + " to " + portName + " on " + stepName);
                endpoint.addBinding(binding);
            }
        }

        boolean catchErrors = false;
        for (Binding binding : endpoint.getBinding()) {
            if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                PipeNameBinding pipe = (PipeNameBinding) binding;
                Output output = env.readablePort(pipe.getStep(), pipe.getPort());
                if (output == null) {
                    Step fromstep = env.visibleStep(pipe.getStep());
                    if ("error".equals(pipe.getPort()) && XProcConstants.p_catch.equals(fromstep.getType())) {
                        catchErrors = true;
                    } else {
                        error("Unreadable port: " + pipe.getPort() + " on " + pipe.getStep(), XProcConstants.staticError(22));
                        valid = false;
                    }
                } else {
                    if (XProcConstants.p_variable.equals(endpoint.getNode().getNodeName())) {
                        Step pipeStep = env.visibleStep(pipe.getStep());
                        Step container = pipeStep.parent;
                        if (container == this) {
                            throw XProcException.staticError(19, endpoint.getNode(), "Variable binding to " + pipe.getPort() + " on " + pipe.getStep() + " not allowed.");
                        }
                    }
                }
            }
        }

        if (catchErrors) {
            Vector<Binding> newBindings = new Vector<Binding> ();
            Step fromstep = null;
            for (Binding binding : endpoint.getBinding()) {
                catchErrors = false;
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding pipe = (PipeNameBinding) binding;
                    Output output = env.readablePort(pipe.getStep(), pipe.getPort());
                    if (output == null) {
                        fromstep = env.visibleStep(pipe.getStep());
                        if ("error".equals(pipe.getPort()) && XProcConstants.p_catch.equals(fromstep.getType())) {
                            catchErrors = true;
                        }
                    }
                }
                if (catchErrors) {
                    newBindings.add(new ErrorBinding(fromstep.getXProc(), fromstep.getNode()));
                } else {
                    newBindings.add(binding);
                }
            }
            endpoint.clearBindings();
            for (Binding binding : newBindings) {
                endpoint.addBinding(binding);
            }
        }

        return valid;
    }

    protected void checkForBindings(HashSet<Output> outputs) {
        for (Input input : inputs()) {
            for (Binding binding : input.bindings) {
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding b = (PipeNameBinding) binding;
                    Output output = env.readablePort(b.getStep(), b.getPort());
                    if (outputs.contains(output)) {
                        outputs.remove(output);
                    } else {
                        // Doesn't matter. Must be legit but doesn't help us.
                    }
                }
            }
        }

        for (Option option : options()) {
            for (Binding binding : option.bindings) {
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding b = (PipeNameBinding) binding;
                    Output output = env.readablePort(b.getStep(), b.getPort());
                    if (outputs.contains(output)) {
                        outputs.remove(output);
                    } else {
                        // Doesn't matter. Must be legit but doesn't help us.
                    }
                }
            }
        }

        for (Parameter param : parameters()) {
            for (Binding binding : param.bindings) {
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding b = (PipeNameBinding) binding;
                    Output output = env.readablePort(b.getStep(), b.getPort());
                    if (outputs.contains(output)) {
                        outputs.remove(output);
                    } else {
                        // Doesn't matter. Must be legit but doesn't help us.
                    }
                }
            }
        }
    }

    public boolean orderSteps() {
        boolean valid = true;

        if (ordered) {
            return true;
        }
        ordered = true;

        for (Step step : subpipeline) {
            if (!step.orderSteps()) {
                valid = false;
            }
        }
        
        runtime.finest(null, node, "Checking step order for " + getName());

        if (getExtensionAttribute(cx_depend) != null
            || getExtensionAttribute(cx_depends) != null
            || getExtensionAttribute(cx_dependson) != null) {
            throw new XProcException(getNode(), "The correct spelling of the depends-on attribute is cx:depends-on.");
        }

        String dependsOn = getExtensionAttribute(XProcConstants.cx_depends_on);
        if (dependsOn != null) {
            Step step = env.visibleStep(dependsOn);
            if (step == null) {
                throw new XProcException(getNode(), "The value of cx:depends-on must be the name of an in-scope step: " + dependsOn);
            }
            addDependency(dependsOn);
        }

        for (Input input : inputs) {
            for (Binding binding : input.getBinding()) {
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding pipe = (PipeNameBinding) binding;
                    runtime.finest(null, node, getName() + " input " + input.getPort() + " depends on " + pipe.getStep());
                    addDependency(pipe.getStep());
                }
            }
        }
        
        // I don't think output bindings matter...
        
        for (Parameter param : params) {
            for (Binding binding : param.getBinding()) {
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding pipe = (PipeNameBinding) binding;
                    runtime.finest(null, node, getName() + " param depends on " + pipe.getStep());
                    addDependency(pipe.getStep());
                }
            }
        }

        for (Option option : options) {
            for (Binding binding : option.getBinding()) {
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding pipe = (PipeNameBinding) binding;
                    runtime.finest(null, node, getName() + " option depends on " + pipe.getStep());
                    addDependency(pipe.getStep());
                }
            }
        }

        // HACK! This should all be refactored so that this isn't necessary!
        checkVariables();

        // Update step dependencies and find roots...
        Vector<Step> roots = new Vector<Step> ();
        for (Step step : subpipeline) {
            HashSet<String> deps = step.getDependencies();
            boolean root = true;
            for (String stepName : deps) {
                if (!stepName.equals(getName())) {
                    runtime.finest(null, node, getName() + " step " + step.getName() + " depends on " + stepName);
                    addDependency(stepName);
                }
                if (containsStep(stepName)) {
                    root = false;
                }
            }
            if (root) {
                runtime.finest(null, node, "==> " + step.getName() + " is a graph root of " + getName());
                roots.add(step);
                step.depth = 0;
            }
        }

        if (subpipeline.size() > 0) {
            if (roots.size() == 0) {
                error("No roots in " + getName(), XProcConstants.staticError(1));
                valid = false;
            } else {
                // Now find the dependency order for the substeps...
                int depth = 1;
                boolean noloops = true;
                while (noloops && roots.size() > 0) {
                    Vector<Step> nextWave = new Vector<Step> ();
                    for (Step root : roots) {
                        for (Step step : subpipeline) {
                            if (step.dependsOn(root.getName())) {
                                runtime.finest(null, node, "XProcStep " + step.getName() + " depends on " + root.getName() + "(step.depth="+step.depth+", depth="+depth+")");
                                if ((step.depth < 0) || (depth >= step.depth)) {
                                    step.depth = depth;
                                    runtime.finest(null, node, step.getName() + " gets depth " + depth);
                                    nextWave.add(step);
                                } else {
                                    noloops = false;
                                    error("Loop in subpipeline: " + step.getName() + " points back to " + root.getName(), XProcConstants.staticError(1));
                                }
                            } else {
                                runtime.finest(null, node, "XProcStep " + step.getName() + " does not depend on " + root.getName());
                            }
                        }
                    }
                    roots = nextWave;
                    depth++;
                }

                if (noloops) {
                    for (Step step : subpipeline) {
                        if (step.depth < 0) {
                            noloops = false;
                            error("Closed loop in subpipeline involves: " + step.getName(), XProcConstants.staticError(1));
                        }
                    }
                }
                
                valid = valid && noloops;
                
                if (valid) {
                    // Sort the subpipeline into a reasonable order
                    // FIXME: do a real sort, for crying out loud
                    Vector<Step> sorted = new Vector<Step> ();
                    int wave = 0;
                    while (wave < depth) {
                        for (Step step : subpipeline) {
                            if (step.depth == wave) {
                                sorted.add(step);
                            }
                        }
                        wave++;
                    }
                    subpipeline = sorted;
                }
            }
        }

        return valid;
    }

    public void checkVariables() {
        // nop; CompoundStep overrides this...
    }

    public boolean valid() {
        boolean valid = validParams();
        
        if (!matchesDeclaration()) {
            valid = false;
        }

        if (!validOptions()) {
            valid = false;
        }
        
        if (!validBindings()) {
            valid = false;
        }

        for (Log log : logs) {
            Output output = getOutput(log.getPort());
            if (output == null) {
                error("A p:log specified for a bad port: " + log.getPort(), XProcConstants.staticError(26));
                valid = false;
            }
        }

        if (env.countVisibleSteps(getName()) > 1) {
            error("Duplicate step name: " + getName(), XProcConstants.staticError(2));
            valid = false;
        }
        
        return valid;
    }

    protected void augmentIO() {
        Step decl = declaration;
        
        if (decl == null) {
            throw new UnsupportedOperationException("Unexpected step type.");
        }

        Hashtable<String,Input> declInputs = new Hashtable<String,Input> ();
        for (Input input : decl.inputs()) {
            declInputs.put(input.getPort(), input);
        }

        // Calculate the next position
        int position = 0;
        for (Input input : inputs()) {
            position++;
        }
        for (Parameter param : parameters()) {
            position++;
        }
        
        // Manufacture inputs for all the missing inputs; they'll get default bindings later...
        for (String portName : declInputs.keySet()) {
            Input dinput = declInputs.get(portName);
            Input input = getInput(portName);
            if (input == null) {
                runtime.finest(null, node, "Added " + portName + " input to " + getName());
                input = new Input(runtime, node);
                input.setPort(portName);
                input.setParameterInput(dinput.getParameterInput());
                if (dinput.getPrimarySet()) {
                    input.setPrimary(dinput.getPrimary());
                }
                input.setSequence(dinput.getSequence());
                input.setPosition(++position);
                addInput(input);
            } else {
                input.setParameterInput(dinput.getParameterInput());
                input.setPrimary(dinput.getPrimary());
                input.setSequence(dinput.getSequence());
            }
        }
        
        Hashtable<String,Output> declOutputs = new Hashtable<String,Output>();
        for (Output output : decl.outputs()) {
            declOutputs.put(output.getPort(), output);
        }

        // Manufacture outputs for all the missing outputs; they'll get default bindings later...
        for (String portName : declOutputs.keySet()) {
            Output doutput = declOutputs.get(portName);
            Output output = getOutput(portName);
            if (output == null) {
                runtime.finest(null, node, "Added " + portName + " output to " + getName());
                output = new Output(runtime, node);
                output.setPort(portName);
                output.setSequence(doutput.getSequence());
                if (doutput.getPrimarySet()) {
                    output.setPrimary(doutput.getPrimary());
                }
                addOutput(output);
            }
        }
    }

    protected void augmentOptions() {
        Step decl = declaration;
        
        if (decl == null) {
            throw new UnsupportedOperationException("Unexpected step type: " + getType());
        }

        Hashtable<QName,Option> declOptions = new Hashtable<QName,Option> ();
        for (Option option : decl.options()) {
            declOptions.put(option.getName(), option);
        }
        
        // Add any options that have default values...
        for (QName oname : declOptions.keySet()) {
            Option doption = declOptions.get(oname);
            Option option = getOption(oname);

            if (option == null) {
                if (doption.getSelect() != null || doption.getBinding().size() != 0) {
                    addOption(doption);
                }
            } else {
                option.setType(doption.getType(), doption.getNode()); // copy the type over
            }
        }
    }

    public void augment() {
        augmentIO();
        augmentOptions();
    }

    public void patchPipeBindings() {
        for (Input input : inputs) {
            //runtime.finest(null, node, "Patch " + input.getPort() + " on " + getName());
            patchInputBindings(input);
        }

        for (Parameter param : params) {
            //runtime.finest(null, node, "Patch " + input.getPort() + " on " + getName());
            patchInputBindings(param);
        }

        for (Option option : options) {
            //runtime.finest(null, node, "Patch " + input.getPort() + " on " + getName());
            patchInputBindings(option);
        }

        for (Variable var : getVariables()) {
            //runtime.finest(null, node, "Patch " + input.getPort() + " on " + getName());
            patchInputBindings(var);
        }

        for (Step step : subpipeline) {
            step.patchPipeBindings();
        }
    }

    protected void patchInputBindings(EndPoint endpoint) {
        Vector<Binding> bindings = endpoint.getBinding();
        for (int bpos = 0; bpos < bindings.size(); bpos++) {
            Binding binding = bindings.get(bpos);
            
            if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                PipeNameBinding pipename = (PipeNameBinding) binding;
                PipeBinding pipe = new PipeBinding(runtime, pipename.node);
                    
                Output source = env.readablePort(pipename.getStep(), pipename.getPort());
                pipe.setOutput(source);
                pipe.setInput(endpoint);
                
                //runtime.finest(null, node, "Patching " + pipename + " : " + pipe + " " + endpoint + " to " + source);
                    
                bindings.set(bpos, pipe);
                if (source != null) {
                    source.addBinding(pipe);
                }
            }
        }
    }
    
    protected void setEnvironment(Environment newEnvironment) {
        env = newEnvironment;
    }
    
    protected void patchEnvironment(Environment env) {
        // nop;
    }

    public Environment getEnvironment() {
        return env;
    }

    @Override
    public String toString() {
        String str = null;
        if (stepName.startsWith("#")) {
            str = "anonymous step " + stepType;
        } else {
            str = "step " + stepType + " named " + stepName;
        }
        if (node.getLineNumber() > 0) {
            str += " at " + node.getDocumentURI() + ":" + node.getLineNumber();
        } else {
            str += " in " + node.getDocumentURI();
        }
        return str;
    }

    public void dump() {
        System.err.println("============================================================================");
        dump(0);
        System.err.println("");
    }
    
    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        if (getType().getNamespaceURI().equals(XProcConstants.NS_XPROC)) {
            System.err.println(indent + getType().getLocalName() + " " + getName());
        } else {
            System.err.println(indent + "XProcStep " + getName() + " (" + getType() + ")");
        }
        
        for (Input input : inputs) {
            input.dump(depth+2);
        }
        for (Output output : outputs) {
            output.dump(depth+2);
        }
        for (Parameter param : params) {
            param.dump(depth+2);
        }
        for (Option option : options) {
            option.dump(depth+2);
        }
        for (Variable var : getVariables()) {
            var.dump(depth+2);
        }
        for (Step step : subpipeline) {
            step.dump(depth+2);
        }
    }
}
