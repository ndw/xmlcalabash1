package com.xmlcalabash.piperack;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Output extends BaseResource {
    @Override
    protected Representation get(Variant variant) {
        String id = (String) getRequest().getAttributes().get("id");
        if (!getPipelines().containsKey(id)) {
            return badRequest(Status.CLIENT_ERROR_NOT_FOUND, "no pipeline: " + pipelineUri(id), variant.getMediaType());
        }

        String port = (String) getRequest().getAttributes().get("port");

        if (getPipelines().containsKey(id)) {
            PipelineConfiguration pipeconfig = getPipelines().get(id);
            return getOutput(pipeconfig, port);
        } else {
            return badRequest(Status.CLIENT_ERROR_BAD_REQUEST, "no pipe: " + pipelineUri(id), MediaType.APPLICATION_XML);
        }
    }
}