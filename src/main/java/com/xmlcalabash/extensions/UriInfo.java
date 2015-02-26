package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.library.HttpRequest;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.AxisNodes;
import com.xmlcalabash.util.MessageFormatter;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Axis;

import java.net.URI;
import java.io.File;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Mar 12, 2009
 * Time: 9:41:54 PM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "cx:uri-info",
        type = "{http://xmlcalabash.com/ns/extensions}uri-info")

public class UriInfo extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _method = new QName("method");
    private static final QName _status_only = new QName("status-only");
    private static final QName _detailed = new QName("detailed");
    private static final QName _status = new QName("status");
    private static final QName _name = new QName("name");
    private static final QName _value = new QName("value");
    private static final QName _username = new QName("username");
    private static final QName _password = new QName("password");
    private static final QName _auth_method = new QName("auth_method");
    private static final QName _send_authorization = new QName("send_authorization");
    protected final static QName c_uriinfo = new QName("c", XProcConstants.NS_XPROC_STEP, "uri-info");

    private static final QName _uri = new QName("uri");
    private static final QName _readable = new QName("readable");
    private static final QName _writable = new QName("writable");
    private static final QName _exists = new QName("exists");
    private static final QName _absolute = new QName("absolute");
    private static final QName _directory = new QName("directory");
    private static final QName _hidden = new QName("hidden");
    private static final QName _file = new QName("file");
    private static final QName _last_modified = new QName("last-modified");
    private static final QName _size = new QName("size");
    private static final QName _absolute_path = new QName("absolute-path");
    private static final QName _canonical_path = new QName("canonical-path");

    private WritablePipe result = null;

    /**
     * Creates a new instance of UriInfo
     */
    public UriInfo(XProcRuntime runtime, XAtomicStep step) {
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

        RuntimeValue href = getOption(_href);
        URI uri = href.getBaseURI().resolve(href.getString());

        logger.trace(MessageFormatter.nodeMessage(step.getNode(), "Checking uri-info for " + uri));

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_uriinfo);

        if (uri.getScheme().equals("file")) {
            String fn = href.getString();
            if (fn.startsWith("file:")) {
                fn = fn.substring(5);
                if (fn.startsWith("///")) {
                    fn = fn.substring(2);
                }
            }

            File f = new File(fn);

            tree.addAttribute(_href, href.getString());
            tree.addAttribute(_exists, f.exists() ? "true" : "false");
            tree.addAttribute(_readable, f.canRead() ? "true" : "false");

            if (f.exists()) {
                tree.addAttribute(_writable, f.canWrite() ? "true" : "false");
                tree.addAttribute(_size, "" + f.length());
                tree.addAttribute(_absolute, f.isAbsolute() ? "true" : "false");
                tree.addAttribute(_directory, f.isDirectory() ? "true" : "false");
                tree.addAttribute(_hidden, f.isHidden() ? "true" : "false");
                tree.addAttribute(_file, f.isFile() ? "true" : "false");

                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(f.lastModified());

                TimeZone tz = TimeZone.getDefault();
                long gmt = f.lastModified() - tz.getRawOffset();
                if (tz.useDaylightTime() && tz.inDaylightTime(cal.getTime())) {
                    gmt -= tz.getDSTSavings();
                }
                cal.setTimeInMillis(gmt);
                tree.addAttribute(_last_modified, String.format("%1$04d-%2$02d-%3$02dT%4$02d:%5$02d:%6$02dZ",
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND)));
            } else {
                String path = f.getAbsolutePath();
                int slash = path.lastIndexOf("/");
                path = path.substring(0,slash);
                File parent = new File(path);
                tree.addAttribute(_writable, parent.canWrite() ? "true" : "false");
            }

            tree.addAttribute(_absolute_path, f.getAbsolutePath());
            tree.addAttribute(_uri, f.toURI().toASCIIString());

            try {
                String cp = f.getCanonicalPath();
                tree.addAttribute(_canonical_path, cp);
            } catch (Exception e) {
                // nevermind
            }
            tree.startContent();
            tree.addEndElement();
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

            for (QName name : new QName[] {_username, _password, _auth_method, _send_authorization } ) {
                RuntimeValue v = getOption(name);
                if (v != null) { req.addAttribute(name, v.getString()); }
            }
            
            req.startContent();
            req.addEndElement();
            req.endDocument();

            inputPipe.write(req.getResult());

            httpReq.run();

            XdmNode result = S9apiUtils.getDocumentElement(outputPipe.read());
            int status = Integer.parseInt(result.getAttributeValue(_status));
            
            tree.addAttribute(_href, href.getString());
            tree.addAttribute(_status, ""+status);
            tree.addAttribute(_readable, status >= 200 && status < 400 ? "true" : "false");
            tree.addAttribute(_exists, status >= 400 && status < 500 ? "false" : "true");
            tree.addAttribute(_uri, uri.toASCIIString());

            for (XdmNode node : new AxisNodes(result, Axis.CHILD, AxisNodes.SIGNIFICANT)) {
                if ("Last-Modified".equals(node.getAttributeValue(_name))) {
                    String months[] = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                                       "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
                    String dateStr = node.getAttributeValue(_value);
                    // dateStr = Fri, 13 Mar 2009 12:12:07 GMT
                    //           00000000001111111111222222222
                    //           01234567890123456789012345678

                    //System.err.println("dateStr: " + dateStr);
                    
                    String dayStr = dateStr.substring(5,7);
                    String monthStr = dateStr.substring(8,11).toUpperCase();
                    String yearStr = dateStr.substring(12,16);
                    String timeStr = dateStr.substring(17,25);
                    String tzStr = dateStr.substring(26,29);

                    int month = 0;
                    for (month = 0; month < 12; month++) {
                        if (months[month].equals(monthStr)) {
                            break;
                        }
                    }

                    tree.addAttribute(_last_modified, String.format("%1$04d-%2$02d-%3$02dT%4$s%5$s",
                            Integer.parseInt(yearStr), month+1, Integer.parseInt(dayStr), timeStr,
                            "GMT".equals(tzStr) ? "Z" : ""));
                }

                if ("Content-Length".equals(node.getAttributeValue(_name))) {
                    tree.addAttribute(_size, node.getAttributeValue(_value));
                }
            }


            tree.startContent();

            for (XdmNode node : new AxisNodes(result, Axis.CHILD, AxisNodes.SIGNIFICANT)) {
                tree.addSubtree(node);
            }

            tree.addEndElement();
        }

        tree.endDocument();

        result.write(tree.getResult());
    }
}

