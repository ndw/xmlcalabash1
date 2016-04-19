package com.xmlcalabash.extensions.fileutils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Vector;

import com.xmlcalabash.core.XMLCalabash;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataReader;
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
        name = "pxf:tail",
        type = "{http://exproc.org/proposed/steps/file}tail " +
                "{http://xmlcalabash.com/ns/extensions/fileutils}tail")

public class Tail extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _count = new QName("count");
    private static final QName _fail_on_error = new QName("fail-on-error");
    private static final QName c_line = new QName("c", XProcConstants.NS_XPROC_STEP, "line");

    private WritablePipe result = null;

    /*
     * Creates a new instance of UriInfo
     */
    public Tail(XProcRuntime runtime, XAtomicStep step) {
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
        int count = getOption(_count, 10);
        final boolean tail = count >= 0;
        final int maxCount = count >= 0 ? count : -count;

        RuntimeValue href = getOption(_href);

        try {
            DataStore store = runtime.getDataStore();
            store.readEntry(href.getString(), href.getBaseURI().toASCIIString(), "text/*, */*", null, new DataReader() {
                public void load(URI id, String media, InputStream content, long len)
                        throws IOException {
                    TreeWriter tree = new TreeWriter(runtime);
                    tree.startDocument(id);
                    tree.addStartElement(XProcConstants.c_result);
                    tree.startContent();

                    Reader rdr = new InputStreamReader(content);
                    BufferedReader brdr = new BufferedReader(rdr);
                    Vector<String> lines = new Vector<String> ();
                    try {
                        int count = 0;
                        String line = brdr.readLine();
                        while (line != null) {
                            count++;
                            lines.add(line);

                            if (count > maxCount) {
                                line = lines.remove(0);
                                if (!tail) {
                                    tree.addStartElement(c_line);
                                    tree.startContent();
                                    tree.addText(line);
                                    tree.addEndElement();
                                    tree.addText("\n");
                                }
                            }

                            line = brdr.readLine();
                        }
                    } finally {
                        brdr.close();
                    }

                    if (tail) {
                        for (String lline : lines) {
                            tree.addStartElement(c_line);
                            tree.startContent();
                            tree.addText(lline);
                            tree.addEndElement();
                            tree.addText("\n");
                        }
                    }

                    tree.addEndElement();
                    tree.endDocument();

                    result.write(tree.getResult());
                }
            });
        } catch (FileNotFoundException fnfe) {
            URI uri = href.getBaseURI().resolve(href.getString());
            throw new XProcException(href.getNode(), "Cannot read: file does not exist: " + uri.toASCIIString());
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }
}
