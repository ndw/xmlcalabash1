package com.xmlcalabash.drivers;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.SchemaManager;
import net.sf.saxon.s9api.SchemaValidator;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.Controller;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.PipelineConfiguration;

import java.io.IOException;
import java.net.URISyntaxException;

import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 27, 2008
 * Time: 11:45:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class ValidBaseURITest {
    public static void main(String[] args) throws SaxonApiException, IOException, URISyntaxException {
        ValidBaseURITest test = new ValidBaseURITest();
        test.run();
    }

    public void run() throws SaxonApiException {
        Processor processor = new Processor(true);
        SchemaManager manager = processor.getSchemaManager();

        // No resolver here, there isn't one.
        DocumentBuilder builder = processor.newDocumentBuilder();
        SAXSource source = new SAXSource(new InputSource("http://tests.xproc.org/tests/doc/compoundEntity.xml"));
        XdmNode document = builder.build(source);

        source = new SAXSource(new InputSource("http://tests.xproc.org/tests/doc/document.xsd"));
        XdmNode schema = builder.build(source);
        manager.load(schema.asSource());

        XdmDestination destination = new XdmDestination();
        Controller controller = new Controller(processor.getUnderlyingConfiguration());
        Receiver receiver = destination.getReceiver(controller.getConfiguration());
        PipelineConfiguration pipe = controller.makePipelineConfiguration();
        pipe.setRecoverFromValidationErrors(false);
        receiver.setPipelineConfiguration(pipe);

        SchemaValidator validator = manager.newSchemaValidator();
        validator.setDestination(destination);

        dumpTree(document, "Input");

        validator.validate(document.asSource());

        XdmNode valid = destination.getXdmNode();

        dumpTree(valid, "Output");
    }

    public void dumpTree(XdmNode tree, String message) {
        NodeInfo treeNode = tree.getUnderlyingNode();
        System.err.println(message);

        if (treeNode.getSystemId().equals(tree.getBaseURI().toASCIIString())) {
            System.err.println("Dumping tree: " + treeNode.getSystemId());
        } else {
            System.err.println("Dumping tree:: " + treeNode.getSystemId() + ", " + tree.getBaseURI());
        }
        XdmSequenceIterator iter = tree.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            dumpTreeNode(child, "  ");
        }
    }

    private void dumpTreeNode(XdmNode node, String indent) {
        if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
            System.err.println(indent + node.getNodeName() + ": " + node.getBaseURI());
            XdmSequenceIterator iter = node.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                XdmNode child = (XdmNode) iter.next();
                dumpTreeNode(child, indent + "  ");
            }
        }
    }
}