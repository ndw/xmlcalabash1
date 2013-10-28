package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.XdmNode;
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

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Pipelines extends BaseResource {
    @Override
    protected Representation get(Variant variant) {
        TreeWriter tree = new TreeWriter(getGlobalRuntime());
        tree.startDocument(URI.create("http://example.com/"));
        tree.addStartElement(pr_pipelines);
        tree.startContent();
        for (String id : getPipelines().keySet()) {
            PipelineConfiguration pipeconfig = getPipelines().get(id);

            tree.addStartElement(pr_pipeline);
            tree.startContent();

            tree.addStartElement(pr_uri);
            tree.startContent();
            tree.addText(pipelineUri(id));
            tree.addEndElement();

            tree.addStartElement(pr_has_run);
            tree.startContent();
            tree.addText("" + pipeconfig.ran);
            tree.addEndElement();

            formatExpires(tree, pipeconfig.expires);

            tree.addEndElement();
        }
        tree.addEndElement();
        tree.endDocument();

        return new StringRepresentation(serialize(tree.getResult(), variant.getMediaType()), variant.getMediaType());
    }

    @Override
    protected Representation post(Representation entity, Variant variant) {
        Form form = getQuery();
        Random random = new Random();

        String name = form.getFirstValue("name");
        String id = null;
        if (name == null) {
            id = "" + Math.abs(random.nextLong());
        } else {
            id = name;
        }

        while (getPipelines().containsKey(id)) {
            if (name == null) {
                id = "" + Math.abs(random.nextLong());
            } else {
                id = name + "-" + Math.abs(random.nextLong());
            }
        }

        int seconds = getConfiguration().piperackDefaultExpires;
        String secstr = form.getFirstValue("expires");
        if (secstr != null) {
            seconds = Integer.parseInt(secstr);
        }

        Calendar expires = GregorianCalendar.getInstance();
        if (seconds >= 0) {
            long millis = seconds;
            long extime = expires.getTimeInMillis() + (millis*1000);
            expires.setTimeInMillis(extime);
        } else {
            expires.setTimeInMillis(Long.MAX_VALUE);
        }

        XProcRuntime runtime = new XProcRuntime(getConfiguration());

        try {
            XdmNode doc = runtime.parse(new InputSource(entity.getStream()));
            XPipeline pipeline = runtime.use(doc);
            getPipelines().put(id, new PipelineConfiguration(runtime, pipeline, expires));
        } catch (Exception e) {
            throw new XProcException(e);
        }

        Series<Header> responseHeaders = (Series<Header>) getResponse().getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Series(Header.class);
            getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
        }
        responseHeaders.add(new Header("Location", pipelineUri(id)));

        TreeWriter tree = new TreeWriter(getGlobalRuntime());
        tree.startDocument(URI.create("http://example.com/"));

        tree.addStartElement(pr_response);
        tree.startContent();
        tree.addStartElement(pr_code);
        tree.startContent();
        tree.addText("" + Status.SUCCESS_CREATED.getCode());
        tree.addEndElement();

        if (expires.getTimeInMillis() != Long.MAX_VALUE) {
            tree.addStartElement(pr_expires);
            tree.startContent();
            SimpleDateFormat gmtFrmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            tree.addText(gmtFrmt.format(expires.getTime()));
            tree.addEndElement();
        }

        tree.addStartElement(pr_uri);
        tree.startContent();
        tree.addText(pipelineUri(id));
        tree.addEndElement();

        tree.addStartElement(pr_message);
        tree.startContent();
        tree.addText("Created " + pipelineUri(id));
        tree.addEndElement();
        tree.addEndElement();
        tree.endDocument();

        setStatus(Status.SUCCESS_CREATED);
        return new StringRepresentation(serialize(tree.getResult(), variant.getMediaType()), variant.getMediaType());
    }
}
