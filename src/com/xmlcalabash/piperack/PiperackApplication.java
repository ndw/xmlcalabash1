package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.engine.header.Header;
import org.restlet.representation.StringRepresentation;
import org.restlet.routing.Router;
import org.restlet.util.Series;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class PiperackApplication extends Application {
    private ConcurrentMap<String, PipelineConfiguration> pipes = new ConcurrentHashMap<String, PipelineConfiguration> ();
    private XProcConfiguration config = null;
    private XProcRuntime globalRuntime = null;
    private XdmNode xsl = null;
    private boolean stopped = false;

    public PiperackApplication(XProcConfiguration config, XProcRuntime runtime) throws SaxonApiException {
        this.config = config;
        globalRuntime = runtime;

        InputStream instream = PiperackApplication.class.getResourceAsStream("/etc/serializepr.xsl");
        if (instream == null) {
            System.err.println("Error: cannot load /etc/serializepr.xsl from jar file");
            System.exit(-1);
        } else {
            SAXSource source = new SAXSource(new InputSource(instream));
            DocumentBuilder builder = config.getProcessor().newDocumentBuilder();
            builder.setLineNumbering(true);
            builder.setBaseURI(URI.create("http://xmlcalabash.com/ns/piperack"));
            xsl = builder.build(source);
        }

        for (String name : config.piperackDefaultPipelines.keySet()) {
            PipelineSource src = config.piperackDefaultPipelines.get(name);
            loadPipeline(src.uri, name, src.expires);
        }

    }

    @Override
    public Restlet createInboundRoot() {
        // Create a root router
        Router router = new Router(getContext());

        // Attach the handlers to the root router
        router.attach("/pipelines", Pipelines.class);
        router.attach("/pipelines/{id}", Pipeline.class);
        router.attach("/pipelines/{id}/inputs/{port}", Input.class);
        router.attach("/pipelines/{id}/outputs/{port}", Output.class);
        router.attach("/pipelines/{id}/options", Options.class);
        router.attach("/pipelines/{id}/options/{option}", Option.class);
        router.attach("/pipelines/{id}/parameters", Parameters.class);
        router.attach("/pipelines/{id}/parameters/{port}", Parameters.class);
        router.attach("/pipelines/{id}/parameters/{port}/{param}", Parameter.class);
        router.attach("/pipelines/{id}/run", Run.class);
        router.attach("/pipelines/{id}/reset", Reset.class);
        router.attach("/status", Status.class);
        router.attach("/help", Help.class);
        router.attach("/stop", Stop.class);
        router.attach("/", Help.class);

        // Return the root router
        return router;
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        stopped = true;
    }

    public void expirePipelines() {
        Calendar now = GregorianCalendar.getInstance();
        HashSet<String> deleteKeys = new HashSet<String> ();
        for (String key : pipes.keySet()) {
            PipelineConfiguration pipeconfig = pipes.get(key);
            if (now.compareTo(pipeconfig.expires) > 0) {
                deleteKeys.add(key);
            }
        }
        for (String key : deleteKeys) {
            System.err.println("Expired: " + key);
            pipes.remove(key);
        }
    }

    public ConcurrentMap<String, PipelineConfiguration> getPipelines() {
        return pipes;
    }

    public XProcConfiguration getConfiguration() {
        return config;
    }

    public XProcRuntime getGlobalRuntime() {
        return globalRuntime;
    }

    public XdmNode xsl() {
        return xsl;
    }

    public boolean stopped() {
        return stopped;
    }

    private void loadPipeline(String uri, String id, int seconds) {
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
            XdmNode doc = runtime.parse(uri, runtime.getStaticBaseURI().toASCIIString());
            XPipeline pipeline = runtime.use(doc);
            getPipelines().put(id, new PipelineConfiguration(runtime, pipeline, expires));
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }
}
