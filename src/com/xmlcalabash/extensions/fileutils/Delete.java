package com.xmlcalabash.extensions.fileutils;

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
import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: May 24, 2009
 * Time: 3:17:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class Delete extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _recursive = new QName("recursive");
    private static final QName _fail_on_error = new QName("fail-on-error");

    private WritablePipe result = null;

    /**
     * Creates a new instance of UriInfo
     */
    public Delete(XProcRuntime runtime, XAtomicStep step) {
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

        boolean recursive = getOption(_recursive, false);
        boolean fail_on_error = getOption(_fail_on_error, true);
        
        RuntimeValue href = getOption(_href);
        URI uri = href.getBaseURI().resolve(href.getString());
        File file;
        if (!"file".equals(uri.getScheme())) {
            throw new XProcException(step.getNode(), "Only file: scheme URIs are supported by the delete step.");
        } else {
            file = new File(uri.getPath());
        }

        if (!file.exists()) {
            if (fail_on_error) {
                throw new XProcException(step.getNode(), "Cannot delete: file does not exist: " + file.getAbsolutePath());
            }
        } else {

            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(XProcConstants.c_result);
            tree.startContent();

            tree.addText(file.toURI().toASCIIString());

            performDelete(file, recursive, fail_on_error);

            tree.addEndElement();
            tree.endDocument();

            result.write(tree.getResult());
        }
    }
    
    private void performDelete(File file, boolean recursive, boolean fail_on_error) {
        if (recursive && file.isDirectory()) {
            for (File f : file.listFiles()) {
                performDelete(f, recursive, fail_on_error);
            }
        }

        if (!file.delete() && fail_on_error) {
            throw new XProcException(step.getNode(), "Delete failed for: " + file.getAbsolutePath());
        }
    }
}