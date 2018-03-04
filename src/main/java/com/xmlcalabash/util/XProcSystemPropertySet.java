package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.s9api.QName;

/**
 * Used to resolve XProc <a href="http://www.w3.org/TR/xproc/#f.system-property">system properties</a>.
 * One of these is built into XML Calabash, but more can be added by applications using it.
 * See PR #186
 */
public interface XProcSystemPropertySet {
    /**
     * Looks up a <a href="http://www.w3.org/TR/xproc/#f.system-property">system property</a> by the given name.
     * If this {@code XProcSystemPropertySet} does not have any such system property, this method returns {@code null}.
     *
     * @param runtime the XProc runtime in which the call was made
     * @param propertyName the name of the system property to look up
     * @return the string value of that system property, or {@code null}
     * @throws XProcException if any error occurs
     */
    String systemProperty(XProcRuntime runtime, QName propertyName) throws XProcException;

    /**
     * The built-in system property set. It will answer the system properties <a href="http://www.w3.org/TR/xproc/#f.system-property">listed in the XProc specification</a>, and a number of system properties specific to XML Calabash.
     */
    XProcSystemPropertySet BUILTIN = new XProcSystemPropertySet() {
        @Override
        public String systemProperty(XProcRuntime runtime, QName propertyName) throws XProcException {
            String uri = propertyName.getNamespaceURI();
            String local = propertyName.getLocalName();

            if (uri.equals(XProcConstants.NS_XPROC)) {
                if ("episode".equals(local)) {
                    return runtime.getEpisode();
                } else if ("language".equals(local)) {
                    return runtime.getLanguage();
                } else if ("product-name".equals(local)) {
                    return runtime.getProductName();
                } else if ("product-version".equals(local)) {
                    return runtime.getProductVersion();
                } else if ("vendor".equals(local)) {
                    return runtime.getVendor();
                } else if ("vendor-uri".equals(local)) {
                    return runtime.getVendorURI();
                } else if ("version".equals(local)) {
                    return runtime.getXProcVersion();
                } else if ("xpath-version".equals(local)) {
                    return runtime.getXPathVersion();
                } else if ("psvi-supported".equals(local)) {
                    return runtime.getPSVISupported() ? "true" : "false";
                } else {
                    return null;
                }
            } else if (uri.equals(XProcConstants.NS_CALABASH_EX)) {
                if ("transparent-json".equals(local)) {
                    return runtime.transparentJSON() ? "true" : "false";
                } else if ("json-flavor".equals(local)) {
                    return runtime.jsonFlavor();
                } else if ("general-values".equals(local)) {
                    return runtime.getAllowGeneralExpressions() ? "true" : "false";
                } else if ("xpointer-on-text".equals(local)) {
                    return runtime.getAllowXPointerOnText() ? "true" : "false";
                } else if ("use-xslt-1.0".equals(local) || "use-xslt-10".equals(local)) {
                    return runtime.getUseXslt10Processor() ? "true" : "false";
                } else if ("html-serializer".equals(local)) {
                    return runtime.getHtmlSerializer() ? "true" : "false";
                } else if ("saxon-version".equals(local)) {
                    return runtime.getConfiguration().getProcessor().getSaxonProductVersion();
                } else if ("saxon-edition".equals(local)) {
                    return runtime.getConfiguration().saxonProcessor;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    };
}

