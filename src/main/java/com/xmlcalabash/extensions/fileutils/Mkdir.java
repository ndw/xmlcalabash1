package com.xmlcalabash.extensions.fileutils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import com.xmlcalabash.core.XMLCalabash;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: May 24, 2009
 * Time: 3:17:23 PM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "pxf:mkdir",
        type = "{http://exproc.org/proposed/steps/file}mkdir " +
                "{http://xmlcalabash.com/ns/extensions/fileutils}mkdir")

public class Mkdir extends DefaultStep {
    private static final QName _href = new QName("href");

    private WritablePipe result = null;

    /*
     * Creates a new instance of UriInfo
     */
    public Mkdir(XProcRuntime runtime, XAtomicStep step) {
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

        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        RuntimeValue href = getOption(_href);

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();

        try {
            DataStore store = runtime.getDataStore();
            URI uri = store.createList(href.getString(), href.getBaseURI().toASCIIString());
            tree.addText(uri.toASCIIString());
        } catch (FileNotFoundException e) {
            throw new XProcException(step.getNode(), "Cannot mkdir: file exists: " + href.getString());
        } catch (IOException e) {
            throw new XProcException(step.getNode(), "Mkdir failed for: " + href.getString());
        }

        tree.addEndElement();
        tree.endDocument();

        result.write(tree.getResult());
    }
}
