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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
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
    protected Logger logger = null;

    private int readLimit = 0;
    private static final Pattern rangeRE = Pattern.compile("^.*?=(\\d*)?(,(\\d*)?)?$");
    private static final Pattern lengthRE = Pattern.compile("^length=(\\d+)(,[^;]*)?(.*)$");
    private static final Pattern leadingWhitespaceRE = Pattern.compile("^(\\s*)(\\S.*)$");
    private static final int INCLUDE_MATCH = 0;
    private static final int EXCLUDE_MATCH = 1;
    private static final int TRIM = 2;

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

    protected XPointerScheme(QName name, String data, int readLimit) {
        this.readLimit = readLimit;
        schemeName = name;
        schemeData = data;

        logger = LoggerFactory.getLogger(XPointerScheme.class);
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

    protected String selectText(BufferedReader rd, int contentLength) {
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
        try {
            rd.mark(readLimit);

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
        } finally {
            try {
                rd.reset();
            } catch (IOException ioe) {
                // oh well
            }
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

    public String selectSearchText(BufferedReader rd, int contentLength) {
        String select = textEquivalent();

        if (select == null) {
            throw new XProcException("XPointer cannot be used to select text: " + schemeName + "(" + schemeData + ")");
        }

        // search=(digit)/string/opt,(digit)/string/opt;integrity
        // Where start and end can be enclosed in character
        // and the options for start opt are "from", "after", or "trim"
        // and the options for end opt are "to", "before", or "trim"

        // Yes, this is probably all horribly inefficient...

        Matcher matcher = null;
        String origSelect = select;
        String startSearch = null;
        int startOpt = INCLUDE_MATCH;
        int startCount = 1;
        String endSearch = null;
        int endOpt = INCLUDE_MATCH;
        int endCount = 1;
        boolean found = false;
        boolean strip = false;
        int stripWS = Integer.MAX_VALUE;

        select = select.substring(7).trim();
        if ("".equals(select)) {
            malformedSearch("at least one of start/end required", origSelect);
        }

        String skip = "";
        char ch = select.charAt(0);
        if (ch == ',') {
            select = select.substring(1);
        } else {
            while (Character.isDigit(ch)) {
                skip = skip + ch;
                select = select.substring(1);
                if ("".equals(select)) {
                    malformedSearch("start must specify a search string", origSelect);
                }
                ch = select.charAt(0);
            }

            if (!"".equals(skip)) {
                startCount = Integer.parseInt(skip);
            }

            select = select.substring(1);
            int pos = select.indexOf(ch);
            if (pos < 0) {
                malformedSearch("unterminated start string", origSelect);
            }
            startSearch = select.substring(0, pos);
            select = select.substring(pos+1).trim();

            if (select.startsWith("trim")) {
                startOpt = TRIM;
                select = select.substring(4).trim();
            } else if (select.startsWith("from")) {
                startOpt = INCLUDE_MATCH;
                select = select.substring(4).trim();
            } else if (select.startsWith("after")) {
                startOpt = EXCLUDE_MATCH;
                select = select.substring(5).trim();
            } else if ("".equals(select) || select.startsWith(",")) {
                // ok
            } else {
                malformedSearch("invalid start option", origSelect);
            }
        }

        if (select.startsWith(",")) {
            select = select.substring(1);
        }

        if (!"".equals(select)) {
            skip = "";
            ch = select.charAt(0);
            while (Character.isDigit(ch)) {
                skip = skip + ch;
                select = select.substring(1);
                if ("".equals(select)) {
                    malformedSearch("end must specify a search string", origSelect);
                }
                ch = select.charAt(0);
            }

            if (!"".equals(skip)) {
                endCount = Integer.parseInt(skip);
            }

            select = select.substring(1);
            int pos = select.indexOf(ch);
            if (pos < 0) {
                malformedSearch("unterminated end string", origSelect);
            }
            endSearch = select.substring(0, pos);
            select = select.substring(pos+1).trim();

            if (select.startsWith("trim")) {
                endOpt = TRIM;
                select = select.substring(4).trim();
            } else if (select.startsWith("to")) {
                endOpt = INCLUDE_MATCH;
                select = select.substring(2).trim();
            } else if (select.startsWith("before")) {
                endOpt = EXCLUDE_MATCH;
                select = select.substring(6).trim();
            }
        }

        if (select.startsWith(";")) {
            select = select.substring(1).trim();
        }

        if (select.startsWith("strip")) {
            strip = true;
            select = select.substring(5).trim();
            if (select.startsWith(";")) {
                select = select.substring(1).trim();
            }
        }

        logger.trace("XPointer search scheme: search='" + startSearch + "';" + startOpt + ",'" + endSearch + "';" + endOpt);
        String data = "";
        try {
            rd.mark(readLimit);

            matcher = lengthRE.matcher(select);
            if (matcher.matches()) {
                int checklen = Integer.parseInt(matcher.group(1));
                String charset = matcher.group(2);
                select = matcher.group(3);

                if (contentLength >= 0) {
                    if (checklen != contentLength) {
                        throw new IllegalArgumentException("Integrity check failed: " + checklen + " != " + contentLength);
                    }
                }

                if (select.startsWith(";")) {
                    select = select.substring(1).trim();
                }

                if (select.startsWith("strip")) {
                    strip = true;
                    select = select.substring(5).trim();
                }
            }

            if (!"".equals(select)) {
                malformedSearch("unexpected characters at end", origSelect);
            }

            Vector<String> lines = new Vector<> ();
            boolean finished = false;
            boolean output = false;
            String line;
            while (!finished && (line = rd.readLine()) != null) {
                if (output && endSearch != null && line.contains(endSearch)) {
                    if (endCount == 1) {
                        output = false;
                        finished = true;
                        if (endOpt == INCLUDE_MATCH) {
                            lines.add(line);
                        }
                    }
                    endCount--;
                }

                if (output) {
                    lines.add(line);
                }

                if (startSearch == null || line.contains(startSearch)) {
                    found = true;
                    if (startCount == 1) {
                        output = true;
                        if (startOpt == INCLUDE_MATCH) {
                            lines.add(line);
                        }
                    }
                    startCount--;
                }
            }

            if (!found) {
                throw new XProcException("No matching lines found");
            }

            if (lines.size() > 0) {
                if (strip && stripWS > 0) {
                    for (String l : lines) {
                        matcher = leadingWhitespaceRE.matcher(l);
                        if (matcher.matches()) {
                            int wslen = matcher.group(1).length();
                            if (wslen < stripWS) {
                                stripWS = wslen;
                            }
                        }
                    }
                }

                while (lines.size() > 0 && startOpt == TRIM && "".equals(lines.firstElement().trim())) {
                    lines.remove(0);
                }

                while (lines.size() > 0 && endOpt == TRIM && "".equals(lines.lastElement().trim())) {
                    lines.remove(lines.size()-1);
                }
            }

            for (String l : lines) {
                if (strip && stripWS > 0 && l.length() >= stripWS) {
                    l = l.substring(stripWS);
                }
                data += l + "\n";
            }

        } catch (IOException ioe) {
            throw new XProcException(ioe);
        } finally {
            try {
                rd.reset();
            } catch (IOException ioe) {
                // oh well
            }
        }
        return data;
    }

    private void malformedSearch(String select, String msg) {
        throw new XProcException("Malformed search: " + msg + ": " + select);
    }
}
