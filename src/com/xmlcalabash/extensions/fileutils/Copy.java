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
import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: May 24, 2009
 * Time: 3:17:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class Copy extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _target = new QName("target");
    private static final QName _fail_on_error = new QName("fail-on-error");
    private static final int bufsize = 8192;

    private WritablePipe result = null;

    /**
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
        URI uri = href.getBaseURI().resolve(href.getString());
        File file;
        if (!"file".equals(uri.getScheme())) {
            throw new XProcException("Only file: scheme URIs are supported by the copy step.");
        } else {
            file = new File(uri.getPath());
        }

        if (!file.exists()) {
             throw new XProcException("Cannot copy: file does not exist: " + file.getAbsolutePath());
        }

        if (file.isDirectory()) {
             throw new XProcException("Cannot copy: file is a directory: " + file.getAbsolutePath());
        }

        href = getOption(_target);
        uri = href.getBaseURI().resolve(href.getString());
        File target;
        if (!"file".equals(uri.getScheme())) {
            throw new XProcException("Only file: scheme URIs are supported by the copy step.");
        } else {
            target = new File(uri.getPath());
        }

        if (target.isDirectory()) {
            target = new File(target, file.getName());
            if (target.isDirectory()) {
                throw new XProcException("Cannot copy: target is a directory: " + target.getAbsolutePath());
            }
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();

        tree.addText(target.toURI().toASCIIString());

        try {
            FileInputStream src = new FileInputStream(file);
            FileOutputStream dst = new FileOutputStream(target);
            byte[] buffer = new byte[bufsize];
            int read = src.read(buffer, 0, bufsize);
            while (read >= 0) {
                dst.write(buffer, 0, read);
                read = src.read(buffer, 0, bufsize);
            }
            src.close();
            dst.close();
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