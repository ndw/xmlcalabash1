package com.xmlcalabash.extensions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.util.TypeUtils;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.om.SingletonAttributeMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataReader;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */


@XMLCalabash(
        name = "cx:java-properties",
        type = "{http://xmlcalabash.com/ns/extensions}java-properties")

public class JavaProperties extends DefaultStep {
    private static final String ACCEPT_TEXT = "text/plain, text/*, */*";
    private static final QName c_param_set = new QName("c", XProcConstants.NS_XPROC_STEP, "param-set");
    private static final QName c_param = new QName("c", XProcConstants.NS_XPROC_STEP, "param");
    private static final QName _href = new QName("","href");
    private static final QName _name = new QName("name");
    private static final QName _namespace = new QName("namespace");
    private static final QName _value = new QName("value");
    private WritablePipe result = null;

    /*
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

        final Properties properties;

        String pFn = null;
        URI pURI = null;

        if (getOption(_href) != null) {
            pFn = getOption(_href).getString();
            pURI = getOption(_href).getBaseURI();
        }

        if (pURI == null) {
            properties = System.getProperties();
        } else {
            properties = new Properties();
            try {
                String base = pURI.toASCIIString();
                DataStore store = runtime.getDataStore();
                store.readEntry(pFn, base, ACCEPT_TEXT, null, new DataReader() {
                    public void load(URI id, String media, InputStream stream,
                            long len) throws IOException {
                        properties.load(stream);
                    }
                });
            } catch (IOException mue) {
                throw new XProcException(XProcException.err_E0001, mue);
            }
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_param_set);

        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            AttributeMap amap = EmptyAttributeMap.getInstance();
            amap = amap.put(TypeUtils.attributeInfo(_name, name));
            amap = amap.put(TypeUtils.attributeInfo(_namespace, ""));
            amap = amap.put(TypeUtils.attributeInfo(_value, value));
            tree.addStartElement(c_param, amap);
            tree.addEndElement();
        }

        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }
}
