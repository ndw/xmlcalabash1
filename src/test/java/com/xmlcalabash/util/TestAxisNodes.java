package com.xmlcalabash.util;

import com.xmlcalabash.util.AxisNodes;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.s9api.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;

/**
 * Created by ndw on 8/16/14.
 */
public class TestAxisNodes {
    private static Processor processor = new Processor(true);
    private static XdmNode root = null;

    // FIXME: This test should check for USE_WHEN but I'm too lazy to construct a runtime

    @BeforeClass
    public static void setupClass() throws Exception {
        String xml = "<document><?foo?><p>A paragraph</p> <p>A paragraph</p> some text ";
        xml += "<p>Another paragraph</p><!--comment-->";
        xml += "<documentation xmlns='http://www.w3.org/ns/xproc'>some doc</documentation></document>";

        DocumentBuilder builder = processor.newDocumentBuilder();
        root = S9apiUtils.getDocumentElement(builder.build(new SAXSource(new InputSource(new StringReader(xml)))));
    }

    @Test
    public void testALL() {
        AxisNodes an = new AxisNodes(root, Axis.CHILD, AxisNodes.ALL);

        XdmNode n = an.iterator().next();
        assertEquals(XdmNodeKind.PROCESSING_INSTRUCTION, n.getNodeKind());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("", "p"), n.getNodeName());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.TEXT, n.getNodeKind());
        assertEquals(" ", n.getStringValue());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("", "p"), n.getNodeName());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.TEXT, n.getNodeKind());
        assertEquals(" some text ", n.getStringValue());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("", "p"), n.getNodeName());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.COMMENT, n.getNodeKind());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("http://www.w3.org/ns/xproc", "documentation"), n.getNodeName());

        assertEquals(false, an.iterator().hasNext());
    }

    @Test
    public void testSIGNIFICANT() {
        AxisNodes an = new AxisNodes(root, Axis.CHILD, AxisNodes.SIGNIFICANT);

        XdmNode n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("", "p"), n.getNodeName());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("", "p"), n.getNodeName());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.TEXT, n.getNodeKind());
        assertEquals(" some text ", n.getStringValue());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("", "p"), n.getNodeName());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("http://www.w3.org/ns/xproc", "documentation"), n.getNodeName());

        assertEquals(false, an.iterator().hasNext());
    }

    @Test
    public void testPIPELINE() {
        AxisNodes an = new AxisNodes(null, root, Axis.CHILD, AxisNodes.PIPELINE);

        XdmNode n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("", "p"), n.getNodeName());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("", "p"), n.getNodeName());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.TEXT, n.getNodeKind());
        assertEquals(" some text ", n.getStringValue());

        n = an.iterator().next();
        assertEquals(XdmNodeKind.ELEMENT, n.getNodeKind());
        assertEquals(new QName("", "p"), n.getNodeName());

        assertEquals(false, an.iterator().hasNext());
    }
}
