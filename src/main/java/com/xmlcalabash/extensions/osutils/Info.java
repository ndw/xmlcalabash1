package com.xmlcalabash.extensions.osutils;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.TypeUtils;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import java.net.URI;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Jun 3, 2009
 * Time: 7:48:27 PM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "pos:info",
        type = "{http://exproc.org/proposed/steps/os}info " +
                "{http://xmlcalabash.com/ns/extensions/osutils}info")

public class Info extends DefaultStep {
    private WritablePipe result = null;

    /*
     * Creates a new instance of UriInfo
     */
    public Info(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        TreeWriter tree = new TreeWriter(runtime);
        AttributeMap attr = EmptyAttributeMap.getInstance();

        tree.startDocument(step.getNode().getBaseURI());

        attr = attr.put(TypeUtils.attributeInfo(new QName("file-separator"), System.getProperty("file.separator")));
        attr = attr.put(TypeUtils.attributeInfo(new QName("path-separator"), System.getProperty("path.separator")));
        attr = attr.put(TypeUtils.attributeInfo(new QName("os-architecture"), System.getProperty("os.arch")));
        attr = attr.put(TypeUtils.attributeInfo(new QName("os-name"), System.getProperty("os.name")));
        attr = attr.put(TypeUtils.attributeInfo(new QName("os-version"), System.getProperty("os.version")));
        attr = attr.put(TypeUtils.attributeInfo(new QName("cwd"), System.getProperty("user.dir")));
        attr = attr.put(TypeUtils.attributeInfo(new QName("user-name"), System.getProperty("user.name")));
        attr = attr.put(TypeUtils.attributeInfo(new QName("user-home"), System.getProperty("user.home")));

        tree.addStartElement(XProcConstants.c_result, attr);
        tree.addEndElement();
        tree.endDocument();

        result.write(tree.getResult());
    }
}
