package com.xmlcalabash.util;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import java.net.URI;

/**
 * Created by ndw on 8/27/14.
 */
public class MessageFormatter {
    public static String codeMessage(QName code, String message) {
        if (code != null) {
            return message + ": " + code;
        } else {
            return message;
        }
    }

    public static String nodeMessage(XdmNode node, String message) {
        String prefix = "";
        if (node != null) {
            URI cwd = URIUtils.cwdAsURI();
            String systemId = cwd.relativize(node.getBaseURI()).toASCIIString();
            int line = node.getLineNumber();
            int col = node.getColumnNumber();

            if (systemId != null && !"".equals(systemId)) {
                prefix = prefix + systemId + ":";
            }
            if (line != -1) {
                prefix = prefix + line + ":";
            }
            if (col != -1) {
                prefix = prefix + col + ":";
            }
        }

        return prefix + message;
    }
}
