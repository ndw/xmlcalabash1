package com.xmlcalabash.extensions.fileutils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import com.xmlcalabash.core.XMLCalabash;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataInfo;
import com.xmlcalabash.io.DataStore.DataWriter;
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
        name = "pxf:touch",
        type = "{http://exproc.org/proposed/steps/file}touch " +
                "{http://xmlcalabash.com/ns/extensions/fileutils}touch")

public class Touch extends DefaultStep {
    private static final QName _href = new QName("href");

    private WritablePipe result = null;

    /**
     * Creates a new instance of UriInfo
     */
    public Touch(XProcRuntime runtime, XAtomicStep step) {
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

        final TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();

        try {
            DataStore store = runtime.getDataStore();
            String base = href.getBaseURI().toASCIIString();
            try {
                store.infoEntry(href.getString(), base, "*/*", new DataInfo() {
                    public void list(URI id, String media, long lastModified)
                            throws IOException {
                        // file already exists
                        tree.addText(id.toASCIIString());
                    }
                });
            } catch (FileNotFoundException e) {
                URI uri = store.writeEntry(href.getString(), base, "text/plain", new DataWriter() {
                    public void store(OutputStream content) throws IOException {
                        // empty file
                    }
                });
                tree.addText(uri.toASCIIString());
            }
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
        
        tree.addEndElement();
        tree.endDocument();

        result.write(tree.getResult());
    }
}
