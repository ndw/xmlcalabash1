package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.runtime.XPipeline;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class PipelineConfiguration {
    public XProcRuntime runtime = null;
    public XPipeline pipeline = null;
    public HashMap<String, Integer> inputs = new HashMap<String, Integer> ();
    public HashMap<String, Vector<XdmNode>> outputs = new HashMap<String, Vector<XdmNode>> ();
    public HashMap<QName, String> options = new HashMap<QName, String> ();
    public HashSet<QName> gvOptions = new HashSet<QName> ();
    public HashMap<QName, String> parameters = new HashMap<QName, String> ();
    public HashSet<QName> gvParameters = new HashSet<QName> ();
    public HashSet<String> inputPorts = new HashSet<String> ();
    public HashSet<String> outputPorts = new HashSet<String> ();
    public String definput = null;
    public String defoutput = null;
    public boolean ran = false;
    public Calendar expires = null;

    public PipelineConfiguration(XProcRuntime runtime, XPipeline xpipeline, Calendar expires) {
        this.runtime = runtime;
        this.pipeline = xpipeline;
        this.expires = expires;

        DeclareStep pipeline = xpipeline.getDeclareStep();

        // Figure out the default input port
        for (String port : xpipeline.getInputs()) {
            inputPorts.add(port);
            com.xmlcalabash.model.Input input = pipeline.getInput(port);
            if (!input.getParameterInput() && input.getPrimary()) {
                definput = port;
            }
        }

        // Figure out the default output port
        for (String port : xpipeline.getOutputs()) {
            outputPorts.add(port);
            com.xmlcalabash.model.Output output = pipeline.getOutput(port);
            if (output.getPrimary()) {
                defoutput = port;
            }
        }
    }

    public void reset() {
        ran = false;
        outputs.clear();
        inputs.clear();
        options.clear();
        gvOptions.clear();
        parameters.clear();
        gvParameters.clear();
    }

    public void writeTo(String port) {
        int count = 0;
        if (inputs.containsKey(port)) {
            count = inputs.get(port);
        }
        inputs.put(port, new Integer(count + 1));
    }

    public int documentCount(String port) {
        if (inputs.containsKey(port)) {
            return inputs.get(port);
        } else {
            return 0;
        }
    }

    public void setOption(QName name, String value) {
        options.put(name, value);
    }

    public void setGVOption(QName name) {
        gvOptions.add(name);
    }

    public void setParameter(QName name, String value) {
        parameters.put(name, value);
    }

    public void setGVParameter(QName name) {
        gvParameters.add(name);
    }
}
