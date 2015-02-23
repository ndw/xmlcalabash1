package com.xmlcalabash.piperack;

import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;
import net.sf.saxon.s9api.QName;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

import java.util.HashMap;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Parameters extends BaseResource {
    @Override
    protected Representation post(Representation entity, Variant variant) {
        String id = (String) getRequest().getAttributes().get("id");
        if (!getPipelines().containsKey(id)) {
            return badRequest(Status.CLIENT_ERROR_NOT_FOUND, "no pipeline: " + pipelineUri(id), variant.getMediaType());
        }

        PipelineConfiguration pipeconfig = getPipelines().get(id);
        XPipeline xpipeline = pipeconfig.pipeline;

        String port = (String) getRequest().getAttributes().get("port");
        if (port == null) {
            // Figure out the default parameter port
            DeclareStep pipeline = xpipeline.getDeclareStep();
            for (String iport : xpipeline.getInputs()) {
                com.xmlcalabash.model.Input input = pipeline.getInput(iport);
                if (input.getParameterInput() && input.getPrimary()) {
                    port = iport;
                }
            }
        }

        if (port == null) {
            return badRequest(Status.CLIENT_ERROR_BAD_REQUEST, "No primary parameter input port", variant.getMediaType());
        }

        String message = "Parameters added: ";
        boolean first = true;

        HashMap<QName,String> params = convertForm(getQuery());

        for (QName name : params.keySet()) {
            RuntimeValue value = new RuntimeValue(params.get(name), null, null);
            xpipeline.setParameter(port, name, value);
            pipeconfig.setParameter(name, params.get(name));
            if (!first) {
                message += ", ";
            }
            message += name;
            first = false;
        }

        return okResponse(message, variant.getMediaType(), Status.SUCCESS_ACCEPTED);
    }
}