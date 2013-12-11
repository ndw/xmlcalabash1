package com.xmlcalabash.extensions;


import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.io.WritablePipe;
import org.xml.sax.InputSource;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

/**
 *
 * @author ndw
 */
public class Unzip extends DefaultStep {
    protected final static QName _href = new QName("", "href");
    protected final static QName _content_type = new QName("", "content-type");
    protected final static QName c_zipfile = new QName("c", XProcConstants.NS_XPROC_STEP, "zipfile");
    protected final static QName c_file = new QName("c", XProcConstants.NS_XPROC_STEP, "file");
    protected final static QName _file = new QName("", "file");
    protected final static QName _charset = new QName("", "charset");
    protected final static QName _name = new QName("", "name");
    protected final static QName c_directory = new QName("c", XProcConstants.NS_XPROC_STEP, "directory");
    protected final static QName _compressed_size = new QName("", "compressed-size");
    protected final static QName _comment = new QName("", "comment");
    protected final static QName _size = new QName("", "size");
    protected final static QName _date = new QName("", "date");

    private WritablePipe result = null;
    private String zipFn = null;
    private URI zipURI = null;
    private String name = null;
    private String contentType = "application/xml";
    private String charset = null;

    /** Creates a new instance of Unzip */
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
        zipURI = getOption(_href).getBaseURI();

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
            URL url = zipURI.resolve(zipFn).toURL();
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();

            ZipInputStream zipFile = new ZipInputStream(stream);

            TreeWriter tree = new TreeWriter(runtime);

            if (name == null) {
                tree.startDocument(step.getNode().getBaseURI());
                tree.addStartElement(c_zipfile);
                tree.addAttribute(_href, url.toString());
                tree.startContent();

                DatatypeFactory dfactory = DatatypeFactory.newInstance();
                GregorianCalendar cal = new GregorianCalendar();

                ZipEntry entry = zipFile.getNextEntry();
                while (entry != null) {
                    cal.setTimeInMillis(entry.getTime());
                    XMLGregorianCalendar xmlCal = dfactory.newXMLGregorianCalendar(cal);

                    if (entry.isDirectory()) {
                        tree.addStartElement(c_directory);
                    } else {
                        tree.addStartElement(c_file);

                        tree.addAttribute(_compressed_size, ""+entry.getCompressedSize());
                        tree.addAttribute(_size, ""+entry.getSize());
                    }

                    if (entry.getComment() != null) {
                        tree.addAttribute(_comment, entry.getComment());
                    }

                    tree.addAttribute(_name, ""+entry.getName());
                    tree.addAttribute(_date, xmlCal.toXMLFormat());
                    tree.startContent();
                    tree.addEndElement();
                    entry = zipFile.getNextEntry();
                }

                zipFile.close();

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
                    boolean storeText = (contentType != null && contentType.startsWith("text/") && charset != null);

                    // There's no point giving the file the URI of the pipeline document.
                    // This formulation is parallel to the jar scheme.
                    URI zipURI = URI.create("zip:" + zipFn + "!" + entry.getName());

                    tree.startDocument(zipURI);
                    tree.addStartElement(XProcConstants.c_data);
                    tree.addAttribute(_name,name);
                    tree.addAttribute(_content_type, contentType);
                    if (!storeText) {
                        tree.addAttribute(_encoding, "base64");
                    }
                    tree.startContent();

                    if (storeText) {
                        InputStreamReader reader = new InputStreamReader(zipFile, charset);
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
                        reader.close();
                    } else {
                        BufferedInputStream bufstream = new BufferedInputStream(zipFile);
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
                        bufstream.close();
                    }

                    tree.addEndElement();
                    tree.endDocument();
                    result.write(tree.getResult());
                }

                zipFile.close();
            }
        } catch (MalformedURLException mue) {
            throw new XProcException(XProcException.err_E0001, mue);
        } catch (IOException ioe) {
            throw new XProcException(XProcException.err_E0001, ioe);
        } catch (DatatypeConfigurationException dce) {
            throw new XProcException(XProcException.err_E0001, dce);
        }
    }
}
