package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 12/18/10
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMLtoJSON {
    private static QName _type = new QName("", "type");
    private static QName _name = new QName("", "name");
    private static final QName c_body = new QName("c", XProcConstants.NS_XPROC_STEP, "body");
    private static final QName c_pair = new QName("c", XProcConstants.NS_XPROC_STEP, "pair");

    public static String convert(XdmNode json) {
        JSONStringer js = new JSONStringer();

        json = S9apiUtils.getDocumentElement(json);

        try {
            if (c_body.equals(json.getNodeName())) {
                processChildren(json, js);
            } else {
                build(json, js);
            }
        } catch (JSONException jse) {
            throw new XProcException(jse);
        }

        return js.toString();
    }

    private static void build(XdmNode json, JSONStringer js) throws JSONException {
        String type = json.getAttributeValue(_type);

        if ("object".equals(type)) {
            if (c_pair.equals(json.getNodeName())) {
                js.key(json.getAttributeValue(_name));
            }
            js.object();
            processChildren(json, js);
            js.endObject();
        } else if ("array".equals(type)) {
            if (c_pair.equals(json.getNodeName())) {
                js.key(json.getAttributeValue(_name));
            }
            js.array();
            processChildren(json, js);
            js.endArray();
        } else {
            if (json.getAttributeValue(_name) != null) {
                js.key(json.getAttributeValue(_name));
            }

            if ("null".equals(type)) {
                js.value(null);
            } else if ("number".equals(type)) {
                String value = json.getStringValue();
                if (value.contains(".")) {
                    Double d = Double.parseDouble(value);
                    js.value(d);
                } else {
                    long i = Long.parseLong(value);
                    js.value(i);
                }
            } else if ("boolean".equals(type)) {
                js.value("true".equals(json.getStringValue()));
            } else {
                js.value(json.getStringValue());
            }
        }
    }

    private static void processChildren(XdmNode json, JSONStringer js) throws JSONException {
        XdmSequenceIterator iter = json.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmItem item = iter.next();
            if (item instanceof XdmNode) {
                XdmNode child = (XdmNode) item;
                if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                    build(child, js);
                }
            }
        }
    }
}
