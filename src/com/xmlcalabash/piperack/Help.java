package com.xmlcalabash.piperack;

import com.xmlcalabash.util.TreeWriter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;

import java.net.URI;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Help extends BaseResource {
    protected TreeWriter tree = null;

    @Override
    protected Representation get(Variant variant) {
        tree = new TreeWriter(getGlobalRuntime());
        tree.startDocument(URI.create("http://example.com/"));

        tree.addStartElement(pr_help);
        tree.startContent();

        describe("/pipelines", "GET", "Print list of available pipelines.");
        describe("/pipelines", "POST", "Add a pipeline to the server; suggest id with name parameter.");
        describe("/pipelines/{id}", "GET", "Print information about the specified pipeline.");
        describe("/pipelines/{id}", "POST", "Send posted document to the primary input port; set options from URI parameters; run pipeline; return first document on primary output port.");
        describe("/pipelines/{id}/inputs/{port}", "POST", "Send posted document to the specified port.");
        describe("/pipelines/{id}/outputs/{port}", "GET", "Read next docuent from specified port.");
        describe("/pipelines/{id}/options", "POST", "Set options using URI parameters; use xmlns:xxx to specify bindings if necessary.");
        describe("/pipelines/{id}/options/{option}", "POST", "Sets the value of the specified option to the posted content.");
        describe("/pipelines/{id}/parameters", "POST", "Set parameters on the primary parameter input port using URI parameters; use xmlns:xxx to specify bindings if necessary.");
        describe("/pipelines/{id}/parameters/{port}", "POST", "Set parameters on the specified parameter input port using URI parameters; use xmlns:xxx to specify bindings if necessary.");
        describe("/pipelines/{id}/parameters/{port}/{param}", "POST", "Sets the value of the specified parameter on the specified port to the posted content.");
        describe("/pipelines/{id}/run", "POST", "Set options from URI parameters; run the pipeline; return the first document on the primary output port.");
        describe("/pipelines/{id}/reset", "POST", "Reset the pipeline (discard inputs, outputs, options, and parameters)");
        describe("/status","GET","Print server status information.");
        describe("/help", "GET", "Print this help information.");
        describe("/stop", "POST", "Terminate the server.");
        describe("/", "GET", "Print this help information (synonymous with /help)");

        tree.addEndElement();
        tree.endDocument();

        return new StringRepresentation(serialize(tree.getResult(), variant.getMediaType()), variant.getMediaType());
    }

    protected void describe(String uri, String method, String description) {
        tree.addStartElement(pr_endpoint);
        tree.startContent();

        tree.addStartElement(pr_uri);
        tree.addAttribute(_method, method);
        tree.startContent();
        tree.addText(uri);
        tree.addEndElement();

        tree.addStartElement(pr_description);
        tree.startContent();
        tree.addText(description);
        tree.addEndElement();
        tree.addEndElement();
    }
}
