package com.xmlcalabash.runtime;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Input;
import com.xmlcalabash.model.DeclareStep;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 7, 2008
 * Time: 8:02:28 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class XStep {
    protected XProcRuntime runtime = null;
    protected Step step = null;
    protected String name = null;
    private Hashtable<String,XInput> inputs = new Hashtable<String,XInput> ();
    private Hashtable<String,XOutput> outputs = new Hashtable<String,XOutput> ();
    private Hashtable<QName, RuntimeValue> options = new Hashtable<QName, RuntimeValue> ();
    private Hashtable<String, Hashtable<QName, RuntimeValue>> parameters = new Hashtable<String, Hashtable<QName, RuntimeValue>> ();
    protected XCompoundStep parent = null;
    public static final RuntimeValue unboundVariable = new RuntimeValue("Random string");
    protected Logger logger = Logger.getLogger(this.getClass().getName());
    protected Hashtable<QName,RuntimeValue> inScopeOptions = new Hashtable<QName,RuntimeValue> ();

    public XStep(XProcRuntime runtime, Step step) {
        this.runtime = runtime;
        this.step = step;
        if (step != null) {
            name = step.getName();
        }
    }

    public XdmNode getNode() {
        return step.getNode();
    }

    public String getName() {
        return name;
    }

    public DeclareStep getDeclareStep() {
        return step.getDeclaration();
    }

    public XCompoundStep getParent() {
        return parent;
    }

    public void addInput(XInput input) {
        String port = input.getPort();
        if (inputs.containsKey(port)) {
            throw new XProcException("Attempt to add output '" + port + "' port to the same step twice.");
        }
        inputs.put(port, input);
    }

    public void addOutput(XOutput output) {
        String port = output.getPort();
        if (outputs.containsKey(port)) {
            throw new XProcException("Attempt to add output '" + port + "' port to the same step twice.");
        }
        outputs.put(port, output);
    }

    public XInput getInput(String port) {
        if (inputs.containsKey(port)) {
            return inputs.get(port);
        } else {
            throw new XProcException("Attempt to get non-existant input '" + port + "' port from step.");
        }
    }

    public XOutput getOutput(String port) {
        if (outputs.containsKey(port)) {
            return outputs.get(port);
        } else {
            throw new XProcException("Attempt to get non-existant output '" + port + "' port from step.");
        }
    }

    public void setParameter(QName name, RuntimeValue value) {
        Set<String> ports = getParameterPorts();
        int pportCount = 0;
        String pport = null;
        for (String port : ports) {
            pport = port;
            pportCount++;
        }

        if (pportCount == 0) {
            throw new XProcException("Attempt to set parameter but there's no parameter port.");
        }

        if (pportCount > 1) {
            throw new XProcException("Attempt to set parameter w/o specifying a port (and there's more than one)");
        }

        setParameter(pport, name, value);
    }

    public void setParameter(String port, QName name, RuntimeValue value) {
        Hashtable<QName,RuntimeValue> pparams;
        if (parameters.containsKey(port)) {
            pparams = parameters.get(port);
        } else {
            pparams = new Hashtable<QName,RuntimeValue> ();
            parameters.put(port, pparams);
        }

        if (pparams.containsKey(name)) {
            throw new XProcException("Dup parameter");
        }

        if (XProcConstants.NS_XPROC.equals(name.getNamespaceURI())) {
            throw XProcException.dynamicError(31);
        }

        pparams.put(name, value);

    }

    public Set<QName> getOptions() {
        return options.keySet();
    }

    public RuntimeValue getOption(QName name) {
        if (options.containsKey(name)) {
            return options.get(name);
        } else {
            return null;
        }
    }

    public void setOption(QName name, RuntimeValue value) {
        if (options.containsKey(name)) {
            throw new XProcException("Dup option");
        }
        options.put(name, value);
    }

    public void clearOptions() {
        options.clear();
    }

    public Set<QName> getParameters() {
        return getParameters("*");
    }

    public RuntimeValue getParameter(QName name) {
        Set<String> ports = getParameterPorts();
        int pportCount = 0;
        String pport = null;
        for (String port : ports) {
            pport = port;
            pportCount++;
        }

        if (pportCount != 1) {
            return null;
        }

        return getParameter(pport, name);
    }

    public Set<String> getParameterPorts() {
        HashSet<String> ports = new HashSet<String> ();
        for (Input input : step.inputs()) {
            if (input.getParameterInput()) {
                ports.add(input.getPort());
            }

        }
        return ports;
    }

    public Set<QName> getParameters(String port) {
        if (parameters.containsKey(port)) {
            return parameters.get(port).keySet();
        } else {
            return new HashSet<QName> ();
        }
    }

    public RuntimeValue getParameter(String port, QName name) {
        if (parameters.containsKey(port)) {
            Hashtable<QName,RuntimeValue> pparams = parameters.get(port);
            if (pparams.containsKey(name)) {
                return pparams.get(name);
            }
        }
        return null;
    }

    public String getExtensionAttribute(QName name) {
        if (step != null) {
            return step.getExtensionAttribute(name);
        } else {
            return null;
        }
    }

    public String getInheritedExtensionAttribute(QName name) {
        if (getExtensionAttribute(name) != null) {
            return getExtensionAttribute(name);
        }
        if (parent != null) {
            return parent.getInheritedExtensionAttribute(name);
        }
        return null;
    }

    public boolean hasInScopeVariableBinding(QName name) {
        if (inScopeOptions.containsKey(name)) {
            return true;
        }

        return getParent() == null ? false : getParent().hasInScopeVariableBinding(name);
    }

    public boolean hasInScopeVariableValue(QName name) {
        if (inScopeOptions.containsKey(name)) {
            RuntimeValue v = getOption(name);
            return v != null;
        }

        return getParent() == null ? false : getParent().hasInScopeVariableBinding(name);
    }

    public Hashtable<QName,RuntimeValue> getInScopeOptions() {
        // We make a copy so that what our children do can't effect us
        Hashtable<QName,RuntimeValue> globals = new Hashtable<QName,RuntimeValue> ();
        if (inScopeOptions != null) {
            for (QName name : inScopeOptions.keySet()) {
                globals.put(name,inScopeOptions.get(name));
            }
        }
        return globals;
    }

    public abstract RuntimeValue optionAvailable(QName optName);
    public abstract void instantiate(Step step);
    public abstract void reset();
    public abstract void run() throws SaxonApiException;

    public void error(XdmNode node, String message, QName code) {
        runtime.error(logger, node, message, code);
    }

    public void warning(XdmNode node, String message) {
        runtime.warning(logger, node, message);
    }

    public void info(XdmNode node, String message) {
        runtime.info(logger, node, message);
    }

    public void fine(XdmNode node, String message) {
        runtime.fine(logger, node, message);
    }

    public void finer(XdmNode node, String message) {
        runtime.fine(logger, node, message);
    }

    public void finest(XdmNode node, String message) {
        runtime.fine(logger, node, message);
    }
}
