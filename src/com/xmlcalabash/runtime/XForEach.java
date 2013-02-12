package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcData;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Variable;
import com.xmlcalabash.model.Option;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 14, 2008
 * Time: 5:44:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class XForEach extends XCompoundStep {
    private Pipe current = null;
    private int sequencePosition = 0;
    private int sequenceLength = 0;

    public XForEach(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step, parent);
    }

    public ReadablePipe getBinding(String stepName, String portName) {
        if (name.equals(stepName) && ("#current".equals(portName) || "current".equals(portName))) {
            if (current == null) {
                current = new Pipe(runtime);
            }
            return new Pipe(runtime,current.documents());
        } else {
            return super.getBinding(stepName, portName);
        }
    }

    protected void copyInputs() throws SaxonApiException {
        // nop;
    }

    public void reset() {
        super.reset();
        sequenceLength = 0;
        sequencePosition = 0;
    }
    
    public void run() throws SaxonApiException {
        fine(null, "Running p:for-each " + step.getName());

        XProcData data = runtime.getXProcData();
        data.openFrame(this);

        if (current == null) {
            current = new Pipe(runtime);
        }

        String iport = "#iteration-source";

        sequencePosition = 0;
        sequenceLength = 0;

        inScopeOptions = parent.getInScopeOptions();

        // FIXME: Do I really have to do this? At the very least, only do it if we have to!
        Vector<XdmNode> nodes = new Vector<XdmNode> ();
        for (ReadablePipe is_reader : inputs.get(iport)) {
            while (is_reader.moreDocuments()) {
                XdmNode is_doc = is_reader.read();
                finest(step.getNode(), "Input copy from " + is_reader);
                finest(step.getNode(), is_doc.toString());
                nodes.add(is_doc);
                sequenceLength++;
            }
        }

        runtime.getXProcData().setIterationSize(sequenceLength);

        runtime.start(this);

        for (XdmNode is_doc : nodes) {
            // Setup the current port before we compute variables!
            current.resetWriter();
            current.write(is_doc);
            finest(step.getNode(), "Copy to current");

            sequencePosition++;
            runtime.getXProcData().setIterationPosition(sequencePosition);

            for (Variable var : step.getVariables()) {
                RuntimeValue value = computeValue(var);
                inScopeOptions.put(var.getName(), value);
            }

            // N.B. At this time, there are no compound steps that accept parameters or options,
            // so the order in which we calculate them doesn't matter. That will change if/when
            // there are such compound steps.

            // Calculate all the variables
            inScopeOptions = parent.getInScopeOptions();
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

                    boolean seqOk = step.getOutput(wport).getSequence();
                    int docsCopied = 0;

                    WritablePipe pipe = outputs.get(wport);
                    // The output of a for-each is a sequence, irrespective of what the output says
                    pipe.canWriteSequence(true);

                    for (ReadablePipe reader : inputs.get(port)) {
                        reader.canReadSequence(true); // Hack again!
                        while (reader.moreDocuments()) {
                            XdmNode doc = reader.read();
                            pipe.write(doc);
                            docsCopied++;
                            finest(step.getNode(), "Output copy from " + reader + " to " + pipe);
                        }
                        reader.resetReader();
                    }

                    if (docsCopied != 1 && !seqOk) {
                        throw XProcException.dynamicError(6);
                    }
                }
            }

            for (XStep step : subpipeline) {
                step.reset();
            }
        }

        runtime.finish(this);

        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                pipe.close(); // Indicate that we're done
            }
        }

        data.closeFrame();
    }
}
