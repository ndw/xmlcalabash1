/*
 * CompoundStep.java
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
import java.util.Hashtable;
import java.util.Vector;
import java.util.HashSet;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class CompoundStep extends Step {
    private Environment inheritedEnv = null;
    private HashSet<QName> variablesSeen = new HashSet<QName> ();
    private Vector<Variable> variables = new Vector<Variable> (); // Order matters!!!
    private boolean augmented = false;
    
    /** Creates a new instance of CompoundStep */
    public CompoundStep(XProcRuntime xproc, XdmNode node, QName type, String name) {
        super(xproc, node, type, name);
    }

    public boolean containsStep(String stepName) {
        for (Step step : subpipeline) {
            if (stepName.equals(step.getName())) {
                return true;
            }
        }
        return false;
    }

    public void addVariable(Variable variable) {
        if (variablesSeen.contains(variable.getName()) || (getOption(variable.getName()) != null)) {
            throw XProcException.staticError(4, "Duplicate variable/option name: " + variable.getName());
        }

        variablesSeen.add(variable.getName());
        variables.add(variable);
    }

    public Collection<Variable> getVariables() {
        return variables;
    }

    @Override
    protected void setEnvironment(Environment newEnvironment) {
        Environment env = new Environment(newEnvironment);

        // Now what about my subpipeline?
        for (Step step : subpipeline) {
            env.addStep(step);
        }

        inheritedEnv = new Environment(env);
        
        patchEnvironment(env);
        
        super.setEnvironment(env);
        
        // Now what about my subpipeline
        Step lastStep = null;
        for (Step step : subpipeline) {
            Environment senv = new Environment(env);
            if (lastStep != null) {
                senv.setDefaultReadablePort(lastStep.getDefaultOutput());
            }
            lastStep = step;
            step.setEnvironment(senv);
        }
    }
    
    @Override
    public void augment() {
        if (augmented) {
            return;
        }
        augmented = true;
        
        // avoid a concurrent modification exception; I could use .clone()
        // but then I'd get a warning about an unchecked cast...
        Vector<Step> stepcopy = new Vector<Step> ();
        for (Step step : subpipeline) {
            stepcopy.add(step);
        }

        for (Step step : stepcopy) {
            step.augment();
        }
        
        super.augment();

        if (XProcConstants.p_declare_step.equals(this.getType())) {
            // You have to be explicit in p:declare-step
        } else {
            // Now, if this step has no outputs and the last step has a primary output,
            // then manufacture a primary output here too.
            if (outputs().size() == 0 || (outputs().size() == 1 && "#current".equals(outputs().get(0).getPort()))) {
                Step last = subpipeline.get(subpipeline.size()-1);
                Output primary = last.getPrimaryOutput();

                String portName = "!result";
                if (XProcConstants.p_viewport.equals(node.getNodeName())) {
                    // Viewport is magic...
                    portName = "result";
                }
            
                if (primary != null) {
                    Output output = new Output(runtime, node);
                    output.setPort(portName);
                    output.setPrimary(true);
                    // N.B. The output of a for-each can always produce a sequence!
                    output.setSequence(primary.getSequence() || XProcConstants.p_for_each.equals(this.getType()));
                    addOutput(output);
                
                    Input input = new Input(runtime, node);
                    input.setPort("|" + portName);
                    input.setSequence(primary.getSequence());
                    input.setPrimary(true);
                    addInput(input);
                }
            }
        }
    }

    @Override
    protected void augmentOptions() {
        // nop; don't try to copy from decl
    }

    @Override
    protected void augmentIO() {
        /*
        // Special case: if an *atomic* step is run directly, what we're looking at here is
        // its declaration which is *compound*. But we don't want to augment it's I/O like
        // a compound step because we've wrapped a pipeline around it and want to treat it
        // like an atomic step. This is a bit messy, I know.
        if (subpipeline.size() == 0) {
            return;
        }
        */

        // Output bindings on a compound step are really the input half of an input/output binding;
        // create the other half
        for (Output output : outputs) {
            if (!"#current".equals(output.getPort())) {
                Input input = getInput("|" + output.getPort());
                if (input == null) {
                    input = new Input(runtime, output.getNode());
                    input.setPort("|" + output.getPort());
                    input.setSequence(true); // the other half will check
                    input.setPrimary(output.getPrimary());
                    addInput(input);
                }
            }
        }

        // inputs on compound steps are really outputs from the point of view of the subpipeline
        for (Input input : inputs()) {
            if (!input.getPort().startsWith("|") && !input.getPort().startsWith("#")) {
                Output output = new Output(runtime, input.getNode());
                output.setPort(input.getPort() + "|");
                output.setSequence(true); // the other half will check
                output.setPrimary(input.getPrimary());
                addOutput(output);
            }
        }
    }

    public Output getOutput(String portName) {
        Output output = super.getOutput(portName);
        if (output != null) {
            return output;
        }
        
        // On compound steps, inputs are also readable by the contained steps.
        // Except that you can't really read from an input so we go looking for the corresponding
        // output.
        for (Input input : inputs) {
            if (portName.equals(input.getPort())) {
                return super.getOutput(portName + "|");
            }
        }

        return null;
    }

    @Override
    public void checkVariables() {
        for (Variable variable : variables) {
            for (Binding binding : variable.getBinding()) {
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding pipe = (PipeNameBinding) binding;
                    String name = pipe.getStep();
                    boolean ancestor = false;
                    Step step = this;
                    while (step != null && !ancestor) {
                        ancestor = name.equals(step.getName());
                        step = step.parent;
                    }
                    if (!ancestor) {
                        runtime.finest(null, node, getName() + " variable depends on " + pipe.getStep());
                        addDependency(pipe.getStep());
                    }
                }
            }
        }
    }

    protected boolean checkBinding(Input input) {
        boolean valid = true;

        //System.err.println("Check bindings for " + input.getPort() + " on " + getName());
        
        if (input.getBinding().size() == 0) {
            // On compound steps, 
            Port port = inheritedEnv.getDefaultReadablePort();

            // Check if the declaration has a default binding for this port
            Vector<Binding> declBinding = null;
            if (XProcConstants.p_pipeline.equals(getType())) {
                Step decl = declaration;
                for (Input dinput : decl.inputs()) {
                    if (dinput.getPort().equals(input.getPort())) {
                        declBinding = dinput.getBinding();
                    }
                }
            }

            if (input.getPort().startsWith("|") && subpipeline.size() > 0) {
                // This needs to be bound to the output of the last step.
                Step substep = subpipeline.get(subpipeline.size()-1);
                port = substep.getDefaultOutput();
            }

            // FIXME: Is this right? We don't want to steal the root input/output bindings, but
            // we do want to steal all the others, right?
            Output output = null;
            if (input.getPort().startsWith("|") && parent != null) {
                String oport = input.getPort().substring(1);
                output = getOutput(oport);
            }
            
            if (output != null && output.getBinding().size() > 0) {
                // For |result, we want to copy result's bindings over
                for (Binding binding : output.getBinding()) {
                    input.addBinding(binding);
                }
                output.clearBindings();
            } else if (port == null) {
                if (declBinding != null) {
                    for (Binding binding : declBinding) {
                        input.addBinding(binding);
                    }
                } else if (input.getParameterInput()) {
                    EmptyBinding empty = new EmptyBinding();
                    input.addBinding(empty);
                } else {
                    valid = false;
                    error("Input " + input.getPort() + " unbound on " + getType() + " step named " + getName() + " and no default binding available.", XProcConstants.staticError(32));
                }
            } else {
                String stepName = port.getStep().getName();
                String portName = port.getPort();

                PipeNameBinding binding = new PipeNameBinding(runtime, node);
                binding.setStep(stepName);
                binding.setPort(portName);

                input.addBinding(binding);
            }
        }
            
        for (Binding binding : input.getBinding()) {
            if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                PipeNameBinding pipe = (PipeNameBinding) binding;
                Output output = env.readablePort(pipe.getStep(), pipe.getPort());
                if (output == null) {
                    error("Unreadable port: " + pipe.getPort() + " on " + pipe.getStep(), XProcException.err_E0001);
                    valid = false;
                }
            }
        }
        
        return valid;
    }

    public void checkPrimaryIO() {
        checkPrimaryInput(false);
        checkPrimaryInput(true);
        checkPrimaryOutput();

        int count = 0;
        int pcount = 0;
        Port primary = null;
        Port defPrimary = null;
        for (Output output : outputs()) {
            if (!output.getPort().startsWith("#")) {
                count++;
                defPrimary = output;
            
                if (output.getPrimary()) {
                    pcount++;
                    primary = output;
                }
            }
        }
        
        if (pcount > 1) {
            throw XProcException.staticError(30);
        }

        if (count == 1 && (defPrimary.getPrimary() || !defPrimary.getPrimarySet())) {
            defPrimary.setPrimary(true);
        }
    }

    private void checkPrimaryInput(boolean checkParameterInput) {
        int count = 0;
        int pcount = 0;
        Port defPrimary = null;
        Port primary = null;

        for (Input input : inputs()) {
            if (!input.getPort().startsWith("|")) {
                count++;
                if (input.getParameterInput() == checkParameterInput) {
                    if (input.getPrimary()) {
                        pcount++;
                        if (primary == null) {
                            primary = input;
                        }
                    }

                    if (defPrimary == null && !input.getPrimarySet()) {
                        defPrimary = input;
                    }
                }
            }
        }

        if (pcount > 1) {
            throw XProcException.staticError(30);
        }

        if (count == 1 && primary == null && defPrimary != null) {
            if (defPrimary.getPrimary() || !defPrimary.getPrimarySet()) {
                defPrimary.setPrimary(true);
            }
        }
    }

    private void checkPrimaryOutput() {
        int count = 0;
        int pcount = 0;
        Port defPrimary = null;
        Port primary = null;

        for (Output output : outputs()) {
            if (!output.getPort().endsWith("|")) {
                count++;
                if (output.getPrimary()) {
                    pcount++;
                    if (primary == null) {
                        primary = output;
                    }
                }

                if (defPrimary == null && !output.getPrimarySet()) {
                    defPrimary = output;
                }
            }
        }

        if (pcount > 1) {
            throw XProcException.staticError(30);
        }

        if (count == 1 && primary == null && defPrimary != null) {
            if (defPrimary.getPrimary() || !defPrimary.getPrimarySet()) {
                defPrimary.setPrimary(true);
            }
        }
    }

    public boolean valid() {
        boolean valid = validParams();
        valid = valid && validOptions();
        valid = valid && validBindings();

        if (env.countVisibleSteps(getName()) > 1) {
            error("Duplicate step name: " + getName(), XProcConstants.staticError(2));
            valid = false;
        }

        for (Step step : subpipeline) {
            boolean stepValid = step.valid();
            valid = valid && stepValid;
        }

        valid = valid && validOutputBinding();

        return valid;
    }

    protected boolean validOutputBinding() {
        // Does the last step have a primary output, and is it bound?
        boolean ok = true;

        if (subpipeline.size() > 0) {
            Step substep = subpipeline.get(subpipeline.size()-1);
            Output port = substep.getDefaultOutput();
            if (port != null) {
                ok = false;
                for (Input input : inputs) {
                    if (input.getPort().startsWith("|")) {
                        for (Binding binding : input.getBinding()) {
                            if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                                PipeNameBinding b = (PipeNameBinding) binding;
                                ok = ok || (b.getStep().equals(substep.getName()) && port.getPort().equals(b.getPort()));
                            }
                        }
                    }
                }

                if (!ok) {
                    // What if one of the other steps in the subpipline reads it?
                    // That would be OK too...
                    for (int spos = 0; spos < subpipeline.size()-1; spos++) {
                        Step sibling = subpipeline.get(spos);
                        for (Input input : sibling.inputs()) {
                            for (Binding binding : input.getBinding()) {
                                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                                    PipeNameBinding b = (PipeNameBinding) binding;
                                    ok = ok || (b.getStep().equals(substep.getName()) && port.getPort().equals(b.getPort()));
                                }
                            }
                        }
                    }
                }


                if (!ok) {
                    error("Unbound primary output port on last step: " + getName(), XProcConstants.staticError(6));
                }
            }
        }

        return ok;
    }

    protected boolean validBindings() {
        boolean valid = super.validBindings();
        for (Variable var : getVariables()) {
            if (!checkOptionBinding(var, true)) {
                valid = false;
            }
        }

        return valid;
    }

    protected void checkForBindings(HashSet<Output> outputs) {
        super.checkForBindings(outputs);

        for (Variable var : getVariables()) {
            for (Binding binding : var.bindings) {
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

        for (Step substep : subpipeline) {
            substep.checkForBindings(outputs);
        }
    }

}
