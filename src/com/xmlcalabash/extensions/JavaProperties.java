package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class JavaProperties extends DefaultStep {
    private static final QName c_param_set = new QName("c", XProcConstants.NS_XPROC_STEP, "param-set");
    private static final QName c_param = new QName("c", XProcConstants.NS_XPROC_STEP, "param");
    private static final QName _href = new QName("","href");
    private static final QName _name = new QName("name");
    private static final QName _namespace = new QName("namespace");
    private static final QName _value = new QName("value");
    private WritablePipe result = null;

    /**
     * Creates a new instance of Identity
     */
    public JavaProperties(XProcRuntime runtime, XAtomicStep step) {
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

        Properties properties = new Properties();

        String pFn = null;
        URI pURI = null;

        if (getOption(_href) != null) {
            pFn = getOption(_href).getString();
            pURI = getOption(_href).getBaseURI();
        }

        if (pURI == null) {
            properties = System.getProperties();
        } else {
            try {
                URL url = pURI.resolve(pFn).toURL();
                URLConnection connection = url.openConnection();
                InputStream stream = connection.getInputStream();
                properties.load(stream);
            } catch (MalformedURLException mue) {
                throw new XProcException(XProcException.err_E0001, mue);
            } catch (IOException ioe) {
                throw new XProcException(XProcException.err_E0001, ioe);
            }
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_param_set);
        tree.startContent();

        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            tree.addStartElement(c_param);
            tree.addAttribute(_name, name);
            tree.addAttribute(_namespace, "");
            tree.addAttribute(_value, value);
            tree.startContent();
            tree.addEndElement();
        }

        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }
}