package com.xmlcalabash.drivers;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.OutputURIResolver;
import net.sf.saxon.lib.CollectionURIResolver;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;

import com.xmlcalabash.util.CollectionResolver;
import com.xmlcalabash.model.RuntimeValue;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 27, 2008
 * Time: 11:45:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class BaseURITest {
    public static void main(String[] args) throws SaxonApiException, IOException, URISyntaxException {
        BaseURITest test = new BaseURITest();
        test.run();
    }

    public void run() throws SaxonApiException {
        Processor processor = new Processor(false);
        Configuration config = processor.getUnderlyingConfiguration();

        String textStyle = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'\n"
                + "                version='2.0'>\n"
                + "\n"
                + "<xsl:output method='xml' encoding='utf-8' indent='no'\n"
                + "	    omit-xml-declaration='yes'/>\n"
                + "\n"
                + "<xsl:preserve-space elements='*'/>\n"
                + "\n"
                + "<xsl:template match='/'>\n"
                + "  <xsl:apply-templates/>\n"
                + "</xsl:template>\n"
                + "\n"
                + "<xsl:template match='*'>\n"
                + "  <xsl:copy>\n"
                + "    <xsl:copy-of select='@*'/>\n"
                + "    <xsl:apply-templates/>\n"
                + "  </xsl:copy>\n"
                + "</xsl:template>\n"
                + "\n"
                + "<xsl:template match='comment()|processing-instruction()|text()'>\n"
                + "  <xsl:copy/>\n"
                + "</xsl:template>\n"
                + "\n"
                + "</xsl:stylesheet>\n";


        String textXML = "<doc xml:base='foo/'><para xml:base='bar/'/></doc>";

        SAXSource stylesheet = new SAXSource(new InputSource(new StringReader(textStyle)));

        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable exec = compiler.compile(stylesheet);
        XsltTransformer transformer = exec.load();

        //transformer.getUnderlyingController().setBaseOutputURI("http://example.com/");

        // No resolver, there isn't one here.
        DocumentBuilder builder = processor.newDocumentBuilder();
        SAXSource document = new SAXSource(new InputSource(new StringReader(textXML)));
        XdmNode context = builder.build(document);
        transformer.setInitialContextNode(context);
        XdmDestination result = new XdmDestination();
        transformer.setDestination(result);
        transformer.transform();

        XdmNode xformed = result.getXdmNode();
        System.err.println("Document base: " + xformed.getBaseURI());

        XdmSequenceIterator nodes = xformed.axisIterator(Axis.CHILD);
        XdmNode docNode = (XdmNode) nodes.next();

        nodes = docNode.axisIterator(Axis.CHILD);
        XdmNode paraNode = (XdmNode) nodes.next();

        System.err.println("doc base: " + docNode.getBaseURI());
        System.err.println("para base: " + paraNode.getBaseURI());

    }

}
