package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadableEmpty;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.model.*;

import net.sf.saxon.s9api.SaxonApiException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcData;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 4:37:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class XChoose extends XCompoundStep {
    public XChoose(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }

    public void instantiate(Step step) {
        instantiateReaders(step);
        parent.addStep(this);

        DeclareStep decl = step.getDeclaration();

        for (Step substep : decl.subpipeline()) {
            if (XProcConstants.p_when.equals(substep.getType())) {
                XWhen newstep = new XWhen(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_otherwise.equals(substep.getType())) {
                XOtherwise newstep = new XOtherwise(runtime, substep, this);
                newstep.instantiate(substep);
            } else {
                throw new XProcException(step.getNode(), "This can't happen, can it? choose contains something that isn't a when or an otherwise?");
            }
        }

        for (Output output : step.outputs()) {
            String port = output.getPort();
            if (port.endsWith("|")) {
                String rport = port.substring(0,port.length()-1);
                XInput xinput = getInput(rport);
                WritablePipe wpipe = xinput.getWriter();
                outputs.put(port, wpipe);
                finest(step.getNode(), " writes to " + wpipe + " for " + port);
            } else {
                XOutput xoutput = new XOutput(runtime, output);
                addOutput(xoutput);
                WritablePipe wpipe = xoutput.getWriter();
                outputs.put(port, wpipe);
                finest(step.getNode(), " writes to " + wpipe + " for " + port);
            }
        }
    }


    public ReadablePipe getBinding(String stepName, String portName) {
        if (name.equals(stepName) && "#xpath-context".equals(portName)) {
            // FIXME: Check that .get(0) works, and that there's no sequence
            Vector<ReadablePipe> xpc = inputs.get("#xpath-context");
            if (xpc.size() == 0) {
                // If there's no binding for a p:choose, the default is an empty binding...
                return new ReadableEmpty();
            }
            ReadablePipe pipe = xpc.get(0);  
            return new Pipe(runtime, pipe.documents());
        } else {
            return super.getBinding(stepName, portName);
        }
    }

    public void run() throws SaxonApiException {
        // N.B. At this time, there are no compound steps that accept parameters or options,
        // so the order in which we calculate them doesn't matter. That will change if/when
        // there are such compound steps.
        
        // Don't reset iteration-position and iteration-size
        XProcData data = runtime.getXProcData();
        int ipos = data.getIterationPosition();
        int isize = data.getIterationSize();
        data.openFrame(this);
        data.setIterationPosition(ipos);
        data.setIterationSize(isize);

        inScopeOptions = parent.getInScopeOptions();
        for (Variable var : step.getVariables()) {
            RuntimeValue value = computeValue(var);
            inScopeOptions.put(var.getName(), value);
        }

        runtime.start(this);

        XCompoundStep xstep = null;
        for (XStep step : subpipeline) {
            if (step instanceof XWhen) {
                XWhen when = (XWhen) step;
                if (when.shouldRun()) {
                    xstep = when;
                    break;
                }
            } else {
                // Must be an otherwise
                xstep = (XOtherwise) step;
                break;
            }
        }

        if (xstep == null) {
            throw XProcException.dynamicError(4);
        }

        for (String port : inputs.keySet()) {
            if (!port.startsWith("|") && !"#xpath-context".equals(port)) {
                xstep.inputs.put(port, inputs.get(port));
            }
        }

        for (String port : outputs.keySet()) {
            if (!port.endsWith("|")) {
                xstep.outputs.put(port, outputs.get(port));
            }
        }

        try {
            xstep.run();
        } finally {
            runtime.finish(this);
            data.closeFrame();
        }
    }
}
