package com.xmlcalabash.extensions.fileutils;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: May 24, 2009
 * Time: 3:17:23 PM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "pxf:tempfile",
        type = "{http://exproc.org/proposed/steps/file}tempfile " +
                "{http://xmlcalabash.com/ns/extensions/fileutils}tempfile")

public class Tempfile extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _prefix = new QName("prefix");
    private static final QName _suffix = new QName("suffix");
    private static final QName _delete_on_exit = new QName("delete-on-exit");

    private WritablePipe result = null;

    /**
     * Creates a new instance of UriInfo
     */
    public Tempfile(XProcRuntime runtime, XAtomicStep step) {
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

        String prefix = "temp";
        String suffix = ".xml";
        boolean delete = false;

        RuntimeValue value = getOption(_prefix);
        if (value != null) {
            prefix = value.getString();
        }
        value = getOption(_suffix);
        if (value != null) {
            suffix = value.getString();
        }
        delete = getOption(_delete_on_exit, false);

        RuntimeValue href = getOption(_href);
        URI uri = href.getBaseURI().resolve(href.getString());
        File file;
        if (!"file".equals(uri.getScheme())) {
            throw new XProcException(step.getNode(), "Only file: scheme URIs are supported by the tempfile step.");
        } else {
            file = new File(uri.getPath());
        }

        if (!file.isDirectory()) {
            throw new XProcException(step.getNode(), "The href on tempfile must point to an existing directory.");
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();

        File temp;
        try {
            temp = File.createTempFile(prefix, suffix,file);
        } catch (IOException ioe) {
            throw new XProcException(step.getNode(), "Failed to create temporary file in " + file.toURI().toASCIIString());
        }

        if (delete) {
            temp.deleteOnExit();
        }

        tree.addText(temp.toURI().toASCIIString());

        tree.addEndElement();
        tree.endDocument();

        result.write(tree.getResult());
    }
}