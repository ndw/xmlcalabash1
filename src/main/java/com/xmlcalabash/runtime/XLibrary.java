package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.model.ErrorBinding;
import com.xmlcalabash.model.Input;
import com.xmlcalabash.model.Output;
import com.xmlcalabash.model.PipeNameBinding;
import com.xmlcalabash.model.Pipeline;
import com.xmlcalabash.model.PipelineLibrary;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.Step;
import net.sf.saxon.s9api.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 20, 2008
 * Time: 8:55:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class XLibrary {
    private XProcRuntime runtime = null;
    private PipelineLibrary library = null;
    private Logger logger = LoggerFactory.getLogger(XLibrary.class);

    public XLibrary(XProcRuntime runtime, PipelineLibrary library) {
        this.runtime = runtime;
        this.library = library;
    }

    public XPipeline getFirstPipeline() {
        QName name = library.firstStep();
        return getPipeline(name);
    }

    public QName getFirstPipelineType() {
        return library.firstStep();
    }

    public XPipeline getPipeline(QName stepName) {
        DeclareStep step = library.getDeclaration(stepName);

        if (step == null) {
            runtime.error(null, library.getNode(), "No step named " + stepName + " in library.", null);
            return null;
        }

        XRootStep root = new XRootStep(runtime);

        if (step.subpipeline().size() == 0) {
            // I need to instantiate an atomic step to replace this declaration in the
            // pipeline I'm about to create.

            Step atomicReplacement = new Step(runtime, step.getNode(),step.getDeclaredType(),step.getName());
            atomicReplacement.setDeclaration(step);

            // TODO: Make sure options and parameters get copied over correctly!

            String wrapper = "XML-CALABASH-GENERATED-WRAPPER-PIPELINE";
            QName ptype = new QName("", "XML-CALABASH-WRAPPER-TYPE");
            // This is an atomic step, manufacture a dummy wrapper pipeline for it.
            DeclareStep pipeline = new DeclareStep(runtime, step.getNode(), wrapper);
            for (Input input : step.inputs()) {
                Input pInput = new Input(runtime, input.getNode());
                pInput.setPort(input.getPort());
                pInput.setPrimary(input.getPrimary());
                pInput.setSequence(input.getSequence());
                pInput.setParameterInput(input.getParameterInput());
                pipeline.addInput(pInput);

                PipeNameBinding pnb = new PipeNameBinding(runtime, input.getNode());
                pnb.setStep(wrapper);
                pnb.setPort(pInput.getPort());
                input.addBinding(pnb);

                atomicReplacement.addInput(input);
            }

            for (Output output : step.outputs()) {
                Output pOutput = new Output(runtime, output.getNode());
                pOutput.setPort(output.getPort());
                pOutput.setPrimary(output.getPrimary());
                pOutput.setSequence(output.getSequence());

                Input pInput = new Input(runtime, output.getNode());
                pInput.setPort("|" + output.getPort());
                pInput.setSequence(output.getSequence());
                pipeline.addInput(pInput);

                PipeNameBinding pnb = new PipeNameBinding(runtime, output.getNode());
                pnb.setStep(step.getName());
                pnb.setPort(output.getPort());
                pInput.addBinding(pnb);

                pipeline.addOutput(pOutput);

                atomicReplacement.addOutput(output);
            }

            pipeline.addStep(atomicReplacement);
            pipeline.setDeclaredType(ptype);
            runtime.declareStep(ptype,pipeline);
            step = pipeline;
            step.setup();
        } else {
            step.setup();
        }

        XPipeline xpipeline = new XPipeline(runtime, step, root);

        if (runtime.getErrorCode() != null) {
            throw new XProcException(runtime.getErrorCode(), runtime.getErrorMessage());
        }
        
        xpipeline.instantiate(step);

        return xpipeline;
    }
}
