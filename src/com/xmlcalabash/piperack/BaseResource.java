package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.ServerInfo;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class BaseResource extends ServerResource {
    protected static final String NS_PR = "http://xmlcalabash.com/ns/piperack";
    protected static final QName pr_pipeline = new QName("", NS_PR, "pipeline");
    protected static final QName pr_input = new QName("", NS_PR, "input");
    protected static final QName pr_output = new QName("", NS_PR, "output");
    protected static final QName pr_has_run = new QName("", NS_PR, "has-run");
    protected static final QName pr_uri = new QName("", NS_PR, "uri");
    protected static final QName pr_status = new QName("", NS_PR, "status");
    protected static final QName pr_version = new QName("", NS_PR, "version");
    protected static final QName pr_saxon_version = new QName("", NS_PR, "saxon-version");
    protected static final QName pr_saxon_edition = new QName("", NS_PR, "saxon-edition");
    protected static final QName pr_copyright = new QName("", NS_PR, "copyright");
    protected static final QName pr_message = new QName("", NS_PR, "message");
    protected static final QName pr_expires = new QName("", NS_PR, "expires");
    protected static final QName pr_pipelines = new QName("", NS_PR, "pipelines");
    protected static final QName pr_help = new QName("", NS_PR, "help");
    protected static final QName pr_description = new QName("", NS_PR, "description");
    protected static final QName pr_endpoint = new QName("", NS_PR, "endpoint");
    protected static final QName pr_error = new QName("", NS_PR, "error");
    protected static final QName pr_response = new QName("", NS_PR, "response");
    protected static final QName pr_code = new QName("", NS_PR, "code");
    protected static final QName pr_option = new QName("", NS_PR, "option");
    protected static final QName pr_parameter = new QName("", NS_PR, "parameter");
    protected static final QName pr_name = new QName("", NS_PR, "name");
    protected static final QName pr_value = new QName("", NS_PR, "value");
    protected static final QName _primary = new QName("primary");
    protected static final QName _default = new QName("default");
    protected static final QName _documents = new QName("documents");
    protected static final QName _initialized = new QName("initialized");
    protected static final QName _format = new QName("format");
    protected static final QName _method = new QName("method");

    protected static final Pattern xmlnsRE = Pattern.compile("^xmlns:(.+)$");
    protected static final Pattern qnameRE = Pattern.compile("^(.+):(.+)$");
    protected static final Pattern portRE = Pattern.compile("(\\w+)@(.+)");
    protected static final HashSet<String> emptyExcludeNS = new HashSet<String> ();

    protected Logger logger = null;

    public BaseResource() {
        super();
        logger = LoggerFactory.getLogger(this.getClass());
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
        getVariants().add(new Variant(MediaType.APPLICATION_XML));
        getVariants().add(new Variant(MediaType.TEXT_HTML));
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));
    }

    protected ConcurrentMap<String, PipelineConfiguration> getPipelines() {
        return ((PiperackApplication) getApplication()).getPipelines();
    }

    protected XProcConfiguration getConfiguration() {
        return ((PiperackApplication) getApplication()).getConfiguration();
    }

    protected XProcRuntime getGlobalRuntime() {
        return ((PiperackApplication) getApplication()).getGlobalRuntime();
    }

    protected XdmNode xsl() {
        return ((PiperackApplication) getApplication()).xsl();
    }

    protected String pipelineUri(String id) {
        ServerInfo serverInfo = getServerInfo();
        String hostname = "localhost";
        if (serverInfo.getAddress() != null) {
            // I bet this is a number not a name :-(
            hostname = serverInfo.getAddress();
        }

        return "http://" + hostname + ":" + serverInfo.getPort() + "/pipelines/" + id;
    }

    protected boolean isXml(MediaType type) {
        String isxml = type.getSubType();
        return MediaType.APPLICATION_XML.equals(type) || isxml.endsWith("+xml");
    }

    protected void formatExpires(TreeWriter tree, Calendar expires) {
        if (expires.getTimeInMillis() != Long.MAX_VALUE) {
            tree.addStartElement(pr_expires);
            tree.startContent();
            SimpleDateFormat gmtFrmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            tree.addText(gmtFrmt.format(expires.getTime()));
            tree.addEndElement();
        }
    }

    protected QName qnameFromForm(String name, Form params) {
        HashMap<String,String> bindings = bindingsFromForm(params);
        return qnameFromForm(name, bindings);
    }

    protected QName qnameFromForm(String name, HashMap<String,String> bindings) {
        Matcher matcher = qnameRE.matcher(name);
        if (matcher.matches()) {
            String ns = bindings.get(matcher.group(1));
            return new QName(matcher.group(1), ns, matcher.group(2));
        } else {
            return new QName(name);
        }
    }

    protected HashMap<String,String> bindingsFromForm(Form params) {
        HashMap<String,String> bindings = new HashMap<String,String> ();
        for (String key : params.getNames()) {
            Matcher matcher = xmlnsRE.matcher(key);
            if (matcher.matches()) {
                bindings.put(matcher.group(1), params.getFirstValue(key));
            }
        }
        return bindings;
    }

    protected HashMap<QName,String> convertForm(Form params) {
        HashMap<QName,String> converted = new HashMap<QName,String>();
        HashMap<String,String> bindings = bindingsFromForm(params);

        for (String key : params.getNames()) {
            Matcher matcher = xmlnsRE.matcher(key);
            if (matcher.matches()) {
                // nop
            } else {
                QName name = qnameFromForm(key, bindings);
                converted.put(name, params.getFirstValue(key));
            }
        }

        return converted;
    }

    protected HashMap<String,String> convertFormStrings(Form params) {
        HashMap<String,String> converted = new HashMap<String,String>();

        for (String key : params.getNames()) {
            Matcher matcher = xmlnsRE.matcher(key);
            if (matcher.matches()) {
                // nop
            } else {
                converted.put(key, params.getFirstValue(key));
            }
        }

        return converted;
    }

    protected String serialize(XdmNode doc, MediaType type) {
        XProcRuntime runtime = getGlobalRuntime();
        String format = "text";

        if (MediaType.TEXT_HTML.equals(type)) {
            format = "html";
        } else if (MediaType.APPLICATION_XML.equals(type)) {
            format = "xml";
        } else if (MediaType.APPLICATION_JSON.equals(type)) {
            format = "json";
        }

        if (xsl() != null) {
            XdmDestination result = null;
            try {
                XsltCompiler compiler = runtime.getProcessor().newXsltCompiler();
                XsltExecutable exec = compiler.compile(xsl().asSource());
                XsltTransformer transformer = exec.load();
                transformer.setParameter(_format, new XdmAtomicValue(format));
                transformer.setInitialContextNode(doc);
                result = new XdmDestination();
                transformer.setDestination(result);
                transformer.transform();
            } catch (SaxonApiException e) {
                throw new XProcException(e);
            }

            doc = result.getXdmNode();
        }

        return doc.toString();
    }

    protected Representation badRequest(Status status, String msg, MediaType type) {
        TreeWriter tree = new TreeWriter(getGlobalRuntime());
        tree.startDocument(URI.create("http://example.com/"));
        tree.addStartElement(pr_error);
        tree.startContent();
        tree.addStartElement(pr_code);
        tree.startContent();
        tree.addText("" + status.getCode());
        tree.addEndElement();
        tree.addStartElement(pr_message);
        tree.startContent();
        tree.addText("Bad request: " + msg);
        tree.addEndElement();
        tree.addEndElement();
        tree.endDocument();
        setStatus(status);
        return new StringRepresentation(serialize(tree.getResult(), type), type);
    }

    protected Representation okResponse(String msg, MediaType type) {
        return okResponse(msg, type, Status.SUCCESS_OK);
    }

    protected Representation okResponse(String msg, MediaType type, Status status) {
        TreeWriter tree = new TreeWriter(getGlobalRuntime());
        tree.startDocument(URI.create("http://example.com/"));
        tree.addStartElement(pr_response);
        tree.startContent();
        tree.addStartElement(pr_code);
        tree.startContent();
        tree.addText("" + status.getCode());
        tree.addEndElement();
        tree.addStartElement(pr_message);
        tree.startContent();
        tree.addText(msg);
        tree.addEndElement();
        tree.addEndElement();
        tree.endDocument();
        return new StringRepresentation(serialize(tree.getResult(), type), type);
    }

    protected Representation runPipeline(String id) {
        PipelineConfiguration pipeconfig = getPipelines().get(id);
        XPipeline pipeline = pipeconfig.pipeline;

        try {
            pipeline.run();
            pipeconfig.ran = true;

            for (String port : pipeline.getOutputs()) {
                Vector<XdmNode> nodes = new Vector<XdmNode> ();
                ReadablePipe rpipe = pipeline.readFrom(port);
                while (rpipe.moreDocuments()) {
                    nodes.add(rpipe.read());
                }
                pipeconfig.outputs.put(port, nodes);
            }
        } catch (Exception e) {
            throw new XProcException(e);
        }

        if (pipeconfig.defoutput != null) {
            return getOutput(pipeconfig, pipeconfig.defoutput);
        } else {
            setStatus(Status.SUCCESS_OK);
            return new EmptyRepresentation();
        }
    }

    protected Representation getOutput(PipelineConfiguration pipeconfig, String port) {
        XProcConfiguration config = getConfiguration();
        XPipeline pipeline = pipeconfig.pipeline;
        XProcRuntime runtime = pipeconfig.runtime;
        Serialization serial = pipeline.getSerialization(port);

        if (serial == null) {
            // Use the configuration options
            // FIXME: should each of these be considered separately?
            // FIXME: should there be command-line options to override these settings?
            serial = new Serialization(runtime, pipeline.getNode()); // The node's a hack
            for (String name : config.serializationOptions.keySet()) {
                String value = config.serializationOptions.get(name);

                if ("byte-order-mark".equals(name)) serial.setByteOrderMark("true".equals(value));
                if ("escape-uri-attributes".equals(name)) serial.setEscapeURIAttributes("true".equals(value));
                if ("include-content-type".equals(name)) serial.setIncludeContentType("true".equals(value));
                if ("indent".equals(name)) serial.setIndent("true".equals(value));
                if ("omit-xml-declaration".equals(name)) serial.setOmitXMLDeclaration("true".equals(value));
                if ("undeclare-prefixes".equals(name)) serial.setUndeclarePrefixes("true".equals(value));
                if ("method".equals(name)) serial.setMethod(new QName("", value));

                // FIXME: if ("cdata-section-elements".equals(name)) serial.setCdataSectionElements();
                if ("doctype-public".equals(name)) serial.setDoctypePublic(value);
                if ("doctype-system".equals(name)) serial.setDoctypeSystem(value);
                if ("encoding".equals(name)) serial.setEncoding(value);
                if ("media-type".equals(name)) serial.setMediaType(value);
                if ("normalization-form".equals(name)) serial.setNormalizationForm(value);
                if ("standalone".equals(name)) serial.setStandalone(value);
                if ("version".equals(name)) serial.setVersion(value);
            }
        }

        if (!pipeconfig.outputs.containsKey(port)) {
            return badRequest(Status.CLIENT_ERROR_BAD_REQUEST, "no port named: " + port, MediaType.APPLICATION_XML);
        }

        Vector<XdmNode> nodes = pipeconfig.outputs.get(port);
        if (nodes.size() == 0) {
            setStatus(Status.SUCCESS_NO_CONTENT);
            return new EmptyRepresentation();
        }

        XdmNode doc = nodes.firstElement();
        nodes.remove(0);

        // I wonder if there's a better way...
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WritableDocument wd = new WritableDocument(runtime, doc.getBaseURI().toASCIIString(), serial, bos);
        wd.write(doc);

        try {
            String xml = bos.toString("UTF-8");
            Representation result = new StringRepresentation(xml, MediaType.APPLICATION_XML);
            setStatus(Status.SUCCESS_OK);
            return result;
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    protected Representation processMultipartForm(PipelineConfiguration pipeconfig, Representation entity, Variant variant) {
        String id = (String) getRequest().getAttributes().get("id");

        XPipeline xpipeline = pipeconfig.pipeline;
        XProcRuntime runtime = pipeconfig.runtime;

        if (pipeconfig.ran) {
            pipeconfig.reset();
            xpipeline.reset();
        }

        String message = "";

        HashMap<String,String> nameValuePairs = new HashMap<String,String> ();
        HashMap<String,String> nsBindings = new HashMap<String,String> ();

        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(100240);

        RestletFileUpload upload = new RestletFileUpload(factory);
        List<FileItem> items;
        try {
            items = upload.parseRequest(getRequest());

            File file = null;
            String filename = null;

            for (final Iterator<FileItem> it = items.iterator(); it.hasNext(); ) {
                FileItem fi = it.next();
                String fieldName = fi.getFieldName();
                String name = fi.getName();

                if (name == null) {
                    Matcher matcher = xmlnsRE.matcher(fieldName);
                    if (matcher.matches()) {
                        nsBindings.put(matcher.group(1), new String(fi.get(), "utf-8"));
                    } else {
                        nameValuePairs.put(fieldName, new String(fi.get(), "utf-8"));
                    }
                } else {
                    String port = fieldName;

                    if (pipeconfig.documentCount(port) == 0) {
                        xpipeline.clearInputs(port);
                    }
                    pipeconfig.writeTo(port);

                    try {
                        XdmNode doc = null;
                        MediaType m = new MediaType(fi.getContentType());

                        if (isXml(m)) {
                            doc = runtime.parse(new InputSource(fi.getInputStream()));
                            logger.debug("Posting XML document to " + port + " for " + id);
                        } else {
                            ReadablePipe pipe = null;
                            pipe = new ReadableData(runtime, XProcConstants.c_data, fi.getInputStream(), fi.getContentType());
                            doc = pipe.read();
                            logger.debug("Posting non-XML document to " + port + " for " + id);
                        }
                        xpipeline.writeTo(port, doc);
                    } catch (Exception e) {
                        throw new XProcException(e);
                    }

                    message += "Posted input to port '" + port + "'\n";
                }
            }


            DeclareStep pipeline = xpipeline.getDeclareStep();
            for (String fieldName : nameValuePairs.keySet()) {
                RuntimeValue value = new RuntimeValue(nameValuePairs.get(fieldName));

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
                    message += "Parameter " + qname.getClarkName() + "=" + value.getString() + "\n";
                } else {
                    logger.debug("Option " + fieldName + "=" + value.getString() + " for " + id);

                    QName qname = qnameFromForm(fieldName, nsBindings);
                    xpipeline.passOption(qname, value);
                    pipeconfig.setGVOption(qname);
                    message += "Option " + qname.getClarkName() + "=" + value.getString() + "\n";
                }
            }

            return okResponse(message, variant.getMediaType(), Status.SUCCESS_OK);
        } catch (XProcException e) {
            pipeconfig.reset();
            xpipeline.reset();
            throw e;
        } catch (Exception e) {
            pipeconfig.reset();
            xpipeline.reset();
            throw new XProcException(e);
        }
    }
}
