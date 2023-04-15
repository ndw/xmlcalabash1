package com.xmlcalabash.extensions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.util.TypeUtils;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import org.apache.http.cookie.Cookie;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
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
        name = "cx:get-cookies",
        type = "{http://xmlcalabash.com/ns/extensions}get-cookies")

public class GetCookies extends DefaultStep {
    private static final QName _cookies = new QName("","cookies");
    private static final QName _domain = new QName("","domain");
    private static final QName _name = new QName("","name");
    private static final QName _value = new QName("","value");
    private static final QName _path = new QName("","path");
    private static final QName _expires = new QName("","expires");
    private static final QName _version = new QName("", "version");
    private static final QName _secure = new QName("","secure");
    private static final QName c_cookies = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "cookies");
    private static final QName c_cookie = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "cookie");
    private static final DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private WritablePipe result = null;

    /*
     * Creates a new instance of Identity
     */
    public GetCookies(XProcRuntime runtime, XAtomicStep step) {
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

        String cookieKey = getOption(_cookies).getString();

        TreeWriter tree = new TreeWriter(runtime);

        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_cookies);

        for (Cookie cookie : runtime.getCookieStore(cookieKey).getCookies()) {
            AttributeMap amap = EmptyAttributeMap.getInstance();
            amap = amap.put(TypeUtils.attributeInfo(_name, cookie.getName()));
            amap = amap.put(TypeUtils.attributeInfo(_value, cookie.getValue()));
            amap = amap.put(TypeUtils.attributeInfo(_domain, cookie.getDomain()));
            amap = amap.put(TypeUtils.attributeInfo(_path, cookie.getPath()));
            //amap = amap.put(TypeUtils.attributeInfo(_secure, cookie.getSecure() ? "true" : "false"));
            //amap = amap.put(TypeUtils.attributeInfo(_version, ""+cookie.getVersion()));
            Date date = cookie.getExpiryDate();
            if (date != null) {
                String iso = iso8601.format(date);
                // Insert the damn colon in the timezone
                iso = iso.substring(0,22) + ":" + iso.substring(22);
                amap = amap.put(TypeUtils.attributeInfo(_expires, iso));
            }
            tree.addStartElement(c_cookie, amap);
            String comment = cookie.getComment();
            if (comment != null) {
                tree.addText(comment);
            }
            tree.addEndElement();
        }

        tree.addEndElement();
        tree.endDocument();

        result.write(tree.getResult());
    }
}