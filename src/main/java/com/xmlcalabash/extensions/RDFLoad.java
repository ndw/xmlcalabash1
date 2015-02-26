package com.xmlcalabash.extensions;

import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.util.Context;
import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XAtomicStep;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.RiotReader;
import org.apache.jena.riot.lang.LangRIOT;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

@XMLCalabash(
        name = "cx:rdf-load",
        type = "{http://xmlcalabash.com/ns/extensions}rdf-load")

public class RDFLoad extends RDFStep {
    /**
     * Creates a new instance of Identity
     */
    public RDFLoad(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void run() throws SaxonApiException {
        super.run();

        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            loadRdf(dataset, doc);
        }

        String href = getOption(_href).getString();
        String graphName = getOption(_graph, (String) null);

        Lang lang = getLanguage(href);
        if (lang == null) {
            throw new XProcException("Could not deduce language for RDFLoad data.");
        }

        Iterator<String> graphNameIter = null;
        StmtIterator statementIter = null;

        try {
            URI baseURI = step.getNode().getBaseURI();
            URL url = baseURI.resolve(href).toURL();
            URLConnection conn = url.openConnection();

            ReaderRIOT reader = RDFDataMgr.createReader(lang);
            StreamRDF dest = StreamRDFLib.dataset(dataset.asDatasetGraph());
            ErrorHandler handler = new ParserErrorHandler(href);
            ParserProfile prof = RiotLib.profile(lang, href, handler);
            Context context = new Context();

            reader.setErrorHandler(handler);
            reader.setParserProfile(prof);

            reader.read(conn.getInputStream(), href, lang.getContentType(), dest, context);
            conn.getInputStream().close();
        } catch (MalformedURLException e) {
            throw new XProcException(e);
        } catch (IOException e) {
            throw new XProcException(e);
        }

        if (graphName == null) {
            statementIter = dataset.getDefaultModel().listStatements();
            dumpStatements(statementIter, null);

            graphNameIter = dataset.listNames();
            while (graphNameIter.hasNext()) {
                String collection = graphNameIter.next();
                statementIter = dataset.getNamedModel(collection).listStatements();
                dumpStatements(statementIter, collection);
            }
        } else {
            statementIter = dataset.getNamedModel(graphName).listStatements();
            dumpStatements(statementIter, graphName);
        }
    }
}
