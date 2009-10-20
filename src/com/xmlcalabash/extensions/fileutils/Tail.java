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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.net.URI;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: May 24, 2009
 * Time: 3:17:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class Tail extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _count = new QName("count");
    private static final QName _fail_on_error = new QName("fail-on-error");
    private static final QName c_line = new QName("c", XProcConstants.NS_XPROC_STEP, "line");

    private WritablePipe result = null;

    /**
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
        int maxCount = getOption(_count, 10);

        RuntimeValue href = getOption(_href);
        URI uri = href.getBaseURI().resolve(href.getString());
        File file;
        if (!"file".equals(uri.getScheme())) {
            throw new XProcException("Only file: scheme URIs are supported by the copy step.");
        } else {
            file = new File(uri.getPath());
        }

        if (!file.exists()) {
             throw new XProcException("Cannot read: file does not exist: " + file.getAbsolutePath());
        }

        if (file.isDirectory()) {
             throw new XProcException("Cannot read: file is a directory: " + file.getAbsolutePath());
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();

        try {
            FileReader rdr = new FileReader(file);
            BufferedReader brdr = new BufferedReader(rdr);
            Vector<String> lines = new Vector<String> ();
            int count = 0;
            String line = brdr.readLine();
            while (line != null) {
                count++;
                lines.add(line);

                if (count > maxCount) {
                    lines.remove(0);
                }

                line = brdr.readLine();
            }

            for (String lline : lines) {
                tree.addStartElement(c_line);
                tree.startContent();
                tree.addText(lline);
                tree.addEndElement();
                tree.addText("\n");
            }

            brdr.close();
            rdr.close();
        } catch (FileNotFoundException fnfe) {
            throw new XProcException(fnfe);
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }

        tree.addEndElement();
        tree.endDocument();

        result.write(tree.getResult());
    }
}