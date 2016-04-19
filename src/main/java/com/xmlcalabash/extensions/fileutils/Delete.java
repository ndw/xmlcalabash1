package com.xmlcalabash.extensions.fileutils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
        name = "pxf:delete",
        type = "{http://exproc.org/proposed/steps/file}delete " +
                "{http://xmlcalabash.com/ns/extensions/fileutils}delete")

public class Delete extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _recursive = new QName("recursive");
    private static final QName _fail_on_error = new QName("fail-on-error");

    private WritablePipe result = null;

    /*
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
        String base = href.getBaseURI().toASCIIString();
        URI uri = href.getBaseURI().resolve(href.getString());

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();

        tree.addText(uri.toASCIIString());

        performDelete(href, base, recursive, fail_on_error);

        tree.addEndElement();
        tree.endDocument();

        result.write(tree.getResult());
    }

    private void performDelete(RuntimeValue href, String base,
            boolean recursive, boolean fail_on_error) {
        DataStore store = runtime.getDataStore();
        if (recursive) {
            try {
                for (String entry : getAllEntries(href.getString(), base)) {
                    store.deleteEntry(entry, entry);
                }
            } catch (FileNotFoundException e) {
                if (fail_on_error) {
                    throw new XProcException(step.getNode(), "Cannot delete: file does not exist", e);
                }
            } catch (IOException e) {
                if (fail_on_error) {
                    throw new XProcException(step.getNode(), e);
                }
            }
        }
        try {
            store.deleteEntry(href.getString(), base);
        } catch (FileNotFoundException e) {
            if (fail_on_error) {
                throw new XProcException(step.getNode(), "Cannot delete: file does not exist", e);
            }
        } catch (IOException e) {
            if (fail_on_error) {
                throw new XProcException(step.getNode(), e);
            }
        }
    }

    private List<String> getAllEntries(final String href, String base)
            throws MalformedURLException, FileNotFoundException, IOException {
        final List<String> entries = new ArrayList<String>();
        DataStore store = runtime.getDataStore();
        store.listEachEntry(href, base, "*/*", new DataStore.DataInfo() {
            public void list(URI id, String media, long lastModified)
                    throws IOException {
                String entry = id.toASCIIString();
                try {
                    entries.addAll(getAllEntries(entry, entry));
                } catch (FileNotFoundException e) {
                    // ignore it, we tried to recurse through a file that wasn't a directory
                }
                entries.add(entry);
            }
        });
        return entries;
    }
}
