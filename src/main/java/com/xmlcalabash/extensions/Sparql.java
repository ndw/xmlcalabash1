package com.xmlcalabash.extensions;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
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

public class Sparql extends RDFStep {
    private static final QName _content_type = new QName("content-type");

    private static final QName s_sparql = new QName("http://www.w3.org/2005/sparql-results#", "sparql");
    private static final QName s_head = new QName("http://www.w3.org/2005/sparql-results#", "head");
    private static final QName s_variable = new QName("http://www.w3.org/2005/sparql-results#", "variable");
    private static final QName s_results = new QName("http://www.w3.org/2005/sparql-results#", "results");
    private static final QName s_result = new QName("http://www.w3.org/2005/sparql-results#", "result");
    private static final QName s_binding = new QName("http://www.w3.org/2005/sparql-results#", "binding");
    private static final QName s_literal = new QName("http://www.w3.org/2005/sparql-results#", "literal");
    private static final QName s_uri = new QName("http://www.w3.org/2005/sparql-results#", "uri");
    private static final QName s_bnode = new QName("http://www.w3.org/2005/sparql-results#", "bnode");

    private static final QName _name = new QName("", "name");

    private ReadablePipe query = null;

    public Sparql(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    @Override
    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else if ("query".equals(port)) {
            query = pipe;
        }
    }

    public void run() throws SaxonApiException {
        super.run();

        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            loadRdf(dataset, doc);
        }

        XdmNode root = S9apiUtils.getDocumentElement(query.read());
        String queryString = null;

        if ((XProcConstants.c_data.equals(root.getNodeName())
                && "application/octet-stream".equals(root.getAttributeValue(_content_type)))
                || "base64".equals(root.getAttributeValue(_encoding))) {
            byte[] decoded = Base64.decode(root.getStringValue());
            queryString = new String(decoded);
        } else {
            queryString = root.getStringValue();
        }

        Query query = QueryFactory.create(queryString);
        QueryExecution qe = null;
        ResultSet results = null;

        /*
        qe = QueryExecutionFactory.create(query, dataset);
        results =  qe.execSelect();
        ResultSetFormatter.out(System.out, results, query);
        qe.close();
        */

        qe = QueryExecutionFactory.create(query, dataset);
        results = qe.execSelect();

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(s_sparql);
        tree.startContent();

        tree.addStartElement(s_head);
        tree.startContent();

        for (String var : results.getResultVars()) {
            tree.addStartElement(s_variable);
            tree.addAttribute(_name, var);
            tree.startContent();
            tree.addEndElement();
        }

        tree.addEndElement();

        tree.addStartElement(s_results);
        tree.startContent();

        while (results.hasNext()) {
            QuerySolution soln = results.next();

            tree.addStartElement(s_result);
            tree.startContent();

            Iterator<String> iter = soln.varNames();
            while (iter.hasNext()) {
                String var = iter.next();

                tree.addStartElement(s_binding);
                tree.addAttribute(_name, var);
                tree.startContent();

                RDFNode node = soln.get(var);

                if (node.isLiteral()) {
                    Literal lit = node.asLiteral();
                    tree.addStartElement(s_literal);

                    if (lit.getLanguage() == null || "".equals(lit.getLanguage())) {
                        String dt = lit.getDatatypeURI();
                        if (dt == null || "".equals(dt)) {
                            // nop
                        } else {
                            tree.addAttribute(_datatype, dt);
                        }
                    } else {
                        tree.addAttribute(XProcConstants.xml_lang, node.asLiteral().getLanguage());
                    }

                    tree.addText(node.asLiteral().toString());
                    tree.addEndElement();
                } else if (node.isResource()) {
                    Resource rsrc = node.asResource();

                    if (rsrc.toString().startsWith("http://marklogic.com/semantics/blank/")) {
                        tree.addStartElement(s_bnode);
                        tree.startContent();
                        tree.addText(rsrc.toString());
                        tree.addEndElement();
                    } else {
                        tree.addStartElement(s_uri);
                        tree.startContent();
                        tree.addText(rsrc.toString());
                        tree.addEndElement();
                    }
                } else {
                    throw new XProcException("Unexpected node type in sparql results");
                }

                tree.addEndElement();
            }

            tree.addEndElement();
        }

        tree.addEndElement();
        tree.addEndElement();
        tree.endDocument();

        qe.close();

        result.write(tree.getResult());
    }
}