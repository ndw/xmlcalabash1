package com.xmlcalabash.extensions;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;

import org.apache.http.cookie.SetCookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.util.S9apiUtils;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class SetCookies extends DefaultStep {
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
    private static DateFormat iso8601tz = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private ReadablePipe source = null;

    /**
     * Creates a new instance of Identity
     */
    public SetCookies(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void reset() {
        source.resetReader();
    }

    public void run() throws SaxonApiException {
        super.run();

        String cookieKey = getOption(_cookies).getString();

        XdmNode doc = source.read();
        XdmNode root = S9apiUtils.getDocumentElement(doc);
        if (!c_cookies.equals(root.getNodeName())) {
            throw new XProcException(step.getNode(), "The input to cx:set-cookies must be a c:cookies document.");
        }
        
        for (XdmNode node : new RelevantNodes(null, root, Axis.CHILD)) {
            if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                if (!c_cookie.equals(node.getNodeName())) {
                    throw new XProcException(step.getNode(), "A c:cookies document must contain only c:cookie elements.");
                }

                String domain = node.getAttributeValue(_domain);
                String name = node.getAttributeValue(_name);
                String value = node.getAttributeValue(_value);
                String path = node.getAttributeValue(_path);
                String expires = node.getAttributeValue(_expires);

                if (name == null || value == null) {
                    throw new XProcException(step.getNode(), "Invalid cookie: " + node);
                }

                SetCookie cookie = new BasicClientCookie(name, value);

                if (domain != null) { cookie.setDomain(domain); }
                if (path != null) { cookie.setPath(path); }

                if (expires != null) {
                    Date date = null;
                    try {
                        if (expires.length() > 21) {
                            // expires = yyyy-MM-dd'T'HH:mm:ss+00:00"
                            expires = expires.substring(0,22) + expires.substring(23);
                            date = iso8601tz.parse(expires);
                        } else {
                            // expires = yyyy-MM-dd'T'HH:mm:ss"
                            date = iso8601.parse(expires);
                        }

                        cookie.setExpiryDate(date);
                    } catch (ParseException pe) {
                        throw new XProcException(pe);
                    }
                }

                runtime.getCookieStore(cookieKey).addCookie(cookie);

            } else if (node.getNodeKind() == XdmNodeKind.TEXT) {
                if ("".equals(node.getStringValue().trim())) {
                    // nop
                } else {
                    throw new XProcException(step.getNode(), "A c:cookies document must not contain non-whitespace text nodes.");
                }
            }
        }
    }
}