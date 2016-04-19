package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.AxisNodes;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.*;

import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "pxp:set-base-uri",
        type = "{http://exproc.org/proposed/steps}set-base-uri")

public class SetBaseURI extends DefaultStep {
    private static final QName _uri = new QName("","uri");
    private XProcRuntime runtime = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private TreeWriter tree = null;
    private URI baseURI = null;

    /*
     * Creates a new instance of SetBaseURI
     */
    public SetBaseURI(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
        this.runtime = runtime;
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

        String uris = getOption(_uri, (String) null);
        if (uris == null) {
            throw new XProcException("URI is required");
        }

        baseURI = getOption(_uri).getBaseURI().resolve(uris);

        XdmNode doc = source.read();
        tree = new TreeWriter(runtime);
        tree.startDocument(baseURI);

        for (XdmNode node : new AxisNodes(doc, Axis.CHILD)) {
            write(node,false);
        }

        tree.endDocument();

        result.write(tree.getResult());
    }

    private void write(XdmNode node, boolean underXmlBase) {
        switch (node.getNodeKind()) {
            case ELEMENT:
                underXmlBase = underXmlBase || (node.getAttributeValue(XProcConstants.xml_base) != null);
                if (underXmlBase) {
                    tree.addStartElement(node);
                } else {
                    tree.addStartElement(node, baseURI);
                }
                for (XdmNode child : new AxisNodes(node, Axis.ATTRIBUTE)) {
                    tree.addAttribute(child);
                }
                for (XdmNode child : new AxisNodes(node, Axis.CHILD)) {
                    write(child,underXmlBase);
                }
                tree.addEndElement();
                break;
            case TEXT:
                tree.addSubtree(node);
                break;
            case COMMENT:
                tree.addSubtree(node);
                break;
            case PROCESSING_INSTRUCTION:
                tree.addSubtree(node);
                break;
            default:
                throw new XProcException("Unexpected node kind!?");
        }
    }
}
