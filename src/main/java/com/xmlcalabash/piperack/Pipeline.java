package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.TypeUtils;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.om.NamespaceMap;
import net.sf.saxon.om.SingletonAttributeMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.xml.sax.InputSource;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;

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

        tree.addStartElement(pr_uri);
        tree.addText(pipelineUri(id));
        tree.addEndElement();

        formatExpires(tree, pipeconfig.expires);

        tree.addStartElement(pr_has_run);
        tree.addText("" + pipeconfig.ran);
        tree.addEndElement();

        if (pipeconfig.ran) {
            for (String port : pipeconfig.outputPorts) {
                AttributeMap attr = EmptyAttributeMap.getInstance();
                if (port.equals(pipeconfig.defoutput)) {
                    attr = attr.put(TypeUtils.attributeInfo(_primary, "true"));
                }
                attr = attr.put(TypeUtils.attributeInfo(_documents, "" + pipeconfig.outputs.get(port).size()));
                tree.addStartElement(pr_output, attr);
                tree.addText(port);
                tree.addEndElement();
            }
        } else {
            XPipeline xpipeline = pipeconfig.pipeline;
            DeclareStep pipeline = xpipeline.getDeclareStep();

            for (String port : pipeconfig.inputPorts) {
                AttributeMap attr = EmptyAttributeMap.getInstance();
                if (port.equals(pipeconfig.definput)) {
                    attr = attr.put(TypeUtils.attributeInfo(_primary, "true"));
                }
                attr = attr.put(TypeUtils.attributeInfo(_documents, "" + pipeconfig.documentCount(port)));
                tree.addStartElement(pr_input, attr);
                tree.addText(port);
                tree.addEndElement();
            }

            for (QName name : pipeline.getOptions()) {
                tree.addStartElement(pr_option);

                NamespaceMap nsmap = NamespaceMap.emptyMap();
                if (!"".equals(name.getPrefix())) {
                    nsmap = nsmap.put(name.getPrefix(), name.getNamespaceUri());
                }

                tree.addStartElement(pr_name, EmptyAttributeMap.getInstance(), nsmap);

                tree.addText(name.toString());
                tree.addEndElement();

                AttributeMap attr = EmptyAttributeMap.getInstance();
                if (pipeconfig.options.containsKey(name)) {
                    // nop
                } else if (pipeconfig.gvOptions.contains(name)) {
                    attr = attr.put(TypeUtils.attributeInfo(_initialized, "true"));
                } else {
                    attr = attr.put(TypeUtils.attributeInfo(_default, "true"));
                }

                tree.addStartElement(pr_value, attr);

                if (pipeconfig.options.containsKey(name)) {
                    tree.addText(pipeconfig.options.get(name));
                }
                tree.addEndElement();
                tree.addEndElement();
            }

            for (QName name : pipeconfig.parameters.keySet()) {
                tree.addStartElement(pr_parameter);

                NamespaceMap nsmap = NamespaceMap.emptyMap();
                if (!"".equals(name.getPrefix())) {
                    nsmap = nsmap.put(name.getPrefix(), name.getNamespaceUri());
                }

                tree.addStartElement(pr_name, EmptyAttributeMap.getInstance(), nsmap);

                tree.addText(name.toString());
                tree.addEndElement();

                tree.addStartElement(pr_value);
                tree.addText(pipeconfig.parameters.get(name));
                tree.addEndElement();

                tree.addEndElement();
            }

            for (QName name : pipeconfig.gvParameters) {
                tree.addStartElement(pr_parameter);

                tree.addStartElement(pr_name);
                tree.addText(name.toString());
                tree.addEndElement();

                tree.addStartElement(pr_value, SingletonAttributeMap.of(TypeUtils.attributeInfo(_initialized, "true")));
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

        if (pipeconfig.ran) {
            pipeconfig.reset();
            xpipeline.reset();
        }

        try {
            if (MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {
                processMultipartForm(pipeconfig, entity, variant);
            } else {
                if (pipeconfig.definput == null) {
                    return badRequest(Status.CLIENT_ERROR_BAD_REQUEST, "No primary input port", variant.getMediaType());
                }
                if (pipeconfig.documentCount(pipeconfig.definput) == 0) {
                    xpipeline.clearInputs(pipeconfig.definput);
                }
                pipeconfig.writeTo(pipeconfig.definput);

                XdmNode doc = null;

                if (isXml(entity.getMediaType())) {
                    doc = runtime.parse(new InputSource(entity.getStream()));
                    logger.debug("Posting XML document to " + pipeconfig.definput + " for " + id);
                } else {
                    ReadablePipe pipe = null;
                    pipe = new ReadableData(runtime, XProcConstants.c_data, entity.getStream(), entity.getMediaType().toString());
                    doc = pipe.read();
                    logger.debug("Posting non-XML document to " + pipeconfig.definput + " for " + id);
                }

                xpipeline.writeTo(pipeconfig.definput, doc);

                HashMap<String, String> nsBindings = bindingsFromForm(getQuery());
                HashMap<String, String> options = convertFormStrings(getQuery());

                DeclareStep pipeline = xpipeline.getDeclareStep();
                for (String fieldName : options.keySet()) {
                    RuntimeValue value = new RuntimeValue(options.get(fieldName));

                    if (fieldName.startsWith("-p")) {
                        fieldName = fieldName.substring(2);

                        String port= null;
                        Matcher matcher = portRE.matcher(fieldName);
                        if (matcher.matches()) {
                            port = matcher.group(1);
                            fieldName = matcher.group(2);
                        }

                        if (port == null) {
                            // Figure out the default parameter port
                            for (String iport : xpipeline.getInputs()) {
                                com.xmlcalabash.model.Input input = pipeline.getInput(iport);
                                if (input.getParameterInput() && input.getPrimary()) {
                                    port = iport;
                                }
                            }
                        }

                        if (port == null) {
                            throw new XProcException("No primary parameter input port.");
                        }

                        logger.debug("Parameter " + fieldName + "=" + value.getString() + " for " + id);

                        QName qname = qnameFromForm(fieldName, nsBindings);
                        xpipeline.setParameter(port, qname, value);
                        pipeconfig.setParameter(qname, value.getString());
                    } else {
                        logger.debug("Option " + fieldName + "=" + value.getString() + " for " + id);

                        QName qname = qnameFromForm(fieldName, nsBindings);
                        xpipeline.passOption(qname, value);
                        pipeconfig.setGVOption(qname);
                    }
                }
            }
        } catch (Exception e) {
            pipeconfig.reset();
            xpipeline.reset();
            return badRequest(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), variant.getMediaType());
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
