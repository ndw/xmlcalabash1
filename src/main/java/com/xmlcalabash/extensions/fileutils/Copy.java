package com.xmlcalabash.extensions.fileutils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import com.xmlcalabash.core.XMLCalabash;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataReader;
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
        name = "pxf:copy",
        type = "{http://exproc.org/proposed/steps/file}copy " +
                "{http://xmlcalabash.com/ns/extensions/fileutils}copy")

public class Copy extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _target = new QName("target");
    private static final QName _fail_on_error = new QName("fail-on-error");
    private static final int bufsize = 8192;

    private WritablePipe result = null;

    /*
     * Creates a new instance of UriInfo
     */
    public Copy(XProcRuntime runtime, XAtomicStep step) {
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

        boolean failOnError = getOption(_fail_on_error, true);

        RuntimeValue href = getOption(_href);

        try {
            final DataStore store = runtime.getDataStore();
            store.readEntry(href.getString(), href.getBaseURI().toASCIIString(), "*/*", null, new DataReader() {
                public void load(URI id, String media, final InputStream src, long len)
                        throws IOException {
                    RuntimeValue target = getOption(_target);
                    URI uri = store.writeEntry(target.getString(), target.getBaseURI().toASCIIString(), media, new DataWriter() {
                        public void store(OutputStream dst) throws IOException {
                            byte[] buffer = new byte[bufsize];
                            int read = src.read(buffer, 0, bufsize);
                            while (read >= 0) {
                                dst.write(buffer, 0, read);
                                read = src.read(buffer, 0, bufsize);
                            }
                        }
                    });

                    TreeWriter tree = new TreeWriter(runtime);
                    tree.startDocument(step.getNode().getBaseURI());
                    tree.addStartElement(XProcConstants.c_result);
                    tree.startContent();

                    tree.addText(uri.toASCIIString());

                    tree.addEndElement();
                    tree.endDocument();

                    result.write(tree.getResult());
                }
            });
        } catch (FileNotFoundException fnfe) {
            URI uri = href.getBaseURI().resolve(href.getString());
            throw new XProcException(step.getNode(), "Cannot copy: file does not exist: " + uri.toASCIIString());
        } catch (IOException ioe) {
            throw new XProcException(step.getNode(), ioe);
        }
    }
}
