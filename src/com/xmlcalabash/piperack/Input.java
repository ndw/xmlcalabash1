package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.io.ReadableDocument;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.runtime.XPipeline;
import net.sf.saxon.s9api.XdmNode;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.xml.sax.InputSource;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Input extends BaseResource {
    @Override
    protected Representation post(Representation entity, Variant variant) {
        String id = (String) getRequest().getAttributes().get("id");
        if (!getPipelines().containsKey(id)) {
            return badRequest(Status.CLIENT_ERROR_NOT_FOUND, "no pipeline: " + pipelineUri(id), variant.getMediaType());
        }

        String port = (String) getRequest().getAttributes().get("port");

        PipelineConfiguration pipeconfig = getPipelines().get(id);
        XPipeline xpipeline = pipeconfig.pipeline;
        XProcRuntime runtime = pipeconfig.runtime;

        if (pipeconfig.ran) {
            pipeconfig.reset();
            xpipeline.reset();
        }

        if (pipeconfig.documentCount(port) == 0) {
            xpipeline.clearInputs(port);
        }
        pipeconfig.writeTo(port);

        try {
            XdmNode doc = null;

            if (isXml(entity.getMediaType())) {
                doc = runtime.parse(new InputSource(entity.getStream()));
            } else {
                ReadablePipe pipe = null;
                pipe = new ReadableData(runtime, XProcConstants.c_data, entity.getStream(), variant.getMediaType().toString());
                doc = pipe.read();
            }
            xpipeline.writeTo(port, doc);
        } catch (Exception e) {
            throw new XProcException(e);
        }

        return okResponse("Added document to the '" + port + "' port.", variant.getMediaType(), Status.SUCCESS_ACCEPTED);
    }

}
