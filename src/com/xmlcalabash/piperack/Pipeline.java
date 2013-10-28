package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.io.ReadableDocument;
import com.xmlcalabash.io.ReadableInline;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.util.Series;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Pipeline extends BaseResource {
    @Override
    protected Representation get(Variant variant) {
        String id = (String) getRequest().getAttributes().get("id");
        if (!getPipelines().containsKey(id)) {
            return badRequest(Status.CLIENT_ERROR_NOT_FOUND, "no pipeline: " + pipelineUri(id), variant.getMediaType());
        }

        TreeWriter tree = new TreeWriter(getGlobalRuntime());
        tree.startDocument(URI.create("http://example.com/"));

        PipelineConfiguration pipeconfig = getPipelines().get(id);

        tree.addStartElement(pr_pipeline);
        tree.startContent();

        tree.addStartElement(pr_uri);
        tree.startContent();
        tree.addText(pipelineUri(id));
        tree.addEndElement();

        formatExpires(tree, pipeconfig.expires);

        tree.addStartElement(pr_has_run);
        tree.startContent();
        tree.addText("" + pipeconfig.ran);
        tree.addEndElement();

        if (pipeconfig.ran) {
            for (String port : pipeconfig.outputPorts) {
                tree.addStartElement(pr_output);
                if (port.equals(pipeconfig.defoutput)) {
                    tree.addAttribute(_primary, "true");
                }
                tree.addAttribute(_documents, "" + pipeconfig.outputs.get(port).size());
                tree.startContent();
                tree.addText(port);
                tree.addEndElement();
            }
        } else {
            XPipeline xpipeline = pipeconfig.pipeline;
            DeclareStep pipeline = xpipeline.getDeclareStep();

            for (String port : pipeconfig.inputPorts) {
                tree.addStartElement(pr_input);
                if (port.equals(pipeconfig.definput)) {
                    tree.addAttribute(_primary, "true");
                }
                tree.addAttribute(_documents, "" + pipeconfig.documentCount(port));
                tree.startContent();
                tree.addText(port);
                tree.addEndElement();
            }

            for (QName name : pipeline.getOptions()) {
                tree.addStartElement(pr_option);
                tree.startContent();

                tree.addStartElement(pr_name);

                if (!"".equals(name.getPrefix())) {
                    tree.addNamespace(name.getPrefix(), name.getNamespaceURI());
                }

                tree.startContent();
                tree.addText(name.toString());
                tree.addEndElement();

                tree.addStartElement(pr_value);
                if (pipeconfig.options.containsKey(name)) {
                    tree.startContent();
                    tree.addText(pipeconfig.options.get(name));
                } else if (pipeconfig.gvOptions.contains(name)) {
                    tree.addAttribute(_initialized, "true");
                    tree.startContent();
                } else {
                    tree.addAttribute(_default, "true");
                    tree.startContent();
                }
                tree.addEndElement();

                tree.addEndElement();
            }

            for (QName name : pipeconfig.parameters.keySet()) {
                tree.addStartElement(pr_parameter);
                tree.startContent();

                tree.addStartElement(pr_name);

                if (!"".equals(name.getPrefix())) {
                    tree.addNamespace(name.getPrefix(), name.getNamespaceURI());
                }

                tree.startContent();
                tree.addText(name.toString());
                tree.addEndElement();

                tree.addStartElement(pr_value);
                tree.startContent();
                tree.addText(pipeconfig.parameters.get(name));
                tree.addEndElement();

                tree.addEndElement();
            }

            for (QName name : pipeconfig.gvParameters) {
                tree.addStartElement(pr_parameter);
                tree.startContent();

                tree.addStartElement(pr_name);
                tree.startContent();
                tree.addText(name.toString());
                tree.addEndElement();

                tree.addStartElement(pr_value);
                tree.addAttribute(_initialized, "true");
                tree.startContent();
                tree.addEndElement();

                tree.addEndElement();
            }
        }
        tree.addEndElement();
        tree.endDocument();

        return new StringRepresentation(serialize(tree.getResult(), variant.getMediaType()), variant.getMediaType());
    }

    @Override
    protected Representation post(Representation entity, Variant variant) {
        String id = (String) getRequest().getAttributes().get("id");
        if (!getPipelines().containsKey(id)) {
            return badRequest(Status.CLIENT_ERROR_NOT_FOUND, "no pipeline: " + pipelineUri(id), variant.getMediaType());
        }

        PipelineConfiguration pipeconfig = getPipelines().get(id);
        XPipeline xpipeline = pipeconfig.pipeline;
        XProcRuntime runtime = pipeconfig.runtime;

        if (pipeconfig.definput == null) {
            return badRequest(Status.CLIENT_ERROR_BAD_REQUEST, "No primary input port", variant.getMediaType());
        }

        if (pipeconfig.ran) {
            pipeconfig.reset();
            xpipeline.reset();
        }

        if (pipeconfig.documentCount(pipeconfig.definput) == 0) {
            xpipeline.clearInputs(pipeconfig.definput);
        }
        pipeconfig.writeTo(pipeconfig.definput);

        try {
            XdmNode doc = null;

            if (isXml(entity.getMediaType())) {
                doc = runtime.parse(new InputSource(entity.getStream()));
            } else {
                ReadablePipe pipe = null;
                pipe = new ReadableData(runtime, XProcConstants.c_data, entity.getStream(), variant.getMediaType().toString());
                doc = pipe.read();
            }

            xpipeline.writeTo(pipeconfig.definput, doc);
        } catch (Exception e) {
            throw new XProcException(e);
        }

        HashMap<QName,String> options = convertForm(getQuery());

        for (QName name : options.keySet()) {
            RuntimeValue value = new RuntimeValue(options.get(name), null, null);
            xpipeline.passOption(name, value);
        }

        return runPipeline(id);
    }

    @Override
    protected Representation delete(Variant variant) {
        String id = (String) getRequest().getAttributes().get("id");
        if (!getPipelines().containsKey(id)) {
            return badRequest(Status.CLIENT_ERROR_NOT_FOUND, "no pipeline: " + pipelineUri(id), variant.getMediaType());
        }

        getPipelines().remove(id);

        setStatus(Status.SUCCESS_OK);
        return new EmptyRepresentation();
    }
}
