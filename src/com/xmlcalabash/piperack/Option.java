package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.io.ReadableDocument;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.xml.sax.InputSource;

import java.util.HashMap;
import java.util.Vector;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Option extends BaseResource {
    @Override
    protected Representation post(Representation entity, Variant variant) {
        String id = (String) getRequest().getAttributes().get("id");
        if (!getPipelines().containsKey(id)) {
            return badRequest(Status.CLIENT_ERROR_NOT_FOUND, "no pipeline: " + pipelineUri(id), variant.getMediaType());
        }

        PipelineConfiguration pipeconfig = getPipelines().get(id);
        XProcRuntime runtime = pipeconfig.runtime;
        XPipeline xpipeline = pipeconfig.pipeline;

        String name = (String) getRequest().getAttributes().get("option");
        QName qname = qnameFromForm(name, getQuery());

        Vector<XdmItem> nodes = new Vector<XdmItem>();

        try {
            ReadablePipe pipe = null;

            if (isXml(entity.getMediaType())) {
                XdmNode doc = runtime.parse(new InputSource(entity.getStream()));
                pipe = new ReadableDocument(runtime, doc, null, null, null);
            } else {
                pipe = new ReadableData(runtime, XProcConstants.c_data, entity.getStream(), variant.getMediaType().toString());
            }

            while (pipe.moreDocuments()) {
                XdmNode doc = pipe.read();
                nodes.add(doc);
            }
        } catch (Exception e) {
            throw new XProcException(e);
        }

        String message = "Option added: " + qname.toString();

        RuntimeValue value = new RuntimeValue(null, nodes, null, null);
        xpipeline.passOption(qname, value);
        pipeconfig.setGVOption(qname);

        return okResponse(message, variant.getMediaType(), Status.SUCCESS_ACCEPTED);
    }
}