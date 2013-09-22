package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.model.*;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.QName;

import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 5:25:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class XPipelineCall extends XAtomicStep {
    private DeclareStep decl = null;

    public XPipelineCall(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step, parent);
        this.parent = parent;
    }

    public void setDeclaration(DeclareStep decl) {
        this.decl = decl;
    }

    public XCompoundStep getParent() {
        return parent;
    }


    public void run() throws SaxonApiException {
        fine(null, "Running " + step.getType());

        decl.setup();

        if (runtime.getErrorCode() != null) {
            throw new XProcException(runtime.getErrorCode(), runtime.getErrorMessage());
        }

        XRootStep root = new XRootStep(runtime);
        XPipeline newstep = new XPipeline(runtime, decl, root);

        newstep.instantiate(decl);

        // Calculate all the options
        inScopeOptions = parent.getInScopeOptions();

        HashSet<QName> pipeOpts = new HashSet<QName> ();
        for (QName name : newstep.step.getOptions()) {
            pipeOpts.add(name);
        }

        for (QName name : step.getOptions()) {
            Option option = step.getOption(name);
            RuntimeValue value = computeValue(option);
            setOption(name, value);

            if (pipeOpts.contains(name)) {
                newstep.passOption(name, value);
            }

            inScopeOptions.put(name, value);
        }

        for (QName name : step.getParameters()) {
            Parameter param = step.getParameter(name);
            RuntimeValue value = computeValue(param);

            String port = param.getPort();
            if (port == null) {
                newstep.setParameter(name, value);
            } else {
                newstep.setParameter(port, name, value);
            }
        }

        for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                newstep.inputs.put(port, inputs.get(port));
            }
        }

        for (String port : outputs.keySet()) {
            if (!port.endsWith("|")) {
                newstep.outputs.put(port, outputs.get(port));
            }
        }

        newstep.run();

    }
}