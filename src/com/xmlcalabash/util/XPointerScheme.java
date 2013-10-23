package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/19/11
 * Time: 2:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class XPointerScheme {
    protected QName schemeName = null;
    protected String schemeData = null;
    private static final Pattern rangeRE = Pattern.compile("^.*?=(\\d*)?(,(\\d*)?)?$");
    private static final Pattern lengthRE = Pattern.compile("^length=(\\d+)(,.*)?$");

    private long sp = -1;
    private long ep = -1;
    private boolean chars = false;
    private long cp = -1;
    private long lp = -1;

    private XPointerScheme() {
        // nop;
    }

    public QName getName() {
        return schemeName;
    }

    public String getData() {
        return schemeData;
    }

    public XPointerScheme(QName name, String data) {
        schemeName = name;
        schemeData = data;
    }

    public String xpathEquivalent() {
        return null;
    }

    public String textEquivalent() {
        return null;
    }

    public Vector<XdmNode> selectNodes(XProcRuntime runtime, XdmNode doc, Hashtable<String,String> nsBindings) {
        String select = xpathEquivalent();

        if (select == null) {
            throw new XProcException("XPointer cannot be used to select nodes: " + schemeName + "(" + schemeData + ")");
        }

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

            Iterator<XdmItem> iter = selector.iterator();
            while (iter.hasNext()) {
                XdmItem item = iter.next();
                XdmNode node = null;
                try {
                    node = (XdmNode) item;
                } catch (ClassCastException cce) {
                    throw new XProcException ("XPointer matched non-node item?: " + schemeName + "(" + schemeData + ")");
                }
                selectedNodes.add(node);
            }
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        return selectedNodes;
    }

    public String selectText(InputStreamReader stream, int contentLength) {
        String select = textEquivalent();

        if (select == null) {
            throw new XProcException("XPointer cannot be used to select text: " + schemeName + "(" + schemeData + ")");
        }

        // RFC 5147:
        // text-fragment   =  text-scheme 0*( ";" integrity-check )
        // text-scheme     =  ( char-scheme / line-scheme )
        // char-scheme     =  "char=" ( position / range )
        // line-scheme     =  "line=" ( position / range )
        // integrity-check =  ( length-scheme / md5-scheme )
        //                      [ "," mime-charset ]
        // position        =  number
        // range           =  ( position "," [ position ] ) / ( "," position )
        // number          =  1*( DIGIT )
        // length-scheme   =  "length=" number
        // md5-scheme      =  "md5=" md5-value
        // md5-value       =  32HEXDIG

        String data = "";
        BufferedReader rd = null;

        rd = new BufferedReader(stream);

        String parts[] = select.split("\\s*;\\s*");
        for (int pos = 1; pos < parts.length; pos++) {
            // start at 1 because we want to skip the scheme
            String check = parts[pos];
            Matcher matcher = lengthRE.matcher(check);
            if (contentLength >= 0 && matcher.matches()) {
                int checklen = Integer.parseInt(matcher.group(1));
                if (checklen != contentLength) {
                    throw new IllegalArgumentException("Integrity check failed: " + checklen + " != " + contentLength);
                }
            }
        }
        select = parts[0];

        select = select.trim();

        sp = -1;
        ep = Long.MAX_VALUE;
        cp = 0;
        lp = 0;

        // FIXME: Isn't there a better way to do this?
        Matcher matcher = rangeRE.matcher(select);
        if (matcher.matches()) {
            String r = matcher.group(1);
            if (r != null && !"".equals(r)) {
                sp = Integer.parseInt(r);
            }
            r = matcher.group(3);
            if (r != null && !"".equals(r)) {
                ep = Integer.parseInt(r);
            }
        }

        if (select.startsWith("char=")) {
            chars = true;
        } else if (select.startsWith("line=")) {
            chars = false;
        } else {
            throw new XProcException("Unparseable XPointer: " + schemeName + "(" + schemeData + ")");
        }

        try {
            String line;
            while ((line = rd.readLine()) != null) {
                if (chars) {
                    data += selectChars(line);
                } else {
                    data += selectLines(line);
                }
            }
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }

        try {
            rd.close();
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }

        return data;
    }

    private String selectChars(String line) {
        String data = "";
        long endcp = cp + line.length()+1;

        if (cp < sp && endcp > sp) {
            line = line.substring((int) (sp - cp));
            cp = sp;
        }

        if (cp >= sp && cp < ep) {
            long rest = ep - cp;
            if (rest > line.length()) {
                data = line + "\n";
                cp = endcp;
            } else {
                data += line.substring(0,(int) rest);
                cp += rest;
            }
        }

        cp = endcp;

        return data;
    }

    private String selectLines(String line) {
        String data = "";
        if (lp >= sp && lp < ep) {
            data = line + "\n";
        }
        lp++;
        return data;
    }
}
