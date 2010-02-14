package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.model.PipelineLibrary;
import com.xmlcalabash.model.DeclareStep;
import net.sf.saxon.s9api.QName;

import java.util.logging.Logger;

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
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public XLibrary(XProcRuntime runtime, PipelineLibrary library) {
        this.runtime = runtime;
        this.library = library;
    }

    public XPipeline getFirstPipeline() {
        QName name = library.firstStep();
        return getPipeline(name);
    }

    public XPipeline getPipeline(QName stepName) {
        DeclareStep step = library.getDeclaration(stepName);

        if (step == null) {
            runtime.error(null, library.getNode(), "No step named " + stepName + " in library.", null);
            return null;
        }

        XRootStep root = new XRootStep(runtime);
        step.setup();

        XPipeline xpipeline = new XPipeline(runtime, step, root);

        runtime.phoneHome(xpipeline.getDeclareStep());

        if (runtime.getErrorCode() != null) {
            throw new XProcException(runtime.getErrorCode(), runtime.getErrorMessage());
        }
        
        xpipeline.instantiate(step);

        return xpipeline;
    }
}
