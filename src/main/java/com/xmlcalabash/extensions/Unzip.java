package com.xmlcalabash.extensions;


import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataReader;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.TypeUtils;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.om.SingletonAttributeMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.tree.iter.SingleAtomicIterator;
import org.xml.sax.InputSource;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "pxp:unzip",
        type = "{http://exproc.org/proposed/steps}unzip " +
                "{http://xmlcalabash.com/ns/extensions}unzip")

public class Unzip extends DefaultStep {
    private static final String ACCEPT_ZIP = "application/zip, */*";
    protected final static QName _href = new QName("", "href");
    protected final static QName _content_type = new QName("", "content-type");
    protected final static QName c_zipfile = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "zipfile");
    protected final static QName c_file = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "file");
    protected final static QName _file = new QName("", "file");
    protected final static QName _charset = new QName("", "charset");
    protected final static QName _name = new QName("", "name");
    protected final static QName c_directory = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "directory");
    protected final static QName _compressed_size = new QName("", "compressed-size");
    protected final static QName _comment = new QName("", "comment");
    protected final static QName _size = new QName("", "size");
    protected final static QName _date = new QName("", "date");

    private WritablePipe result = null;
    private String zipFn = null;
    private String name = null;
    private String contentType = "application/xml";
    private String charset = null;

    /* Creates a new instance of Unzip */
    public Unzip(XProcRuntime runtime, XAtomicStep step) {
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
        
        zipFn = getOption(_href).getString();
        URI zipURI = getOption(_href).getBaseURI();

        if (getOption(_file) != null) {
            name = getOption(_file).getString();
        }

        if (getOption(_content_type) != null) {
            contentType = getOption(_content_type).getString();
        }

        if (getOption(_charset) != null) {
            charset = getOption(_charset).getString();
        }

        try {
            final DatatypeFactory dfactory = DatatypeFactory.newInstance();
            DataStore store = runtime.getDataStore();
            String base = zipURI.toASCIIString();
            store.readEntry(zipFn, base, ACCEPT_ZIP, null, new DataReader() {
                public void load(URI id, String media, InputStream content,
                        long len) throws IOException {
                    unzip(dfactory, id.toASCIIString(), content);
                }
            });
        } catch (IOException | DatatypeConfigurationException mue) {
            throw new XProcException(XProcException.err_E0001, mue);
        }
    }

    void unzip(DatatypeFactory dfactory, String systemId, InputStream stream) throws IOException {
        try (ZipInputStream zipFile = new ZipInputStream(stream)) {
            TreeWriter tree = new TreeWriter(runtime);

            if (name == null) {
                tree.startDocument(step.getNode().getBaseURI());
                tree.addStartElement(c_zipfile, SingletonAttributeMap.of(TypeUtils.attributeInfo(_href, systemId)));

                GregorianCalendar cal = new GregorianCalendar();

                ZipEntry entry = zipFile.getNextEntry();
                while (entry != null) {
                    cal.setTimeInMillis(entry.getTime());
                    XMLGregorianCalendar xmlCal = dfactory.newXMLGregorianCalendar(cal);
                    AttributeMap attr = EmptyAttributeMap.getInstance();

                    if (!entry.isDirectory()) {
                        attr = attr.put(TypeUtils.attributeInfo(_compressed_size, "" + entry.getCompressedSize()));
                        attr = attr.put(TypeUtils.attributeInfo(_size, "" + entry.getSize()));
                    }

                    if (entry.getComment() != null) {
                        attr = attr.put(TypeUtils.attributeInfo(_comment, entry.getComment()));
                    }

                    attr = attr.put(TypeUtils.attributeInfo(_name, "" + entry.getName()));
                    attr = attr.put(TypeUtils.attributeInfo(_date, xmlCal.toXMLFormat()));

                    if (entry.isDirectory()) {
                        tree.addStartElement(c_directory, attr);
                    } else {
                        tree.addStartElement(c_file, attr);
                    }

                    tree.addEndElement();
                    entry = zipFile.getNextEntry();
                }

                tree.addEndElement();
                tree.endDocument();
                result.write(tree.getResult());
            } else {
                ZipEntry entry = zipFile.getNextEntry();
                while (entry != null) {
                    if (name.equals(entry.getName())) {
                        break;
                    }
                    entry = zipFile.getNextEntry();
                }

                if (entry == null) {
                    throw new XProcException(step.getNode(), "ZIP file does not contain '" + name + "'");
                }

                if ("application/xml".equals(contentType) || "text/xml".equals(contentType)
                        || contentType.endsWith("+xml")) {
                    InputSource isource = new InputSource(zipFile);
                    XdmNode doc = runtime.parse(isource);
                    result.write(doc);
                } else {
                    boolean storeText = (contentType.startsWith("text/") && charset != null);
                    AttributeMap attr = EmptyAttributeMap.getInstance();

                    // There's no point giving the file the URI of the pipeline document.
                    // This formulation is parallel to the jar scheme.
                    URI zipURI = URI.create("zip:" + zipFn + "!" + URLEncoder.encode(entry.getName(), "UTF-8"));

                    tree.startDocument(zipURI);
                    attr = attr.put(TypeUtils.attributeInfo(_name, name));
                    attr = attr.put(TypeUtils.attributeInfo(_content_type, contentType));
                    if (!storeText) {
                        attr = attr.put(TypeUtils.attributeInfo(_encoding, "base64"));
                    }
                    tree.addStartElement(XProcConstants.c_data, attr);

                    if (storeText) {
                        try (InputStreamReader reader = new InputStreamReader(zipFile, charset)) {
                            int maxlen = 4096;
                            char[] chars = new char[maxlen];
                            int read = reader.read(chars, 0, maxlen);
                            while (read >= 0) {
                                if (read > 0) {
                                    String s = new String(chars);
                                    tree.addText(s);
                                }
                                read = reader.read(chars, 0, maxlen);
                            }
                        }
                    } else {
                        try (BufferedInputStream bufstream = new BufferedInputStream(zipFile)) {
                            int maxlen = 4096 * 3;
                            byte[] bytes = new byte[maxlen];
                            int read = bufstream.read(bytes, 0, maxlen);
                            while (read >= 0) {
                                if (read > 0) {
                                    String base64 = Base64.encodeBytes(bytes, 0, read);
                                    tree.addText(base64 + "\n");
                                }
                                read = bufstream.read(bytes, 0, maxlen);
                            }
                        }
                    }

                    tree.addEndElement();
                    tree.endDocument();
                    result.write(tree.getResult());
                }
            }
        }
    }
}
