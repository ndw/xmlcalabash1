package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.*;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Binding;
import com.xmlcalabash.util.MessageFormatter;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.SaxonApiException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 7:44:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class XCatch extends XCompoundStep {
    Pipe errorPipe = null;

    public XCatch(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }

    public void writeError(XdmNode doc) {
        errorPipe.write(doc);
    }

    protected ReadablePipe getPipeFromBinding(Binding binding) {
        if (binding.getBindingType() == Binding.ERROR_BINDING) {
            errorPipe = new Pipe(runtime);
            return errorPipe;
        } else {
            return super.getPipeFromBinding(binding);
        }
    }

    public ReadablePipe getBinding(String stepName, String portName) {
        if (name.equals(stepName) && "error".equals(portName)) {
            return new Pipe(runtime,errorPipe.documents());
        } else {
            return super.getBinding(stepName, portName);
        }
    }

    protected void copyInputs() throws SaxonApiException {
        for (String port : inputs.keySet()) {
            if (!port.startsWith("|") && !"error".equals(port)) {
            String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments()) {
                        XdmNode doc = reader.read();
                        pipe.write(doc);
                        logger.trace(MessageFormatter.nodeMessage(step.getNode(), "Compound input copy from " + reader + " to " + pipe));
                    }
                }
            }
        }
    }

    public void reset() {
        super.reset();
        errorPipe.resetReader();
        errorPipe.resetWriter();
    }
}
