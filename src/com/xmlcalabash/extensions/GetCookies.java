package com.xmlcalabash.extensions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

public class GetCookies extends DefaultStep {
    private static final QName _cookies = new QName("","cookies");
    private static final QName _domain = new QName("","domain");
    private static final QName _name = new QName("","name");
    private static final QName _value = new QName("","value");
    private static final QName _path = new QName("","path");
    private static final QName _expires = new QName("","expires");
    private static final QName _version = new QName("", "version");
    private static final QName _secure = new QName("","secure");
    private static final QName c_cookies = new QName("c", XProcConstants.NS_XPROC_STEP, "cookies");
    private static final QName c_cookie = new QName("c", XProcConstants.NS_XPROC_STEP, "cookie");
    private static DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private WritablePipe result = null;

    /**
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
        tree.startContent();

        for (Cookie cookie : runtime.getCookieStore(cookieKey).getCookies()) {
            tree.addStartElement(c_cookie);
            tree.addAttribute(_name, cookie.getName());
            tree.addAttribute(_value, cookie.getValue());
            tree.addAttribute(_domain, cookie.getDomain());
            tree.addAttribute(_path, cookie.getPath());
            //tree.addAttribute(_secure, cookie.getSecure() ? "true" : "false");
            //tree.addAttribute(_version, ""+cookie.getVersion());
            Date date = cookie.getExpiryDate();
            if (date != null) {
                String iso = iso8601.format(date);
                // Insert the damn colon in the timezone
                iso = iso.substring(0,22) + ":" + iso.substring(22);
                tree.addAttribute(_expires, iso);

                Date today = new Date();
                
            }
            tree.startContent();
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