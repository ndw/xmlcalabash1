package com.xmlcalabash.extensions.marklogic;

import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.Configuration;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.model.RuntimeValue;
import com.marklogic.xcc.*;
import com.marklogic.xcc.types.*;
import com.marklogic.xcc.types.XdmItem;

import javax.xml.transform.sax.SAXSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;
import java.util.Hashtable;
import java.util.Locale;

import org.xml.sax.InputSource;


/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 4, 2008
 * Time: 11:24:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class XCCInsertDocument extends DefaultStep {
    private static final QName _user = new QName("","user");
    private static final QName _password = new QName("","password");
    private static final QName _host = new QName("","host");
    private static final QName _port = new QName("","port");
    private static final QName _contentBase = new QName("","content-base");
    private static final QName _bufferSize = new QName("","buffer-size");
    private static final QName _collections = new QName("","collections");
    private static final QName _format = new QName("","format");
    private static final QName _language = new QName("","language");
    private static final QName _locale = new QName("","locale");
    private static final QName _uri = new QName("","uri");
    private static final QName _encoding = new QName("encoding");
    private static final QName _auth_method = new QName("auth-method");

    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Identity
     */
    public XCCInsertDocument(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        String host = getOption(_host, "");
        int port = getOption(_port, 0);
        String user = getOption(_user, "");
        String password = getOption(_password, "");
        String contentBase = getOption(_contentBase, "");

        String format = "xml";
        if (getOption(_format) != null) {
            format = getOption(_format).getString();
        }
        if (!"xml".equals(format) && !"text".equals(format) && !"binary".equals(format)) {
            throw new UnsupportedOperationException("Format must be 'xml', 'text', or 'binary'.");
        }

        XdmNode doc = source.read();
        XdmNode root = S9apiUtils.getDocumentElement(doc);

        String docstring = null;
        byte[] docbinary = null;

        if ("xml".equals(format)) {
            Serializer serializer = makeSerializer();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            serializer.setOutputStream(stream);
            S9apiUtils.serialize(runtime, doc, serializer);

            try {
                docstring = stream.toString("UTF-8");
            } catch (UnsupportedEncodingException uee) {
                // This can't happen...
                throw new XProcException(uee);
            }
        } else if ("text".equals(format)) {
            docstring = doc.getStringValue();
        } else {
            if ("base64".equals(root.getAttributeValue(_encoding))) {
                docbinary = Base64.decode(doc.getStringValue());
            } else if (root.getAttributeValue(_encoding) == null) {
                docstring = root.getStringValue();
            } else {
                throw new UnsupportedOperationException("Binary content must be base64 encoded.");
            }
        }

        ContentCreateOptions options = ContentCreateOptions.newXmlInstance();

        if ("xml".equals(format)) {
            options.setFormatXml();
            options.setEncoding("UTF-8");
        }

        if ("text".equals(format)) {
            options.setFormatText();
            options.setEncoding("UTF-8");
        }

        if ("binary".equals(format)) {
            options.setFormatBinary();
        }

        if (getOption(_bufferSize) != null) {
            options.setBufferSize(getOption(_bufferSize).getInt());
        }
        if (getOption(_collections) != null) {
            String[] collections = getOption(_collections).getString().split("\\s+");
            options.setCollections(collections);
        }
        if (getOption(_language) != null) {
            options.setLanguage(getOption(_language).getString());
        }
        if (getOption(_locale) != null) {
            String value = getOption(_locale).getString();
            Locale locale = new Locale(value);
            options.setLocale(locale);
        }

        String dburi = getOption(_uri).getString();

        Content content = null;
        if (docbinary == null) {
            content = ContentFactory.newContent(dburi, docstring, options);
        } else {
            content = ContentFactory.newContent(dburi, docbinary, options);
        }

        ContentSource contentSource;

        try {
            if ("".equals(contentBase)) {
                contentSource = ContentSourceFactory.newContentSource(host, port, user, password);
            } else {
                contentSource = ContentSourceFactory.newContentSource(host, port, user, password, contentBase);
            }
        } catch (Exception e) {
            throw new XProcException(e);
        }

        if ("basic".equals(getOption(_auth_method, ""))) {
            contentSource.setAuthenticationPreemptive(true);
        }

        Session session;

        try {
            session = contentSource.newSession ();
            session.insertContent(content);
            session.close();
        } catch (Exception e) {
            throw new XProcException(e);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText(dburi);
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }
}