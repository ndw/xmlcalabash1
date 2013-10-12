package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.source.StreamProcessor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Random;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class RDFa extends DefaultStep {
    private static final QName sem_triples = new QName("sem","http://marklogic.com/semantics", "triples");
    private static final QName sem_triple = new QName("sem","http://marklogic.com/semantics", "triple");
    private static final QName sem_subject = new QName("sem","http://marklogic.com/semantics", "subject");
    private static final QName sem_predicate = new QName("sem","http://marklogic.com/semantics", "predicate");
    private static final QName sem_object = new QName("sem","http://marklogic.com/semantics", "object");
    private static final QName _datatype = new QName("", "datatype");
    private static final QName _max_triples = new QName("", "max-triples-per-document");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private long limit = 100;
    private long count = 0;

    /**
     * Creates a new instance of Identity
     */
    public RDFa(XProcRuntime runtime, XAtomicStep step) {
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

        String limitStr = getOption(_max_triples).getString();
        try {
            limit = Integer.parseInt(limitStr);
        } catch (NumberFormatException nfe) {
            throw XProcException.dynamicError(19, "The max-triples-per-document on cx:rdf-a must be an integer");
        }

        XdmNode doc = source.read();

        try {
            Sink sink = new Sink();
            StreamProcessor sp = new StreamProcessor(RdfaParser.connect(sink));

            // HACK!!!
            // FIXME: set serializer properties appropriately!
            Serializer serializer = makeSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutputWriter(writer);
            S9apiUtils.serialize(runtime, doc, serializer);
            writer.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(writer.toString().getBytes("UTF-8"));
            sp.process(bais, doc.getBaseURI().toASCIIString());
        } catch (IOException e) {
            throw new XProcException(e);
        } catch (ParseException e) {
            throw new XProcException(e);
        }
    }

    private class Sink implements TripleSink {
        TreeWriter tree = null;
        String baseURI = null;
        long randomValue = 0;
        long milliSecs = 0;

        public Sink() {
            Random random = new Random();
            randomValue = random.nextLong();
            Calendar cal = Calendar.getInstance();
            milliSecs = cal.getTimeInMillis();
        }

        @Override
        public void addNonLiteral(String subj, String pred, String obj) {
            /*
            tree.addStartElement(sem_triple);
            tree.startContent();
            tree.addStartElement(sem_subject);
            tree.startContent();
            tree.addText(patchURI(subj));
            tree.addEndElement();
            tree.addStartElement(sem_predicate);
            tree.startContent();
            tree.addText(patchURI(pred));
            tree.addEndElement();
            tree.addStartElement(sem_object);
            tree.startContent();
            tree.addText(patchURI(obj));
            tree.addEndElement();
            tree.addEndElement();
            */
            nextFile();
        }

        @Override
        public void addPlainLiteral(String subj, String pred, String obj, String lang) {
            tree.addStartElement(sem_triple);
            tree.startContent();
            tree.addStartElement(sem_subject);
            tree.startContent();
            tree.addText(patchURI(subj));
            tree.addEndElement();
            tree.addStartElement(sem_predicate);
            tree.startContent();
            tree.addText(patchURI(pred));
            tree.addEndElement();
            tree.addStartElement(sem_object);

            if (lang == null || "".equals(lang)) {
                tree.addAttribute(_datatype, "http://www.w3.org/2001/XMLSchema#string");
            } else {
                tree.addAttribute(XProcConstants.xml_lang,lang);
            }

            tree.startContent();
            tree.addText(obj);
            tree.addEndElement();
            tree.addEndElement();
            nextFile();
        }

        @Override
        public void addTypedLiteral(String subj, String pred, String obj, String datatype) {
            if (datatype == null) {
                datatype = "http://www.w3.org/2001/XMLSchema#string";
            }
            tree.addStartElement(sem_triple);
            tree.startContent();
            tree.addStartElement(sem_subject);
            tree.startContent();
            tree.addText(patchURI(subj));
            tree.addEndElement();
            tree.addStartElement(sem_predicate);
            tree.startContent();
            tree.addText(patchURI(pred));
            tree.addEndElement();
            tree.addStartElement(sem_object);
            tree.addAttribute(_datatype, datatype);
            tree.startContent();
            tree.addText(obj);
            tree.addEndElement();
            tree.addEndElement();
            nextFile();
        }

        @Override
        public void setBaseUri(String s) {
            baseURI = s;
        }

        @Override
        public void startStream() throws ParseException {
            tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(sem_triples);
            tree.startContent();
        }

        @Override
        public void endStream() throws ParseException {
            tree.addEndElement();
            tree.endDocument();
            if (count > 0) {
                XdmNode out = tree.getResult();
                result.write(out);
            }
        }

        @Override
        public boolean setProperty(String key, Object value) {
            return false;
        }

        private void nextFile() {
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

                count = 0;
            }
        }

        private String patchURI(String uri) {
            if (uri.startsWith("_:")) {
                return "http://marklogic.com/semantics/blank/"
                        + Long.toHexString(fuse(scramble(milliSecs),randomValue))
                        + "/" + uri;
            } else {
                return uri;
            }
        }

        private long rotl(long x, long y)
        {
            return (x<<y)^(x>>(64-y));
        }

        private long fuse(long a, long b)
        {
            return rotl(a,8)^b;
        }

        private long scramble(long x)
        {
            return x^rotl(x,20)^rotl(x,40);
        }
    }
}