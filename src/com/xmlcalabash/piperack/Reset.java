package com.xmlcalabash.piperack;

import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;
import net.sf.saxon.s9api.QName;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

import java.util.HashMap;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Reset extends BaseResource {
    @Override
    protected Representation post(Representation entity, Variant variant) {
        String id = (String) getRequest().getAttributes().get("id");
        if (!getPipelines().containsKey(id)) {
            return badRequest(Status.CLIENT_ERROR_NOT_FOUND, "no pipeline: " + pipelineUri(id), variant.getMediaType());
        }

        PipelineConfiguration pipeconfig = getPipelines().get(id);
        XPipeline xpipeline = pipeconfig.pipeline;

        pipeconfig.reset();
        xpipeline.reset();

        return okResponse("Pipeline reset", variant.getMediaType());
    }

}
