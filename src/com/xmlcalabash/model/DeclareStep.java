/*
 * DeclareStep.java
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

import com.xmlcalabash.core.XProcData;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.core.XProcRuntime;

import java.util.Collection;
import java.util.Vector;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;

public class DeclareStep extends CompoundStep {
    protected boolean psviRequired = false;
    protected String xpathVersion = "2.0";
    private QName declaredType = null;
    private boolean atomic = true;
    private Pipeline implementation = null;
    protected Hashtable<QName, DeclareStep> declaredSteps = new Hashtable<QName, DeclareStep> ();
    protected HashSet<String> importedLibs = new HashSet<String> ();
    private DeclareStep parentDecl = null;
    private Vector<XdmNode> rest = null;
    private HashSet<String> excludedInlineNamespaces = null;

    // If a pipeline contains both import statements and inlined step declarations
    // then we have to be careful not to parse declared steps twice (we will have
    // parsed the imported ones, but not the inlined ones. This flag keeps track
    // of whether we've parsed the body of a step declaration or not.
    // FIXME: Maybe this should be managed by the parser not the DeclareStep?
    private boolean bodyParsed = false;

    /** Creates a new instance of DeclareStep */
    public DeclareStep(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, XProcConstants.p_declare_step, name);
    }

    protected void setXmlContent(Vector<XdmNode> nodes) {
        rest = nodes;
    }

    protected Vector<XdmNode> getXmlContent() {
        return rest;
    }

    public boolean getBodyParsed() {
        return bodyParsed;
    }

    public void setBodyParsed(boolean parsed) {
        bodyParsed = parsed;
    }

    public void setPsviRequired(boolean psvi) {
        psviRequired = psvi;
    }

    public void setXPathVersion(String version) {
        xpathVersion = version;
    }

    public void setDeclaredType(QName type) {
        declaredType = type;
    }

    public void setExcludeInlineNamespaces(HashSet<String> uris) {
        excludedInlineNamespaces = uris;
    }

    public HashSet<String> getExcludeInlineNamespaces() {
        return excludedInlineNamespaces;
    }

    public void setAtomic(boolean isAtomic) {
        atomic = isAtomic;
    }

    public boolean isAtomic() {
        return atomic;
    }

    public boolean isPipeline() {
        return !atomic;
    }

    public QName getDeclaredType() {
        return declaredType;
    }

    public void setParentDecl(DeclareStep decl) {
        parentDecl = decl;
    }

    public void setPipeline(Pipeline pipeline) {
        implementation = pipeline;
    }

    public Pipeline getPipeline() {
        return implementation;
    }

    public void declareStep(QName type, DeclareStep step) {
        if (declaredSteps.containsKey(type)) {
            throw new XProcException(step, "Duplicate step type: " + type);
        } else {
            declaredSteps.put(type, step);
        }
    }

    public boolean imported(String uri) {
        if (importedLibs.contains(uri)) {
            return true;
        }


        if (parentDecl == null) {
            return false;
        }

        return parentDecl.imported(uri);
    }

    public void addImport(String uri) {
        importedLibs.add(uri);
    }

    public DeclareStep getDeclaration() {
        return getStepDeclaration(declaredType);
    }

    public DeclareStep getStepDeclaration(QName type) {
        if (declaredSteps.containsKey(type)) {
            return declaredSteps.get(type);
        } else if (parentDecl != null) {
            return parentDecl.getStepDeclaration(type);
        } else {
            return runtime.getBuiltinDeclaration(type);
        }
    }
    
    public Collection<DeclareStep> getStepDeclarations() {
        return declaredSteps.values();
    }

    public void setupEnvironment() {
        setEnvironment(new Environment(this));
    }

    protected void patchEnvironment(Environment env) {
        if (atomic) {
            //nop;
        } else {
            // See if there's exactly one "ordinary" input
            int count = 0;
            Input defin = null;
            boolean foundPrimary = false;
            for (Input input : inputs) {
                if (!input.getPort().startsWith("|") && !input.getParameterInput()) {
                    count++;
                    foundPrimary |= input.getPrimary();

                    if (!input.getPrimary() && input.getPrimarySet()) {
                        // nop; if the port is explicitly marked primary=false, it can't count
                    } else {
                        if (defin == null || input.getPrimary()) {
                            defin = input;
                        }
                    }
                }
            }

            if (count == 1 || foundPrimary) {
                env.setDefaultReadablePort(defin);
            }
        }
    }

    private int logLevel(Logger logger) {
        Logger log = logger;
        Level level = null;

        if (log != null) {
            level = log.getLevel();
        }

        while (log != null && level == null) {
            log = log.getParent();
            level = log.getLevel();
        }

        if (level == null) {
            // WTF!?
            return Level.SEVERE.intValue();
        } else {
            return level.intValue();
        }
    }

    public void setup() {
        XProcRuntime runtime = this.runtime;
        DeclareStep decl = this;
        boolean debug = runtime.getDebug();

        if (decl.psviRequired && !runtime.getPSVISupported()) {
            throw XProcException.dynamicError(22);
        }
        
        if (debug && logLevel(logger) <= Level.FINEST.intValue()) {
            System.err.println("=====================================================================================");
            System.err.println("Before augment:");
            decl.dump();
        }

        boolean seenPrimaryDocument = false;
        boolean seenPrimaryParameter = false;
        for (Input input : decl.inputs()) {
            if (!input.getPort().startsWith("|") && input.getPrimary()) {
                if (seenPrimaryDocument && !input.getParameterInput()) {
                    error("At most one primary document input port is allowed", XProcConstants.staticError(30));
                }
                if (seenPrimaryParameter && input.getParameterInput()) {
                    error("At most one primary parameter input port is allowed", XProcConstants.staticError(30));
                }

                if (input.getParameterInput()) {
                    seenPrimaryParameter = true;
                } else {
                    seenPrimaryDocument = true;
                }
            }
        }

        boolean seenPrimary = false;
        for (Output output : decl.outputs()) {
            if (!output.getPort().endsWith("|") && output.getPrimary()) {
                if (seenPrimary) {
                    error("At most one primary output port is allowed", XProcConstants.staticError(30));
                }
                seenPrimary = true;
            }
        }

        if (debug && logLevel(logger) <= Level.FINEST.intValue()) {
            System.err.println("After binding pipeline inputs and outputs:");
            decl.dump();
        }

        if (subpipeline.size() == 0) {
            error("Declared step has no subpipeline, but is not known.", XProcConstants.staticError(100)); // FIXME!
            return;
        }

        decl.augment();

        if (debug && logLevel(logger) <= Level.FINEST.intValue()) {
            System.err.println("After augment:");
            decl.dump();
        }

        decl.setupEnvironment();

        if (!decl.valid()) {
            if (logLevel(logger) <= Level.INFO.intValue()) {
                decl.dump();
            }
            return;
        }

        if (debug && logLevel(logger) <= Level.FINEST.intValue()) {
            System.err.println("After valid:");
            decl.dump();
        }

        if (!decl.orderSteps()) {
            if (logLevel(logger) <= Level.INFO.intValue()) {
                decl.dump();
            }
            return;
        }

        if (debug && logLevel(logger) <= Level.FINEST.intValue()) {
            System.err.println("After ordering:");
            decl.dump();
        }

        HashSet<QName> vars = new HashSet<QName> ();
        checkDuplicateVars(vars);

        // Are all the primary outputs bound?
        if (!checkOutputBindings()) {
            if (logLevel(logger) <= Level.INFO.intValue()) {
                decl.dump();
            }
            return;
        }
    }

    protected boolean checkOutputBindings() {
        HashSet<Output> uboutputs = new HashSet<Output> ();

        for (Step substep : subpipeline) {
            for (Output output : substep.outputs()) {
                if (output.getBinding().size() == 0
                        && !output.getPort().endsWith("|") && !output.getPort().startsWith("#")) {
                    uboutputs.add(output);
                }
            }
        }

        for (Input input : inputs()) {
            for (Binding binding : input.bindings) {
                if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                    PipeNameBinding b = (PipeNameBinding) binding;
                    Output output = env.readablePort(b.getStep(), b.getPort());
                    if (uboutputs.contains(output)) {
                        uboutputs.remove(output);
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
                    if (uboutputs.contains(output)) {
                        uboutputs.remove(output);
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
                    if (uboutputs.contains(output)) {
                        uboutputs.remove(output);
                    } else {
                        // Doesn't matter. Must be legit but doesn't help us.
                    }
                }
            }
        }

        for (Step substep : subpipeline) {
            substep.checkForBindings(uboutputs);
        }

        boolean valid = true;
        Iterator<Output> outputIter = uboutputs.iterator();
        while (outputIter.hasNext()) {
            Output output = outputIter.next();
            if (output.getPrimary()) {
                error("Unbound primary output: " + output, new QName("", "ERR"));
                valid = false;
            }
        }

        return valid;
    }

    protected boolean checkBinding(Input input) {
        boolean valid = true;

        // Note: it's ok for there to be no input bindings on a declare-step; the
        // bindings come from the caller

        if (input.getBinding().size() == 0) {
            Port port = null;

            if ("#xpath-context".equals(input.getPort())) {
                if (this instanceof When) {
                    // Manufacture the right port
                    port = new Port(runtime,getNode());
                    port.setStep(parent);
                    port.setPort("#xpath-context");
                } else {
                    port = env.getDefaultReadablePort();
                }
            }

            if ("#iteration-source".equals(input.getPort())
                    || "#viewport-source".equals(input.getPort())) {
                port = env.getParent().getDefaultReadablePort();
            }

            // Check if the declaration has a default binding for this port
            Vector<Binding> declBinding = null;
            // FIXME: is this right?
            if (XProcConstants.p_pipeline.equals(getType())) {
                Step decl = declaration;
                for (Input dinput : decl.inputs()) {
                    if (dinput.getPort().equals(input.getPort())) {
                        declBinding = dinput.getBinding();
                    }
                }
            }

            if (input.getPrimary() && input.getPort().startsWith("|") && subpipeline.size() > 0) {
                // This needs to be bound to the output of the last step.
                Step substep = subpipeline.get(subpipeline.size()-1);
                port = substep.getDefaultOutput();

                if (port == null) {
                    error("Output port '" + input.getPort().substring(1) + "' on " + getStep() + " unbound", XProcConstants.staticError(5));
                    valid = false;
                }
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
                    // FIXME: is this right?
                }
            } else {
                String stepName = port.getStep().getName();
                String portName = port.getPort();

                PipeNameBinding binding = new PipeNameBinding(runtime, node);
                binding.setStep(stepName);
                binding.setPort(portName);

                input.addBinding(binding);
            }
        } else if (input.getParameterInput()) {
            XProcData data = runtime.getXProcData();
            // If depth==0 then we're on a declare step and you aren't allowed to
            // provide default bindings for parameter input ports.
            if (data.getDepth() == 0 && input.getBinding().size() > 0) {
                throw XProcException.staticError(35, input.getNode(), "You must not specify bindings in this context.");
            }
        }

        for (Binding binding : input.getBinding()) {
            if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                PipeNameBinding pipe = (PipeNameBinding) binding;

                // FIXME: This seems like an ugly special case
                Step step = env.visibleStep(pipe.getStep());
                if ((step instanceof Catch && "error".equals(pipe.getPort()))
                        || (step instanceof Choose && "#xpath-context".equals(pipe.getPort()))) { 
                    // then that's ok
                } else {
                    Output output = env.readablePort(pipe.getStep(), pipe.getPort());
                    if (output == null) {
                        error("Unreadable port: " + pipe.getPort() + " on " + pipe.getStep(), XProcException.err_E0001);
                        valid = false;
                    }
                }
            }
        }

        return valid;
    }
}
