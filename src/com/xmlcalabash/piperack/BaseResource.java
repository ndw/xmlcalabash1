package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
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
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.ServerInfo;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ServerResource;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
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
    protected static final HashSet<String> emptyExcludeNS = new HashSet<String> ();

    public BaseResource() {
        super();
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
        Serialization serial = pipeline.getSerialization(pipeconfig.defoutput);

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
}
