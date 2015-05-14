package com.xmlcalabash.drivers;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * Created by ndw on 5/14/15.
 *
 * This class attempts to test how XML Calabash performs when it's embedded in other applications
 */
public class EmbeddedTest {
    String pipeline_xml = "<p:declare-step xmlns:p=\"http://www.w3.org/ns/xproc\" version=\"1.0\"\n" +
            "                xmlns:c=\"http://www.w3.org/ns/xproc-step\"\n" +
            "                xmlns:cx=\"http://xmlcalabash.com/ns/extensions\"\n" +
            "                xmlns:exf=\"http://exproc.org/standard/functions\"\n" +
            "                exclude-inline-prefixes=\"cx exf\"\n" +
            "                name=\"main\">\n" +
            "<p:output port=\"result\"/>\n" +
            "\n" +
            "<p:identity>\n" +
            "  <p:input port=\"source\">\n" +
            "    <p:inline><doc/></p:inline>\n" +
            "  </p:input>\n" +
            "</p:identity>\n" +
            "\n" +
            "</p:declare-step>\n";

    public static void main(String[] args) throws SaxonApiException, IOException, URISyntaxException {
        EmbeddedTest test = new EmbeddedTest();
        test.run();
    }

    public void run() throws SaxonApiException {
        Processor saxon = new Processor(false);
        XProcConfiguration config = new XProcConfiguration(saxon);
        XProcRuntime runtime = new XProcRuntime(config);

        InputStream stream = new ByteArrayInputStream(pipeline_xml.getBytes());
        DocumentBuilder builder = saxon.newDocumentBuilder();
        XdmNode pipeline_doc = builder.build(new SAXSource(new InputSource(stream)));

        XPipeline pipeline = runtime.use(pipeline_doc);
        pipeline.run();
    }

}
