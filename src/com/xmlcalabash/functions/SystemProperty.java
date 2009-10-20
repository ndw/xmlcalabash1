package com.xmlcalabash.functions;

import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.Item;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;

/**
 * Implementation of the XSLT system-property() function
 */

public class SystemProperty extends SystemFunction {
    private XProcRuntime runtime;
    private NamespaceResolver nsContext;
    private StructuredQName propertyName;
    private transient boolean checked = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public SystemProperty(XProcRuntime runtime) {
        this.runtime = runtime;
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(visitor);
        if (argument[0] instanceof StringLiteral) {
            try {
                propertyName = StructuredQName.fromLexicalQName(
                        ((StringLiteral)argument[0]).getStringValue(),
                        false,
                        visitor.getConfiguration().getNameChecker(),
                        visitor.getStaticContext().getNamespaceResolver());
            } catch (XPathException e) {
                if (e.getErrorCodeLocalPart()==null || e.getErrorCodeLocalPart().equals("FOCA0002")
                        || e.getErrorCodeLocalPart().equals("FONS0004")) {
                    e.setErrorCode("XTDE1390");
                    throw e;
                }
            }
            // Don't actually read the system property yet, it might be different at run-time
        } else {
            // we need to save the namespace context
            nsContext = visitor.getStaticContext().getNamespaceResolver();
        }
    }

    /**
     * preEvaluate: this method performs compile-time evaluation for properties in the XSLT namespace only
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (propertyName != null && XProcConstants.NS_XPROC.equals(propertyName.getNamespaceURI())) {
            return new StringLiteral(getProperty(propertyName.getNamespaceURI(), propertyName.getLocalName()));
        } else {
           return this;
        }
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        StructuredQName qName = propertyName;
        if (qName == null) {
            CharSequence name = argument[0].evaluateItem(context).getStringValueCS();
            try {
                qName = StructuredQName.fromLexicalQName(name,
                        false,
                        context.getConfiguration().getNameChecker(),
                        nsContext);
            } catch (XPathException err) {
                 dynamicError("Invalid system property name. " + err.getMessage(), "XTDE1390", context);
                 return null;
            }
        }
        return new StringValue(getProperty(qName.getNamespaceURI(), qName.getLocalName()));
    }

    /**
     * Here's the real code:
     * @param uri the namespace URI of the system property name
     * @param local the local part of the system property name
     * @return the value of the corresponding system property
    */

    private String getProperty(String uri, String local) {
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
                return "";
            }
	    } else {
            return "";
        }
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by Norman Walsh are Copyright (C) Mark Logic Corporation. All Rights Reserved.
//
// Contributor(s): Norman Walsh.
//
