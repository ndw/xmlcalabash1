package com.xmlcalabash.extensions.fileutils;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.library.HttpRequest;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.*;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Axis;

import java.io.File;
import java.net.URI;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: May 24, 2009
 * Time: 3:17:23 PM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "pxf:info",
        type = "{http://exproc.org/proposed/steps/file}info " +
                "{http://xmlcalabash.com/ns/extensions/fileutils}info")

public class Info extends DefaultStep {
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
    private static final QName _send_authorization = new QName("send-authorization");
    private static final QName _fail_on_error = new QName("fail-on-error");
    protected final static QName c_uri = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "uri");
    protected final static QName c_directory = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "directory");
    protected final static QName c_file = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "file");
    protected final static QName c_other = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "other");
    protected final static QName c_error = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "error");
    protected final static QName err_fu01 = XProcConstants.qNameFor(XProcConstants.NS_XPROC_ERROR, "FU01");

    private static final QName _uri = new QName("uri");
    private static final QName _readable = new QName("readable");
    private static final QName _writable = new QName("writable");
    private static final QName _exists = new QName("exists");
    private static final QName _hidden = new QName("hidden");
    private static final QName _last_modified = new QName("last-modified");
    private static final QName _size = new QName("size");

    private WritablePipe result = null;

    /*
     * Creates a new instance of UriInfo
     */
    public Info(XProcRuntime runtime, XAtomicStep step) {
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

        boolean failOnError = getOption(_fail_on_error, true);

        logger.trace(MessageFormatter.nodeMessage(step.getNode(), "Checking info for " + uri));

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());

        if ("file".equals(uri.getScheme())) {
            File file = URIUtils.toFile(uri);

            if (!file.exists()) {
                if (failOnError) {
                    throw new XProcException(err_fu01);
                } else {
                    tree.addStartElement(c_error);
                    tree.addText("File not found");
                    tree.addEndElement();
                    tree.endDocument();
                    result.write(tree.getResult());
                    return;
                }
            }

            AttributeMap amap = EmptyAttributeMap.getInstance();
            amap = amap = amap.put(TypeUtils.attributeInfo(_href, uri.toASCIIString()));

            if (file.canRead())  { amap = amap.put(TypeUtils.attributeInfo(_readable, "true")); }
            if (file.canWrite()) { amap = amap.put(TypeUtils.attributeInfo(_writable, "true")); }
            if (file.isHidden()) { amap = amap.put(TypeUtils.attributeInfo(_hidden, "true")); }
            amap = amap.put(TypeUtils.attributeInfo(_size, "" + file.length()));

            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(file.lastModified());

            TimeZone tz = TimeZone.getDefault();
            long gmt = file.lastModified() - tz.getRawOffset();
            if (tz.useDaylightTime() && tz.inDaylightTime(cal.getTime())) {
                gmt -= tz.getDSTSavings();
            }
            cal.setTimeInMillis(gmt);
            amap = amap.put(TypeUtils.attributeInfo(_last_modified, String.format("%1$04d-%2$02d-%3$02dT%4$02d:%5$02d:%6$02dZ",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));

            if (file.isDirectory()) {
                tree.addStartElement(c_directory, amap);
            } else if (file.isFile()) {
                tree.addStartElement(c_file, amap);
            } else {
                tree.addStartElement(c_other, amap);
            }

            tree.addEndElement();
        } else {
            // Let's try HTTP
            HttpRequest httpReq = new HttpRequest(runtime, step);
            Pipe inputPipe = new Pipe(runtime);
            Pipe outputPipe = new Pipe(runtime);
            httpReq.setInput("source", inputPipe);
            httpReq.setOutput("result", outputPipe);

            TreeWriter req = new TreeWriter(runtime);
            AttributeMap amap = EmptyAttributeMap.getInstance();

            req.startDocument(step.getNode().getBaseURI());
            amap = amap.put(TypeUtils.attributeInfo(_method, "HEAD"));
            amap = amap.put(TypeUtils.attributeInfo(_href, uri.toASCIIString()));
            amap = amap.put(TypeUtils.attributeInfo(_status_only, "true"));
            amap = amap.put(TypeUtils.attributeInfo(_detailed, "true"));

            for (QName name : new QName[] {_username, _password, _auth_method, _send_authorization } ) {
                RuntimeValue v = getOption(name);
                if (v != null) { amap = amap.put(TypeUtils.attributeInfo(name, v.getString())); }
            }

            req.addStartElement(XProcConstants.c_request, amap);
            req.addEndElement();
            req.endDocument();

            inputPipe.write(req.getResult());

            httpReq.run();

            XdmNode result = S9apiUtils.getDocumentElement(outputPipe.read());
            assert result != null;

            int status = Integer.parseInt(result.getAttributeValue(_status));

            amap = EmptyAttributeMap.getInstance();
            amap = amap.put(TypeUtils.attributeInfo(_href, href.getString()));
            amap = amap.put(TypeUtils.attributeInfo(_status, ""+status));
            amap = amap.put(TypeUtils.attributeInfo(_readable, status >= 200 && status < 400 ? "true" : "false"));
            amap = amap.put(TypeUtils.attributeInfo(_exists, status >= 400 && status < 500 ? "false" : "true"));
            amap = amap.put(TypeUtils.attributeInfo(_uri, uri.toASCIIString()));

            for (XdmNode node : new AxisNodes(result, Axis.CHILD, AxisNodes.SIGNIFICANT)) {
                if ("Last-Modified".equals(node.getAttributeValue(_name))) {
                    String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
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
                        if (monthStr.equals(months[month])) {
                            break;
                        }
                    }

                    amap = amap.put(TypeUtils.attributeInfo(_last_modified, String.format("%1$04d-%2$02d-%3$02dT%4$s%5$s",
                            Integer.parseInt(yearStr), month+1, Integer.parseInt(dayStr), timeStr,
                            "GMT".equals(tzStr) ? "Z" : "")));
                }

                if ("Content-Length".equals(node.getAttributeValue(_name))) {
                    amap = amap.put(TypeUtils.attributeInfo(_size, node.getAttributeValue(_value)));
                }
            }

            tree.addStartElement(c_uri, amap);

            for (XdmNode node : new AxisNodes(result, Axis.CHILD, AxisNodes.SIGNIFICANT)) {
                tree.addSubtree(node);
            }

            tree.addEndElement();
        }

        tree.endDocument();

        result.write(tree.getResult());
    }
}

