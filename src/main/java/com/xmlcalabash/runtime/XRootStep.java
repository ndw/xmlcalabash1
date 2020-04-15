package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.*;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 5:26:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class XRootStep extends XCompoundStep {
    private Vector<XdmNode> errors = new Vector<XdmNode> ();

    public XRootStep(XProcRuntime runtime) {
        super(runtime, null, null);
    }

    public Hashtable<QName,RuntimeValue> getInScopeOptions() {
        return new Hashtable<QName,RuntimeValue> ();
    }

/*
    public void addVariable(QName name, RuntimeValue value) {
        throw new XProcException("The root step can't have getVariables!");
    }
*/

    public RuntimeValue getVariable(QName name) {
        throw new XProcException("The root step doesn't have getVariables!");
    }

    public ReadablePipe getBinding(String stepName, String portName) {
        throw new XProcException("No in-scope binding for " + portName + " on " + stepName);
    }

/*
    public void instantiate(DeclareStep step) {
        throw new XProcException("The root step can't be instantiated!");
    }
*/
    
    public void run() {
        throw new XProcException("The root step can't be run!");
    }

    public void reportError(XdmNode doc) {
        errors.add(doc);
    }
}