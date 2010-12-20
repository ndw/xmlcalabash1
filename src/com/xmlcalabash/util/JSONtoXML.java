package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcException;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 12/18/10
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONtoXML {
    private static QName _json = new QName("", "json");
    private static QName _type = new QName("", "type");
    private static QName _pair = new QName("", "pair");
    private static QName _item = new QName("", "item");
    private static QName _name = new QName("", "name");

    public static XdmNode convert(Processor processor, JSONTokener jt) {
        TreeWriter tree = new TreeWriter(processor);
        tree.startDocument(null);
        build(tree, jt);
        tree.endDocument();
        return tree.getResult();
    }

    private static void build(TreeWriter tree, JSONTokener jt) {
        tree.addStartElement(_json);

        try {
            char ch = jt.next();
            jt.back();

            if (ch == '{') {
                tree.addAttribute(_type, "object");
                tree.startContent();
                buildPairs(tree, new JSONObject(jt));
            } else {
                tree.addAttribute(_type, "array");
                tree.startContent();
                buildArray(tree, new JSONArray(jt));
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }

        tree.addEndElement();
    }

    private static void buildPairs(TreeWriter tree, JSONObject jo) {
        try {
            Iterator keys = jo.keys();
            while (keys.hasNext()) {
                String name = (String) keys.next();
                Object json = jo.get(name);
                tree.addStartElement(_pair);
                tree.addAttribute(_name, name);

                if (json instanceof JSONObject) {
                    tree.addAttribute(_type, "object");
                    tree.startContent();
                    buildPairs(tree, (JSONObject) json);
                } else if (json instanceof JSONArray) {
                    tree.addAttribute(_type, "array");
                    tree.startContent();
                    buildArray(tree, (JSONArray) json);
                } else if (json instanceof Integer) {
                    tree.addAttribute(_type, "number");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json instanceof Double) {
                    tree.addAttribute(_type, "number");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json instanceof Long) {
                    tree.addAttribute(_type, "number");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json instanceof String) {
                    tree.addAttribute(_type, "string");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json instanceof Boolean) {
                    tree.addAttribute(_type, "boolean");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json == JSONObject.NULL) {
                    tree.addAttribute(_type, "null");
                    tree.startContent();
                } else {
                    throw new XProcException("Unexpected type in JSON conversion.");
                }

                tree.addEndElement();
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }
    }

    private static void buildArray(TreeWriter tree, JSONArray arr) {
        try {
            for (int pos = 0; pos < arr.length(); pos++) {
                Object json = arr.get(pos);

                tree.addStartElement(_item);

                if (json instanceof JSONObject) {
                    tree.addAttribute(_type, "object");
                    tree.startContent();
                    buildPairs(tree, (JSONObject) json);
                } else if (json instanceof JSONArray) {
                    tree.addAttribute(_type, "array");
                    tree.startContent();
                    buildArray(tree, (JSONArray) json);
                } else if (json instanceof Integer) {
                    tree.addAttribute(_type, "number");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json instanceof Double) {
                    tree.addAttribute(_type, "number");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json instanceof Long) {
                    tree.addAttribute(_type, "number");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json instanceof String) {
                    tree.addAttribute(_type, "string");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json instanceof Boolean) {
                    tree.addAttribute(_type, "boolean");
                    tree.startContent();
                    tree.addText(json.toString());
                } else if (json == JSONObject.NULL) {
                    tree.addAttribute(_type, "null");
                    tree.startContent();
                } else {
                    throw new XProcException("Unexpected type in JSON conversion.");
                }

                tree.addEndElement();
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }
    }
}
