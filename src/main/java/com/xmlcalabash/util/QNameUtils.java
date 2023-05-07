package com.xmlcalabash.util;

import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.om.StructuredQName;

public class QNameUtils {
    public static boolean hasForm(StructuredQName qname, String namespace, String localname) {
        return qname != null && namespace.equals(qname.getNamespaceUri().toString()) && localname.equals(qname.getLocalPart());
    }

    public static boolean hasForm(StructuredQName qname, NamespaceUri namespace, String localname) {
        return qname != null && namespace == qname.getNamespaceUri() && localname.equals(qname.getLocalPart());
    }
}
