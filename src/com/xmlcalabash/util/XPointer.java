package com.xmlcalabash.util;

import net.sf.saxon.s9api.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 18, 2008
 * Time: 2:17:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class XPointer {
    private static final QName _xmlns = new QName("", "xmlns");
    private static final QName _element = new QName("", "element");
    private static final QName _xpath = new QName("", "xpath");
    private static final QName _text = new QName("", "text");

    private Vector<XPointerScheme> parts = new Vector<XPointerScheme> ();

    public XPointer(String xpointer) {
        String pointer = xpointer;
        while (pointer != null) {
            pointer = parse(pointer);
        }
    }

    public Hashtable<String,String> xpathNamespaces() {
        Hashtable<String,String> bindings = new Hashtable<String,String> ();
        for (XPointerScheme scheme : parts) {
            if (_xmlns.equals(scheme.getName())) {
                XPointerXmlnsScheme xmlns = (XPointerXmlnsScheme) scheme;
                bindings.put(xmlns.getPrefix(), xmlns.getURI());
            }
        }
        return bindings;
    }

    public Vector<XdmNode> selectNodes(XProcRuntime runtime, XdmNode doc) {
        Vector<XdmNode> result = null;

        for (XPointerScheme scheme : parts) {
            String select = scheme.xpathEquivalent();
            if (result == null && select != null) {
                try {
                    result = scheme.selectNodes(runtime, doc, xpathNamespaces());
                } catch (XProcException e) {
                    result = null;
                    // try the next one
                }
            }
        }

        return result;
    }

    public String selectText(InputStreamReader stream, int contentLength) {
        String result = null;
        RuntimeException except = null;

        for (XPointerScheme scheme : parts) {
            String select = scheme.textEquivalent();
            if (result == null && select != null) {
                try {
                    result = scheme.selectText(stream, contentLength);
                } catch (IllegalArgumentException iae) {
                    // in this case we will never have started reading the file, so we're good to go
                    except = iae;
                    result = null;
                } catch (XProcException xe) {
                    // try the next one
                    except = xe;
                    result = null;
                    try {
                        stream.reset();
                    } catch (IOException ioe) {
                        throw new XProcException(ioe);
                    }
                }
            }
        }

        if (result == null && except != null) {
            throw except;
        }

        return result;
    }

    private String parse(String xpointer) {
        // FIXME: Hack! Is this acceptable?
        if (xpointer.startsWith("/") && !xpointer.contains("(")) {
            xpointer = "element(" + xpointer + ")";
        }

        xpointer = xpointer.trim();

        if (xpointer.matches("^[\\w:]+\\s*\\(.*")) {
            Pattern scheme = Pattern.compile("^([\\w+:]+)\\s*(\\(.*)$");
            Matcher matcher = scheme.matcher(xpointer);
            if (matcher.matches()) { // scheme(data) ...
                QName name = schemeName(matcher.group(1));
                String data = matcher.group(2);
                int dataend = indexOfEnd(data);

                if (dataend < 0) {
                    throw new XProcException("Unparseable XPointer: " + xpointer);
                }

                String rest = data.substring(dataend);
                data = data.substring(1, dataend-1);     // 1 because we want to skip the initial "("

                data = cleanup(data);

                if (_xmlns.equals(name)) {
                    parts.add(new XPointerXmlnsScheme(name, data));
                } else if (_element.equals(name)) {
                    parts.add(new XPointerElementScheme(name, data));
                } else if (_xpath.equals(name)) {
                    parts.add(new XPointerXPathScheme(name, data));
                } else if (_text.equals(name)) {
                    parts.add(new XPointerTextScheme(name, data));
                } else {
                    parts.add(new XPointerScheme(name, data));
                }

                if ("".equals(rest)) {
                    rest = null;
                }
                return rest;
            } else {
                scheme = Pattern.compile("^([\\w+:]+)\\s*\\(\\)\\s*(.*)$");
                matcher = scheme.matcher(xpointer);
                if (matcher.matches()) { // scheme() ...
                    QName name = schemeName(matcher.group(1));
                    String data = cleanup(matcher.group(2));

                    parts.add(new XPointerScheme(name, data));

                    String rest = matcher.group(3);
                    if ("".equals(rest)) {
                        rest = null;
                    }
                    return rest;
                } else {
                    throw new XProcException("Unparseable XPointer: " + xpointer);
                }
            }
        } else if (xpointer.matches("^[\\w:]+\\s*$")) {
            parts.add(new XPointerScheme(_element, xpointer));
            return null;
        } else {
            throw new XProcException("Unparseable XPointer: " + xpointer);
        }
    }

    private QName schemeName(String name) {
        if (name.contains(":")) {
            int pos = name.indexOf(":");
            String pfx = name.substring(0,pos);
            String lcl = name.substring(pos+1);
            QName qname = null;
            for (pos = parts.size()-1; qname == null && pos >= 0; pos--) {
                XPointerScheme scheme = parts.get(pos);
                if (scheme instanceof XPointerXmlnsScheme) {
                    String prefix = ((XPointerXmlnsScheme) scheme).prefix;
                    String uri = ((XPointerXmlnsScheme) scheme).uri;
                    if (pfx.equals(prefix)) {
                        return new QName(pfx, uri, lcl);
                    }
                }
            }
        } else {
            return new QName("", name);
        }

        throw new XProcException("Scheme name without bound prefix: " + name);
    }

    private int indexOfEnd(String data) {
        // Make sure we don't get fooled by ^^, ^(, or ^)
        data = data.replaceAll("\\^[\\(\\)]", "xx");

        int depth = 0;
        int pos = 0;
        boolean done = false;
        while (pos < data.length() && !done) {
            String s = data.substring(pos, pos+1);

            if ("(".equals(s)) {
                depth++;
            } else if (")".equals(s)) {
                depth--;
            }

            done = (")".equals(s) && depth == 0);

            pos++;
        }

        if (depth != 0) {
            return -1;
        } else {
            return pos;
        }
    }

    private String cleanup(String data) {
        String repl = data.replaceAll("\\^([\\(\\)\\^])", "$1");
        return repl;
    }

    private class XPointerXmlnsScheme extends XPointerScheme {
        protected String prefix = null;
        protected String uri = null;

        public XPointerXmlnsScheme(QName name, String data) {
            super(name,data);

            Pattern scheme = Pattern.compile("([\\w:]+)\\s*=\\s*([^=]+)$");
            Matcher matcher = scheme.matcher(data);
            if (matcher.matches()) {
                prefix = matcher.group(1);
                uri = matcher.group(2);
            } else {
                throw new XProcException("Unparseable xmlns(): " + data);
            }
        }

        public String getPrefix() {
            return prefix;
        }

        public String getURI() {
            return uri;
        }
    }

    private class XPointerElementScheme extends XPointerScheme {
        public XPointerElementScheme(QName name, String data) {
            super(name,data);
        }

        public String xpathEquivalent() {
            String xpath = "";
            String data = schemeData;
            int pos = data.indexOf("/");
            if (pos < 0) {
                return "id('" + data + "')";
            }

            if (pos > 0) {
                xpath = "id('" + data.substring(0,pos) + "')";
                data = data.substring(pos);
            }

            Pattern dscheme = Pattern.compile("^/(\\d+)(.*)$");
            Matcher dmatcher = dscheme.matcher(data);
            while (dmatcher.matches()) {
                xpath += "/*[" + dmatcher.group(1) + "]";
                data = dmatcher.group(2);
                dmatcher = dscheme.matcher(data);
            }

            if (!"".equals(data)) {
                throw new XProcException("Element pointer didn't parse.");
            }

            return xpath;
        }
    }

    private class XPointerXPathScheme extends XPointerScheme {
        public XPointerXPathScheme(QName name, String data) {
            super(name,data);
        }

        public String xpathEquivalent() {
            return schemeData;
        }
    }

    private class XPointerTextScheme extends XPointerScheme {
        public XPointerTextScheme(QName name, String data) {
            super(name,data);
        }

        public String textEquivalent() {
            return schemeData;
        }
    }
}
