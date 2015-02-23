package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.serialize.charcode.XMLCharacterData;
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
    public static final String CALABASH_DEPRECATED = "calabash-deprecated";
    public static final String CALABASH            = "calabash";
    public static final String JSONX               = "jsonx";
    public static final String JXML                = "jxml";
    public static final String MARKLOGIC           = "marklogic";

    public static final String JSONX_NS = "http://www.ibm.com/xmlns/prod/2009/jsonx";
    public static final String MLJS_NS  = "http://marklogic.com/json";
    public static final String JXML_NS  = "http://www.xmlsh.org/jxml";

    private static final QName _type = new QName("", "type");
    private static final QName _name = new QName("", "name");

    private static final QName c_json = new QName("c", XProcConstants.NS_XPROC_STEP, "json");
    private static final QName c_pair = new QName("c", XProcConstants.NS_XPROC_STEP, "pair");
    private static final QName c_item = new QName("c", XProcConstants.NS_XPROC_STEP, "item");

    private static final QName _json = new QName("json");
    private static final QName _pair = new QName("pair");
    private static final QName _item = new QName("item");

    private static final QName j_object = new QName("j", JSONX_NS, "object");
    private static final QName j_array = new QName("j", JSONX_NS, "array");
    private static final QName j_string = new QName("j", JSONX_NS, "string");
    private static final QName j_number = new QName("j", JSONX_NS, "number");
    private static final QName j_boolean = new QName("j", JSONX_NS, "boolean");
    private static final QName j_null = new QName("j", JSONX_NS, "null");

    private static final QName mj_json = new QName("j", MLJS_NS, "json");

    private static final QName jx_object = new QName("j", JXML_NS, "object");
    private static final QName jx_member = new QName("j", JXML_NS, "member");
    private static final QName jx_boolean = new QName("j", JXML_NS, "boolean");
    private static final QName jx_array = new QName("j", JXML_NS, "array");
    private static final QName jx_string = new QName("j", JXML_NS, "string");
    private static final QName jx_number = new QName("j", JXML_NS, "number");
    private static final QName jx_null = new QName("j", JXML_NS, "null");

    public static boolean knownFlavor(String jsonFlavor) {
        return (JSONtoXML.CALABASH_DEPRECATED.equals(jsonFlavor)
                || JSONtoXML.CALABASH.equals(jsonFlavor)
                || JSONtoXML.JSONX.equals(jsonFlavor)
                || JSONtoXML.JXML.equals(jsonFlavor)
                || JSONtoXML.MARKLOGIC.equals(jsonFlavor));
    }

    public static XdmNode convert(Processor processor, JSONTokener jt, String flavor) {
        TreeWriter tree = new TreeWriter(processor);
        tree.startDocument(null);

        if (JSONX.equals(flavor)) {
            buildJsonX(tree, jt);
        } else if (MARKLOGIC.equals(flavor)) {
            buildMarkLogic(tree, jt);
        } else if (JXML.equals(flavor)) {
            buildJxml(tree, jt);
        } else if (CALABASH.equals(flavor)) {
            buildMine(tree, jt, true);
        } else {
            buildMine(tree, jt, false);
        }

        tree.endDocument();
        return tree.getResult();
    }

    private static void buildJsonX(TreeWriter tree, JSONTokener jt) {
        try {
            char ch = jt.next();
            jt.back();

            if (ch == '{') {
                tree.addStartElement(j_object);
                tree.startContent();
                buildJsonXPairs(tree, new JSONObject(jt));
            } else {
                tree.addStartElement(j_array);
                buildJsonXArray(tree, new JSONArray(jt));
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }

        tree.addEndElement();
    }

    private static void buildJsonXPairs(TreeWriter tree, JSONObject jo) {
        try {
            Iterator<String> keys = jo.keys();
            while (keys.hasNext()) {
                String name = keys.next();
                Object json = jo.get(name);
                serializeJsonX(tree, json, name);
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }
    }

    private static void buildJsonXArray(TreeWriter tree, JSONArray arr) {
        try {
            for (int pos = 0; pos < arr.length(); pos++) {
                Object json = arr.get(pos);
                serializeJsonX(tree, json, null);
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }
    }

    private static void serializeJsonX(TreeWriter tree, Object json, String name) {
        if (json instanceof JSONObject) {
            tree.addStartElement(j_object);
            if (name != null) { tree.addAttribute(_name, name); }
            tree.startContent();
            buildJsonXPairs(tree, (JSONObject) json);
            tree.addEndElement();
        } else if (json instanceof JSONArray) {
            tree.addStartElement(j_array);
            if (name != null) { tree.addAttribute(_name, name); }
            tree.startContent();
            buildJsonXArray(tree, (JSONArray) json);
            tree.addEndElement();
        } else if (json instanceof Integer || json instanceof Double || json instanceof Long) {
            tree.addStartElement(j_number);
            if (name != null) { tree.addAttribute(_name, name); }
            tree.startContent();
            tree.addText(json.toString());
            tree.addEndElement();
        } else if (json instanceof String) {
            tree.addStartElement(j_string);
            if (name != null) { tree.addAttribute(_name, name); }
            tree.startContent();
            tree.addText(json.toString());
            tree.addEndElement();
        } else if (json instanceof Boolean) {
            tree.addStartElement(j_boolean);
            if (name != null) { tree.addAttribute(_name, name); }
            tree.startContent();
            tree.addText(json.toString());
            tree.addEndElement();
        } else if (json == JSONObject.NULL) {
            tree.addStartElement(j_null);
            if (name != null) { tree.addAttribute(_name, name); }
            tree.startContent();
            tree.addEndElement();
        } else {
            throw new XProcException("Unexpected type in JSON conversion.");
        }
    }

    private static void buildMarkLogic(TreeWriter tree, JSONTokener jt) {
        tree.addStartElement(mj_json);

        try {
            char ch = jt.next();
            jt.back();

            if (ch == '{') {
                tree.addAttribute(_type, "object");
                tree.startContent();
                buildMarkLogicPairs(tree, new JSONObject(jt));
            } else {
                tree.addAttribute(_type, "object");
                buildMarkLogicArray(tree, new JSONArray(jt));
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }

        tree.addEndElement();
    }

    private static void buildMarkLogicPairs(TreeWriter tree, JSONObject jo) {
        try {
            Iterator<String> keys = jo.keys();
            while (keys.hasNext()) {
                String name = keys.next();
                Object json = jo.get(name);
                serializeMarkLogic(tree, json, name);
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }
    }

    private static void buildMarkLogicArray(TreeWriter tree, JSONArray arr) {
        try {
            for (int pos = 0; pos < arr.length(); pos++) {
                Object json = arr.get(pos);
                serializeMarkLogic(tree, json, null);
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }
    }

    private static void serializeMarkLogic(TreeWriter tree, Object json, String name) {
        String localName = "item";
        if (name != null) {
            if ("".equals(name)) {
                localName = "_";
            } else {
                localName = "";
                for (int pos = 0; pos < name.length(); pos++) {
                    int ch = name.charAt(pos);
                    if ('_' != ch
                        && ((pos == 0 && XMLCharacterData.isNCNameStart10(ch))
                            || (pos > 0 && XMLCharacterData.isNCName10(ch)))) {
                        localName += Character.toString((char) ch);
                    } else {
                        localName += String.format("_%04x", ch);
                    }
                }
            }
        }

        QName elemName = new QName("j", MLJS_NS, localName);
        tree.addStartElement(elemName);

        if (json instanceof JSONObject) {
            tree.addAttribute(_type, "object");
            tree.startContent();
            buildMarkLogicPairs(tree, (JSONObject) json);
        } else if (json instanceof JSONArray) {
            tree.addAttribute(_type, "array");
            tree.startContent();
            buildMarkLogicArray(tree, (JSONArray) json);
        } else if (json instanceof Integer || json instanceof Double || json instanceof Long) {
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

    private static void buildJxml(TreeWriter tree, JSONTokener jt) {
        try {
            char ch = jt.next();
            jt.back();

            if (ch == '{') {
                tree.addStartElement(jx_object);
                tree.startContent();
                buildJxmlPairs(tree, new JSONObject(jt));
            } else {
                tree.addStartElement(jx_array);
                buildJxmlArray(tree, new JSONArray(jt));
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }

        tree.addEndElement();
    }

    private static void buildJxmlPairs(TreeWriter tree, JSONObject jo) {
        try {
            Iterator<String> keys = jo.keys();
            while (keys.hasNext()) {
                String name = keys.next();
                Object json = jo.get(name);
                serializeJxml(tree, json, name);
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }
    }

    private static void buildJxmlArray(TreeWriter tree, JSONArray arr) {
        try {
            for (int pos = 0; pos < arr.length(); pos++) {
                Object json = arr.get(pos);
                serializeJxml(tree, json, null);
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }
    }

    private static void serializeJxml(TreeWriter tree, Object json, String name) {
        if (name != null) {
            tree.addStartElement(jx_member);
            tree.addAttribute(_name, name);
            tree.startContent();
        }

        if (json instanceof JSONObject) {
            tree.addStartElement(jx_object);
            tree.startContent();
            buildJxmlPairs(tree, (JSONObject) json);
            tree.addEndElement();
        } else if (json instanceof JSONArray) {
            tree.addStartElement(jx_array);
            tree.startContent();
            buildJxmlArray(tree, (JSONArray) json);
            tree.addEndElement();
        } else if (json instanceof Integer || json instanceof Double || json instanceof Long) {
            tree.addStartElement(jx_number);
            tree.startContent();
            tree.addText(json.toString());
            tree.addEndElement();
        } else if (json instanceof String) {
            tree.addStartElement(jx_string);
            tree.startContent();
            tree.addText(json.toString());
            tree.addEndElement();
        } else if (json instanceof Boolean) {
            tree.addStartElement(jx_boolean);
            tree.startContent();
            tree.addText(json.toString());
            tree.addEndElement();
        } else if (json == JSONObject.NULL) {
            tree.addStartElement(jx_null);
            tree.startContent();
            tree.addEndElement();
        } else {
            throw new XProcException("Unexpected type in JSON conversion.");
        }

        if (name != null) {
            tree.addEndElement();
        }
    }

    private static void buildMine(TreeWriter tree, JSONTokener jt, boolean usens) {
        tree.addStartElement(usens ? c_json : _json);

        try {
            char ch = jt.next();
            jt.back();

            if (ch == '{') {
                tree.addAttribute(_type, "object");
                tree.startContent();
                buildMyPairs(tree, new JSONObject(jt), usens);
            } else {
                tree.addAttribute(_type, "array");
                tree.startContent();
                buildMyArray(tree, new JSONArray(jt), usens);
            }
        } catch (JSONException je) {
            throw new XProcException(je);
        }

        tree.addEndElement();
    }

    private static void buildMyPairs(TreeWriter tree, JSONObject jo, boolean usens) {
        try {
            Iterator<String> keys = jo.keys();
            while (keys.hasNext()) {
                String name = keys.next();
                Object json = jo.get(name);
                tree.addStartElement(usens ? c_pair : _pair);
                tree.addAttribute(_name, name);

                if (json instanceof JSONObject) {
                    tree.addAttribute(_type, "object");
                    tree.startContent();
                    buildMyPairs(tree, (JSONObject) json, usens);
                } else if (json instanceof JSONArray) {
                    tree.addAttribute(_type, "array");
                    tree.startContent();
                    buildMyArray(tree, (JSONArray) json, usens);
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

    private static void buildMyArray(TreeWriter tree, JSONArray arr, boolean usens) {
        try {
            for (int pos = 0; pos < arr.length(); pos++) {
                Object json = arr.get(pos);

                tree.addStartElement(usens ? c_item : _item);

                if (json instanceof JSONObject) {
                    tree.addAttribute(_type, "object");
                    tree.startContent();
                    buildMyPairs(tree, (JSONObject) json, usens);
                } else if (json instanceof JSONArray) {
                    tree.addAttribute(_type, "array");
                    tree.startContent();
                    buildMyArray(tree, (JSONArray) json, usens);
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
