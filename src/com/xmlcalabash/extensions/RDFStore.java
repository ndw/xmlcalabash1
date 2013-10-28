package com.xmlcalabash.extensions;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotReader;
import org.apache.jena.riot.lang.LangRIOT;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class RDFStore extends RDFStep {
    protected static final QName _content_type = new QName("", "content-type");
    String contentType = null;

    /**
     * Creates a new instance of Identity
     */
    public RDFStore(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void run() throws SaxonApiException {
        super.run();

        makeAnonymousNodes = true;
        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            loadRdf(dataset, doc);
        }

        URI href = null;
        if (getOption(_href) != null) {
            String uri = getOption(_href).getString();
            href = step.getNode().getBaseURI();
            href = href.resolve(uri);
        }

        String graphName = getOption(_graph, (String) null);

        Lang lang = getLanguage(href == null ? (String) null : href.toASCIIString());
        if (lang == null) {
            lang = Lang.RDFXML;
        }

        String langtag = "RDF/XML";
        if (lang == Lang.RDFXML) {
            langtag = "RDF/XML";
            contentType = "application/rdf+xml";
        } else if (lang == Lang.NTRIPLES) {
            langtag = "N-TRIPLE";
            contentType = "application/n-triples";
        } else if (lang == Lang.N3) {
            langtag = "N3";
            contentType = "application/n3";
        } else if (lang == Lang.TURTLE) {
            langtag = "TURTLE";
            contentType = "application/turtle";
        } else {
            System.err.println("Unsupported language specified; using RDF/XML");
        }

        Model model = null;
        if (graphName == null) {
            model = dataset.getDefaultModel();
        } else {
            model = dataset.getNamedModel(graphName);
        }

        OutputStream outstr = null;
        ByteArrayOutputStream baos = null;

        try {
            if (href == null) {
                baos = new ByteArrayOutputStream();
                outstr = baos;
            } else if (href.getScheme().equals("file")) {
                File output = new File(href);

                File path = new File(output.getParent());
                if (!path.isDirectory()) {
                    if (!path.mkdirs()) {
                        throw XProcException.stepError(50);
                    }
                }
                outstr = new FileOutputStream(output);
            } else {
                final URLConnection conn = href.toURL().openConnection();
                conn.setDoOutput(true);
                outstr = conn.getOutputStream();
            }

            model.write(outstr, langtag);
            outstr.close();

            if (href == null) {
                returnData(baos);
            }
        } catch (IOException e) {
            throw new XProcException(e);
        }

        if (href != null) {
            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(XProcConstants.c_result);
            tree.startContent();
            tree.addText(href.toString());
            tree.addEndElement();
            tree.endDocument();
            result.write(tree.getResult());
        }
    }

    public void returnData(ByteArrayOutputStream baos) {
        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_data);
        tree.addAttribute(_content_type, contentType);
        tree.startContent();
        tree.addText(baos.toString());
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());

    }
}
