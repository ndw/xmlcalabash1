package com.xmlcalabash.util;

import net.sf.saxon.s9api.*;

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

    private Vector<XPointerScheme> parts = new Vector<XPointerScheme> ();

    public XPointer(String xpointer) {
        String pointer = xpointer;
        while (pointer != null) {
            pointer = parse(pointer);
        }
    }

    public String xpathEquivalent() {
        int pos = recognizedScheme();
        if (pos < 0) {
            throw new XProcException("No recognized XPointer schemes.");
        }

        XPointerScheme scheme = parts.get(pos);

        if (scheme instanceof XPointerElementScheme) {
            String xpath = "";
            String data = scheme.schemeData;
            pos = data.indexOf("/");
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
        } else if (scheme instanceof XPointerXPathScheme) {
            return scheme.schemeData;
        } else {
            throw new XProcException("No recognized XPointer schemes.");
        }
    }

    public Hashtable<String,String> xpathNamespaces() {
        Hashtable<String,String> bindings = new Hashtable<String,String> ();
        int end = recognizedScheme();
        int pos;
        for (pos = 0; pos < end; pos++) {
            XPointerScheme scheme = parts.get(pos);
            if (scheme instanceof XPointerXmlnsScheme) {
                XPointerXmlnsScheme xs = (XPointerXmlnsScheme) scheme;
                bindings.put(xs.prefix,xs.uri);
            }
        }
        return bindings;
    }

    public Vector<XdmNode> selectNodes(XProcRuntime runtime, XdmNode doc, String select, Hashtable<String,String> nsBindings) {
        Vector<XdmNode> selectedNodes = new Vector<XdmNode> ();

        XPathSelector selector = null;
        XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
        for (String prefix : nsBindings.keySet()) {
            xcomp.declareNamespace(prefix, nsBindings.get(prefix));
        }

        try {
            XPathExecutable xexec = xcomp.compile(select);
            selector = xexec.load();
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        try {
            selector.setContextItem(doc);

            Iterator iter = selector.iterator();
            while (iter.hasNext()) {
                XdmItem item = (XdmItem) iter.next();
                XdmNode node = null;
                try {
                    node = (XdmNode) item;
                } catch (ClassCastException cce) {
                    throw new XProcException ("XPointer matched non-node item?");
                }
                selectedNodes.add(node);
            }
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        return selectedNodes;
    }

    private int recognizedScheme() {
        int pos = 0;
        while (pos < parts.size()) {
            XPointerScheme scheme = parts.get(pos);
            if (scheme instanceof XPointerElementScheme
                    || scheme instanceof XPointerXPathScheme) {
                return pos;
            }
            pos++;
        }
        return -1;
    }

    private String parse(String xpointer) {
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

    private class XPointerScheme {
        protected QName schemeName = null;
        protected String schemeData = null;

        public XPointerScheme() {
            // nop;
        }

        public XPointerScheme(QName name, String data) {
            schemeName = name;
            schemeData = data;
        }
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
    }

    private class XPointerElementScheme extends XPointerScheme {
        public XPointerElementScheme(QName name, String data) {
            super(name,data);
        }
    }

    private class XPointerXPathScheme extends XPointerScheme {
        public XPointerXPathScheme(QName name, String data) {
            super(name,data);
        }
    }
}
