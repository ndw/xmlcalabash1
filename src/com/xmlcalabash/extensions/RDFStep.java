package com.xmlcalabash.extensions;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.Axis;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class RDFStep extends DefaultStep {
    protected static final QName sem_triples = new QName("sem","http://marklogic.com/semantics", "triples");
    protected static final QName sem_triple = new QName("sem","http://marklogic.com/semantics", "triple");
    protected static final QName sem_subject = new QName("sem","http://marklogic.com/semantics", "subject");
    protected static final QName sem_predicate = new QName("sem","http://marklogic.com/semantics", "predicate");
    protected static final QName sem_object = new QName("sem","http://marklogic.com/semantics", "object");
    protected static final QName cx_graph_name = new QName("cx", XProcConstants.NS_CALABASH_EX, "graph-name");
    protected static final QName cx_database_uri = new QName("cx", XProcConstants.NS_CALABASH_EX, "database-uri");
    protected static final QName _datatype = new QName("", "datatype");
    protected static final QName _graph = new QName("", "graph");
    protected static final QName _href = new QName("", "href");
    protected static final QName _language = new QName("", "language");
    protected static final QName _max_triples = new QName("", "max-triples-per-document");

    protected static Pattern[] patterns = new Pattern[] {
            Pattern.compile("&"), Pattern.compile("<"), Pattern.compile(">") };

    protected ReadablePipe source = null;
    protected WritablePipe result = null;
    protected long limit = 100;
    protected Random random = new Random();
    protected long randomValue = random.nextLong();
    protected Calendar cal = Calendar.getInstance();
    protected long milliSecs = cal.getTimeInMillis();
    protected boolean makeAnonymousNodes = false;

    protected Dataset dataset = DatasetFactory.createMem();
    protected HashMap<String,AnonId> anonids = new HashMap<String, AnonId> ();


    public RDFStep(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        if (getOption(_max_triples) != null) {
            String limitStr = getOption(_max_triples).getString();
            try {
                limit = Integer.parseInt(limitStr);
            } catch (NumberFormatException nfe) {
                throw XProcException.dynamicError(19, "The max-triples-per-document on cx:rdf must be an integer");
            }
        }
    }

    protected Lang getLanguage(String href) {
        String language = getOption(_language, (String) null);

        if (language == null && href != null) {
            String ext = null;
            if (href.contains(".")) {
                int pos = href.lastIndexOf(".");
                ext = href.substring(pos);
            }

            if (ext == null) {
                // nevermind
            } else if (".rdf".equals(ext)) {
                language = "rdf/xml";
            } else if (".ttl".equals(ext)) {
                language = "turtle";
            } else if (".json".equals(ext)) {
                language = "rdf/json";
            } else if (".n3".equals(ext)) {
                language = "n3";
            } else if (".nt".equals(ext)) {
                language = "ntriples";
            } else if (".nq".equals(ext)) {
                language = "nquads";
            } else if (".trig".equals(ext)) {
                language = "trig";
            }
        }

        Lang lang = null;
        if ("rdf/xml".equals(language)) {
            lang = Lang.RDFXML;
        } else if ("turtle".equals(language)) {
            lang = Lang.TURTLE;
        } else if ("rdf/json".equals(language)) {
            lang = Lang.RDFJSON;
        } else if ("n3".equals(language)) {
            lang = Lang.N3;
        } else if ("ntriples".equals(language)) {
            lang = Lang.NTRIPLES;
        } else if ("nquads".equals(language)) {
            lang = Lang.NQUADS;
        } else if ("trig".equals(language)) {
            lang = Lang.TRIG;
        }

        if (lang == null) {
            throw new XProcException("Could not deduce language for RDFLoad data: " + language);
        }

        return lang;
    }

    protected String databasePath() {
        return "/triples/";
    }

    protected String nextDatabaseUri() {
        return databasePath() + Long.toHexString(fuse(scramble(random.nextLong()),fuse(scramble(milliSecs),random.nextLong()))) + ".xml";
    }

    protected void dumpStatements(StmtIterator stmtIter, String graphName) {
        TreeWriter tree = null;
        long count = 0;

        tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(sem_triples);
        tree.startContent();

        tree.addStartElement(cx_database_uri);
        tree.startContent();
        tree.addText(nextDatabaseUri());
        tree.addEndElement();

        if (graphName != null) {
            tree.addStartElement(cx_graph_name);
            tree.startContent();
            tree.addText(graphName);
            tree.addEndElement();
        }

        while (stmtIter.hasNext()) {
            Statement stmt = stmtIter.nextStatement();

            tree.addStartElement(sem_triple);
            tree.startContent();

            subject(tree, stmt.getSubject());
            predicate(tree, stmt.getPredicate());
            object(tree, stmt.getObject());

            tree.addEndElement();

            count += 1;
            if (count >= limit) {
                tree.addEndElement();
                tree.endDocument();

                XdmNode out = tree.getResult();
                result.write(out);

                tree = new TreeWriter(runtime);
                tree.startDocument(step.getNode().getBaseURI());
                tree.addStartElement(sem_triples);
                tree.startContent();

                tree.addStartElement(cx_database_uri);
                tree.startContent();
                tree.addText(nextDatabaseUri());
                tree.addEndElement();

                if (graphName != null) {
                    tree.addStartElement(cx_graph_name);
                    tree.startContent();
                    tree.addText(graphName);
                    tree.addEndElement();
                }

                count = 0;
            }
        }

        if (count > 0) {
            tree.addEndElement();
            tree.endDocument();

            XdmNode out = tree.getResult();
            result.write(out);
        }
    }

    private String resource(Resource rsrc) {
        if (rsrc.isAnon()) {
            return "http://marklogic.com/semantics/blank/" + Long.toHexString(
                    fuse(scramble((long)rsrc.hashCode()),fuse(scramble(milliSecs),randomValue)));
        } else {
            return escapeXml(rsrc.toString());
        }
    }

    protected void subject(TreeWriter tree, Resource subj) {
        tree.addStartElement(sem_subject);
        tree.startContent();
        tree.addText(resource(subj));
        tree.addEndElement();
    }

    protected void predicate(TreeWriter tree, Resource pred) {
        tree.addStartElement(sem_predicate);
        tree.startContent();
        tree.addText(resource(pred));
        tree.addEndElement();
    }

    private void object(TreeWriter tree, RDFNode node) {
        if (node.isLiteral()) {
            Literal lit = node.asLiteral();
            String text = lit.getString();
            String lang = lit.getLanguage();
            String type = lit.getDatatypeURI();

            if (lang == null || "".equals(lang)) {
                lang = null;
            } else {
                lang = escapeXml(lang);
            }

            if (lang == null) {
                if (type == null) {
                    type = "http://www.w3.org/2001/XMLSchema#string";
                }
                type = escapeXml(type);
            } else {
                type = null;
            }

            tree.addStartElement(sem_object);
            if (lang != null) {
                tree.addAttribute(XProcConstants.xml_lang, lang);
            }
            if (type != null) {
                tree.addAttribute(_datatype, type);
            }
            tree.startContent();
            tree.addText(escapeXml(text));
            tree.addEndElement();
        } else if (node.isAnon()) {
            String uri = "http://marklogic.com/semantics/blank/" + Long.toHexString(
                    fuse(scramble((long)node.hashCode()),fuse(scramble(milliSecs),randomValue)));

            tree.addStartElement(sem_object);
            tree.startContent();
            tree.addText(uri);
            tree.addEndElement();
        } else {
            tree.addStartElement(sem_object);
            tree.startContent();
            tree.addText(escapeXml(node.toString()));
            tree.addEndElement();
        }
    }

    protected static String escapeXml(String _in) {
        if (null == _in){
            return "";
        }
        return patterns[2].matcher(
                patterns[1].matcher(
                        patterns[0].matcher(_in).replaceAll("&amp;"))
                        .replaceAll("&lt;")).replaceAll("&gt;");
    }

    protected long rotl(long x, long y)
    {
        return (x<<y)^(x>>(64-y));
    }

    protected long fuse(long a, long b)
    {
        return rotl(a,8)^b;
    }

    protected long scramble(long x)
    {
        return x^rotl(x,20)^rotl(x,40);
    }

    protected void loadRdf(Dataset dataset, XdmNode doc) {
        Model model = dataset.getDefaultModel();
        String graphName = null;

        XdmNode root = S9apiUtils.getDocumentElement(doc);
        if (root.getNodeName().equals(sem_triples)) {
            for (XdmNode node : new RelevantNodes(null, root, Axis.CHILD)) {
                if (node.getNodeName().equals(cx_graph_name)) {
                    graphName = node.getStringValue();
                    model = dataset.getNamedModel(graphName);
                    // ModelFactory.createDefaultModel();
                } else if (node.getNodeName().equals(sem_triple)) {
                    XdmNode subj = null;
                    XdmNode pred = null;
                    XdmNode obj = null;

                    for (XdmNode child : new RelevantNodes(null, node, Axis.CHILD)) {
                        if (child.getNodeName().equals(sem_subject)) {
                            subj = child;
                        } else if (child.getNodeName().equals(sem_predicate)) {
                            pred = child;
                        } else if (child.getNodeName().equals(sem_object)) {
                            obj = child;
                        }
                    }

                    Resource rsrc = getResource(model, subj.getStringValue());
                    Property prop = model.createProperty(pred.getStringValue());

                    String objstr = obj.getStringValue();
                    String lang = obj.getAttributeValue(XProcConstants.xml_lang);
                    String datatype = obj.getAttributeValue(_datatype);

                    if (lang != null) {
                        Literal literal = model.createLiteral(objstr, lang);
                        rsrc.addProperty(prop, literal);
                    } else if (datatype != null) {
                        Literal literal = model.createTypedLiteral(objstr, datatype);
                        rsrc.addProperty(prop, literal);
                    } else {
                        Resource objrsrc = getResource(model, objstr);
                        rsrc.addProperty(prop, objrsrc);
                    }
                } else {
                    // nop
                }
            }
        } else {
            throw new XProcException("Input document is not a sem:triples document");
        }
    }

    protected Resource getResource(Model model, String uri) {
        /* NOTE: You might think this is a good idea, but it's not; if you make them truly
           anonymous in the model, then when they're reserialized they'll get different
           /blank/ URIs and you'll never be able to match them up.
         */

        if (makeAnonymousNodes && uri.startsWith("http://marklogic.com/semantics/blank/")) {
            AnonId id = null;
            if (anonids.containsKey(uri)) {
                id = anonids.get(uri);
            } else {
                id = new AnonId();
                anonids.put(uri, id);
            }

            return model.createResource(id);
        } else {
            return model.createResource(uri);
        }
    }

    protected class ParserErrorHandler implements ErrorHandler {
        String inputfn = "";

        public ParserErrorHandler(String inputfn) {
            this.inputfn = inputfn;
        }

        private String formatMessage(String message, long line, long col) {
            String msg = inputfn + ":";
            if (line >= 0) {
                msg += line;
            }
            if (line >= 0 && col >= 0) {
                msg += ":" + col;
            }
            return msg += " " + message;
        }

        @Override
        public void warning(String message, long line, long col) {
            //
        }

        @Override
        public void error(String message, long line, long col) {
            //
        }

        @Override
        public void fatal(String message, long line, long col) {
            //
        }
    }
}