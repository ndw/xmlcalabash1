package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcData;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Variable;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

import java.util.Iterator;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 14, 2008
 * Time: 5:44:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class XUntilUnchanged extends XCompoundStep {
    private static final QName doca = new QName("","doca");
    private static final QName docb = new QName("","docb");

    private Pipe current = null;
    private int sequencePosition = 0;
    private int sequenceLength = 0;

    public XUntilUnchanged(XProcRuntime runtime, Step step, XCompoundStep parent) {
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
        fine(null, "Running cx:until-unchanged " + step.getName());

        XProcData data = runtime.getXProcData();
        data.openFrame(this);

        if (current == null) {
            current = new Pipe(runtime);
        }

        String iport = "#iteration-source";

        sequencePosition = 0;
        sequenceLength = 1;

        inScopeOptions = parent.getInScopeOptions();

        runtime.getXProcData().setIterationSize(sequenceLength);

        String iPortName = null;
        String oPortName = null;
        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                iPortName = port;
                oPortName = port.substring(1);
            }
        }

        runtime.start(this);
        for (ReadablePipe is_reader : inputs.get(iport)) {
            XdmNode os_doc = null;

            while (is_reader.moreDocuments()) {
                XdmNode is_doc = is_reader.read();
                boolean changed = true;

                while (changed) {
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

                    int docsCopied = 0;

                    for (ReadablePipe reader : inputs.get(iPortName)) {
                        while (reader.moreDocuments()) {
                            os_doc = reader.read();
                            docsCopied++;
                        }
                        reader.resetReader();
                    }

                    if (docsCopied != 1) {
                        throw XProcException.dynamicError(6);
                    }

                    for (XStep step : subpipeline) {
                        step.reset();
                    }

                    XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
                    xcomp.declareVariable(doca);
                    xcomp.declareVariable(docb);

                    XPathExecutable xexec = xcomp.compile("deep-equal($doca,$docb)");
                    XPathSelector selector = xexec.load();

                    selector.setVariable(doca, is_doc);
                    selector.setVariable(docb, os_doc);

                    Iterator<XdmItem> values = selector.iterator();
                    XdmAtomicValue item = (XdmAtomicValue) values.next();
                    changed = !item.getBooleanValue();

                    is_doc = os_doc;
                }

                WritablePipe pipe = outputs.get(oPortName);
                pipe.write(os_doc);
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