package com.xmlcalabash.extensions;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.Deflater;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.library.HttpRequest;
import com.xmlcalabash.runtime.XAtomicStep;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.Serializer;

/**
 *
 * @author ndw
 */
public class Zip extends DefaultStep {
    protected final static QName _href = new QName("", "href");
    protected final static QName _name = new QName("", "name");
    protected final static QName _command = new QName("", "command");
    protected final static QName _compression_method = new QName("", "compression-method");
    protected final static QName _compression_level = new QName("", "compression-level");
    protected final static QName c_zip_manifest = new QName("c", XProcConstants.NS_XPROC_STEP, "zip-manifest");
    protected final static QName c_zipfile = new QName("c", XProcConstants.NS_XPROC_STEP, "zipfile");
    protected final static QName c_entry = new QName("c", XProcConstants.NS_XPROC_STEP, "entry");
    protected final static QName c_file = new QName("c", XProcConstants.NS_XPROC_STEP, "file");
    protected final static QName c_directory = new QName("c", XProcConstants.NS_XPROC_STEP, "directory");
    protected final static QName _compressed_size = new QName("", "compressed-size");
    protected final static QName _comment = new QName("", "comment");
    protected final static QName _size = new QName("", "size");
    protected final static QName _date = new QName("", "date");
    private static final QName _status_only = new QName("status-only");
    private static final QName _detailed = new QName("detailed");
    private static final QName _status = new QName("status");
    private static final QName _value = new QName("value");
    private final static int bufsize = 8192;

    private final static QName serializerAttrs[] = {
            _byte_order_mark,
            _cdata_section_elements,
            _doctype_public,
            _doctype_system,
            _encoding,
            _escape_uri_attributes,
            _include_content_type,
            _indent,
            _media_type,
            _method,
            _normalization_form,
            _omit_xml_declaration,
            _standalone,
            _undeclare_prefixes,
            _version
    };

    private ReadablePipe source = null;
    private ReadablePipe manifest = null;
    private WritablePipe result = null;
    private Map<String, FileToZip> zipManifest = new LinkedHashMap<String, FileToZip> ();
    private Map<String, XdmNode> srcManifest = new LinkedHashMap<String, XdmNode> ();

    /** Creates a new instance of Unzip */
    public Zip(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            manifest = pipe;
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        manifest.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        String zipFn = getOption(_href).getString();

        XdmNode man = S9apiUtils.getDocumentElement(manifest.read());

        if (!c_zip_manifest.equals(man.getNodeName())) {
            throw new XProcException(step.getNode(), "The cx:zip manifest must be a c:zip-manifest.");
        }

        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            XdmNode root = S9apiUtils.getDocumentElement(doc);
            srcManifest.put(root.getBaseURI().toASCIIString(), doc);
        }

        parseManifest(man);

        File zipFile = null;
        try {
            zipFile = new File(new URI(zipFn));
        } catch (URISyntaxException e) {
            throw new XProcException(e);
        }

        File zipParent = zipFile.getParentFile();
        File zipTemp = null;
        ZipFile inZip = null;
        ZipOutputStream outZip = null;

        try {
            zipTemp = File.createTempFile("calabashZip",".zip",zipParent);
            zipTemp.deleteOnExit();
            if (zipFile.exists()) {
                inZip = new ZipFile(zipFile);
            }
            outZip = new ZipOutputStream(new FileOutputStream(zipTemp));
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }

        String command = getOption(_command).getString();

        if ("create".equals(command)) {
            try {
                if (inZip != null) {
                    inZip.close();
                }
            } catch (IOException ioe) {
                throw new XProcException(ioe);
            }
            inZip = null;
        }

        if ("update".equals(command) || "create".equals(command)) {
            update(inZip, outZip, false);
        } else if ("freshen".equals(command)) {
            update(inZip, outZip, true);
        } else if ("delete".equals(command)) {
            delete(inZip, outZip);
        } else {
            throw new XProcException(step.getNode(), "Unexpected cx:zip command: " + command);
        }

        if (zipFile.exists()) {
            zipFile.delete();
        }

        zipTemp.renameTo(zipFile);

        TreeWriter tree = new TreeWriter(runtime);

        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_zipfile);
        tree.addAttribute(_href, zipFile.toURI().toASCIIString());
        tree.startContent();

        try {
            URL url = zipFile.toURI().toURL();
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();

            ZipInputStream zipStream = new ZipInputStream(stream);

            DatatypeFactory dfactory = DatatypeFactory.newInstance();
            GregorianCalendar cal = new GregorianCalendar();

            ZipEntry entry = zipStream.getNextEntry();
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
                entry = zipStream.getNextEntry();
            }

            zipStream.close();
        } catch (MalformedURLException mue) {
            throw new XProcException(XProcException.err_E0001, mue);
        } catch (IOException ioe) {
            throw new XProcException(XProcException.err_E0001, ioe);
        } catch (DatatypeConfigurationException dce) {
            throw new XProcException(XProcException.err_E0001, dce);
        }

        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }

    private void parseManifest(XdmNode man) {
        for (XdmNode child : new RelevantNodes(runtime, man, Axis.CHILD)) {
            if (XdmNodeKind.ELEMENT == child.getNodeKind()) {
                if (c_entry.equals(child.getNodeName())) {
                    String name = child.getAttributeValue(_name);
                    if (name == null || "".equals(name)) {
                        throw new XProcException(step.getNode(), "Missing or invalid name in cx:zip manifest.");
                    }
                    String href = child.getAttributeValue(_href);
                    if (href == null || "".equals(href)) {
                        throw new XProcException(step.getNode(), "Missing or invalid href in cx:zip manifest.");
                    }
                    String hrefuri = child.getBaseURI().resolve(href).toASCIIString();
                    String comment = child.getAttributeValue(_comment);

                    int method = ZipEntry.DEFLATED;
                    int level = Deflater.DEFAULT_COMPRESSION;

                    String value = child.getAttributeValue(_compression_method);
                    if ("stored".equals(value)) {
                        method = ZipEntry.STORED;
                    }

                    value = child.getAttributeValue(_compression_level);
                    if ("smallest".equals(value)) {
                        level = Deflater.BEST_COMPRESSION;
                    } else if ("fastest".equals(value)) {
                        level = Deflater.BEST_SPEED;
                    } else if ("huffman".equals(value)) {
                        level = Deflater.HUFFMAN_ONLY;
                    } else if ("none".equals(value)) {
                        level = Deflater.NO_COMPRESSION;
                        method = ZipEntry.STORED;
                    }

                    zipManifest.put(name, new FileToZip(name, hrefuri, method, level, comment, child));
                } else {
                    throw new XProcException(step.getNode(), "Unexpected element in cx:zip manifest: " + child.getNodeName());
                }
            } else {
                    throw new XProcException(step.getNode(), "Unexpected content in cx:zip manifest.");
            }
        }
    }

    public void update(ZipFile inZip, ZipOutputStream outZip, boolean freshen) {
        byte[] buffer = new byte[bufsize];

        try {
            if (inZip != null) {
                Enumeration zenum = inZip.entries();
                while (zenum.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) zenum.nextElement();
                    String name = entry.getName();

                    boolean skip = srcManifest.containsKey(name);

                    if (!skip) {
                        if (zipManifest.containsKey(name) && freshen) {
                            FileToZip file = zipManifest.get(name);
                            long zipDate = entry.getTime();
                            long lastMod = file.getLastModified();

                            skip = (lastMod > zipDate);
                            if (!skip) {
                                zipManifest.remove(name);
                            }
                        } else if (zipManifest.containsKey(name)) {
                            skip = true;
                        }
                    }

                    if (!skip) {
                        outZip.putNextEntry(entry);
                        InputStream stream = inZip.getInputStream(entry);
                        int read = stream.read(buffer, 0, bufsize);
                        while (read >= 0) {
                            outZip.write(buffer,0, read);
                            read = stream.read(buffer, 0, bufsize);
                        }
                        stream.close();
                        outZip.closeEntry();
                    }
                }
            }

            CRC32 crc = new CRC32();
            
            for (String name : zipManifest.keySet()) {
                FileToZip file = zipManifest.get(name);
                ZipEntry ze = new ZipEntry(name);
                if (file.getComment() != null) {
                    ze.setComment(file.getComment());
                }
                ze.setMethod(file.getMethod());
                outZip.setLevel(file.getLevel());
                
                URI uri = zipManifest.get(name).getHref();
                String href = uri.toASCIIString();
                
                if(ze.getMethod() == ZipEntry.STORED) {
                    // FIXME: Using a boas is risky here, it will fail for huge files; but who STOREs a huge file?
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (srcManifest.containsKey(uri.toString())) {
                        XdmNode doc = srcManifest.get(href);
                        Serializer serializer = makeSerializer(file.getOptions());
                        serializer.setOutputStream(baos);
                        S9apiUtils.serialize(runtime, doc, serializer);
                    } else {
                        URLConnection connection = uri.toURL().openConnection();
                        InputStream stream = connection.getInputStream();
                        int read = stream.read(buffer, 0, bufsize);
                        while (read>0){
                            baos.write(buffer,0,read);
                            read = stream.read(buffer, 0, bufsize);
                        }
                        stream.close();
                    }
                    byte[] bytes =  baos.toByteArray();
                    ze.setSize(bytes.length);
                    crc.reset();
                    crc.update(bytes);
                    ze.setCrc(crc.getValue());
                }

                outZip.putNextEntry(ze);

                if (srcManifest.containsKey(href)) {
                    XdmNode doc = srcManifest.get(href);
                    Serializer serializer = makeSerializer(file.getOptions());
                    serializer.setOutputStream(outZip);
                    S9apiUtils.serialize(runtime, doc, serializer);
                } else {
                    URL url = uri.toURL();
                    URLConnection connection = url.openConnection();
                    InputStream stream = connection.getInputStream();
                    int read = stream.read(buffer, 0, bufsize);
                    while (read >= 0) {
                        outZip.write(buffer,0, read);
                        read = stream.read(buffer, 0, bufsize);
                    }
                    stream.close();
                }

                outZip.closeEntry();
            }

            outZip.close();
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
    }

    public void delete(ZipFile inZip, ZipOutputStream outZip) {
        try {
            if (inZip != null) {
                Enumeration zenum = inZip.entries();
                while (zenum.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) zenum.nextElement();
                    String name = entry.getName();
                    boolean delete = false;

                    if (zipManifest.containsKey(name)) {
                        delete = true;
                    }

                    if (!delete) {
                        outZip.putNextEntry(entry);
                        InputStream stream = inZip.getInputStream(entry);
                        byte[] buffer = new byte[bufsize];
                        int read = stream.read(buffer, 0, bufsize);
                        while (read >= 0) {
                            outZip.write(buffer,0, read);
                            read = stream.read(buffer, 0, bufsize);
                        }
                        stream.close();
                        outZip.closeEntry();
                    }
                }
            }

            outZip.close();
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }

    private class FileToZip {
        private String zipName = null;
        private URI href = null;
        private String origHref = null;
        private int method = -1;
        private int level = -1;
        private String comment = null;
        private long lastModified = -1;
        private Hashtable<QName,String> options = null;

        public FileToZip(String zipName, String href, int method, int level, String comment, XdmNode entry) {
            try {
                origHref = href;
                this.zipName = zipName;
                this.href = new URI(href);
                this.method = method;
                this.level = level;
                this.comment = comment;

                lastModified = readLastModified(this.href);

                // FIXME: There's no validation here...
                for (QName attr : serializerAttrs) {
                    String value = entry.getAttributeValue(attr);
                    if (value != null) {
                        if (options == null) {
                            options = new Hashtable<QName, String> ();
                        }
                        options.put(attr, value);
                    }
                }
            } catch (URISyntaxException use) {
                throw new XProcException(use);
            }
        }

        public String getName() {
            return zipName;
        }

        public URI getHref() {
            return href;
        }

        public int getMethod() {
            return method;
        }

        public int getLevel() {
            return level;
        }

        public String getComment() {
            return comment;
        }

        public long getLastModified() {
            return lastModified;
        }

        public Hashtable<QName,String> getOptions() {
            return options;
        }

        private long readLastModified(URI uri) {
            if (srcManifest.containsKey(origHref)) {
                // If the document to be zipped is in the set of source documents,
                // don't try to read its timestamp from the disk or the web.
                // Use "now".
                Date date = new Date();
                return date.getTime();
            }

            if (uri.getScheme().equals("file")) {
                String fn = uri.toASCIIString();
                if (fn.startsWith("file:")) {
                    fn = fn.substring(5);
                    if (fn.startsWith("///")) {
                        fn = fn.substring(2);
                    }
                }

                File f = new File(fn);
                return f.lastModified();
            } else {
                // Let's try HTTP
                HttpRequest httpReq = new HttpRequest(runtime, step);
                Pipe inputPipe = new Pipe(runtime);
                Pipe outputPipe = new Pipe(runtime);
                httpReq.setInput("source", inputPipe);
                httpReq.setOutput("result", outputPipe);

                TreeWriter req = new TreeWriter(runtime);
                req.startDocument(step.getNode().getBaseURI());
                req.addStartElement(XProcConstants.c_request);
                req.addAttribute(_method, "HEAD");
                req.addAttribute(_href, uri.toASCIIString());
                req.addAttribute(_status_only, "true");
                req.addAttribute(_detailed, "true");

                req.startContent();
                req.addEndElement();
                req.endDocument();

                inputPipe.write(req.getResult());

                try {
                    httpReq.run();
                } catch (SaxonApiException sae) {
                    throw new XProcException(sae);
                }

                XdmNode result = S9apiUtils.getDocumentElement(outputPipe.read());
                int status = Integer.parseInt(result.getAttributeValue(_status));

                if (status == 200) {
                    for (XdmNode node : new RelevantNodes(runtime, result, Axis.CHILD)) {
                        if ("Last-Modified".equals(node.getAttributeValue(_name))) {
                            String months[] = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                                               "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
                            String dateStr = node.getAttributeValue(_value);
                            // dateStr = Fri, 13 Mar 2009 12:12:07 GMT
                            //           00000000001111111111222222222
                            //           01234567890123456789012345678

                            //System.err.println("dateStr: " + dateStr);

                            int day = Integer.parseInt(dateStr.substring(5,7));
                            String monthStr = dateStr.substring(8,11).toUpperCase();
                            int year = Integer.parseInt(dateStr.substring(12,16));
                            int hour = Integer.parseInt(dateStr.substring(17,19));
                            int min = Integer.parseInt(dateStr.substring(20,22));
                            int sec = Integer.parseInt(dateStr.substring(23,25));
                            String tzStr = dateStr.substring(26,29);

                            int month = 0;
                            for (month = 0; month < 12; month++) {
                                if (months[month].equals(monthStr)) {
                                    break;
                                }
                            }

                            // FIXME: check if this is correct!
                            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                            cal.set(year,month,day,hour,min,sec);
                            return cal.getTimeInMillis();
                        }
                    }
                    return -1;
                } else {
                    return -1;
                }
            }
        }
    }

    public Serializer makeSerializer(Hashtable<QName,String> options) {
        Serializer serializer = new Serializer();

        if (options == null) {
            return serializer;
        }

        if (options.containsKey(_byte_order_mark)) {
            serializer.setOutputProperty(Serializer.Property.BYTE_ORDER_MARK, "false".equals(options.get(_byte_order_mark)) ? "yes" : "no");
        }

        if (options.containsKey(_cdata_section_elements)) {
            String list = options.get(_cdata_section_elements);

            // FIXME: Why is list="" sometimes?
            if (!"".equals(list)) {
                String[] names = list.split("\\s+");
                list = "";
                for (String name : names) {
                    QName q = new QName(name, step.getNode());
                    list += q.getClarkName() + " ";
                }

                serializer.setOutputProperty(Serializer.Property.CDATA_SECTION_ELEMENTS, list);
            }
        }

        if (options.containsKey(_doctype_public)) {
            serializer.setOutputProperty(Serializer.Property.DOCTYPE_PUBLIC, options.get(_doctype_public));
        }

        if (options.containsKey(_doctype_system)) {
            serializer.setOutputProperty(Serializer.Property.DOCTYPE_SYSTEM, options.get(_doctype_system));
        }

        if (options.containsKey(_encoding)) {
            serializer.setOutputProperty(Serializer.Property.ENCODING, options.get(_encoding));
        }

        if (options.containsKey(_escape_uri_attributes)) {
            serializer.setOutputProperty(Serializer.Property.ESCAPE_URI_ATTRIBUTES, "true".equals(options.get(_escape_uri_attributes)) ? "yes" : "no");
        }

        if (options.containsKey(_include_content_type)) {
            serializer.setOutputProperty(Serializer.Property.INCLUDE_CONTENT_TYPE, "true".equals(options.get(_include_content_type)) ? "yes" : "no");
        }

        if (options.containsKey(_indent)) {
            serializer.setOutputProperty(Serializer.Property.INDENT, "true".equals(options.get(_indent)) ? "yes" : "no");
        }

        if (options.containsKey(_media_type)) {
            serializer.setOutputProperty(Serializer.Property.MEDIA_TYPE, options.get(_media_type));
        }

        if (options.containsKey(_method)) {
            serializer.setOutputProperty(Serializer.Property.METHOD, options.get(_method));
        }

        if (options.containsKey(_normalization_form)) {
            serializer.setOutputProperty(Serializer.Property.NORMALIZATION_FORM, options.get(_normalization_form));
        }

        if (options.containsKey(_omit_xml_declaration)) {
            serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "true".equals(options.get(_omit_xml_declaration)) ? "yes" : "no");
        }

        if (options.containsKey(_standalone)) {
            String standalone = options.get(_standalone);
            if ("true".equals(standalone)) {
                serializer.setOutputProperty(Serializer.Property.STANDALONE, "yes");
            } else if ("false".equals(standalone)) {
                serializer.setOutputProperty(Serializer.Property.STANDALONE, "no");
            }
            // What about omit?
        }

        if (options.containsKey(_undeclare_prefixes)) {
            serializer.setOutputProperty(Serializer.Property.UNDECLARE_PREFIXES, "true".equals(options.get(_undeclare_prefixes)) ? "yes" : "no");
        }

        if (options.containsKey(_version)) {
            serializer.setOutputProperty(Serializer.Property.VERSION, options.get(_version));
        }

        return serializer;
    }
}