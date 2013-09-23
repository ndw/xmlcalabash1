package com.xmlcalabash.functions;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XCompoundStep;
import com.xmlcalabash.runtime.XStep;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.SequenceType;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;

/**
 * Implementation of the XSLT system-property() function
 */

public class SystemProperty extends XProcExtensionFunctionDefinition {
    private static StructuredQName funcname = new StructuredQName("p", XProcConstants.NS_XPROC, "system-property");

     protected SystemProperty() {
         // you can't call this one
     }

     public SystemProperty(XProcRuntime runtime) {
         tl_runtime.set(runtime);
     }

     public StructuredQName getFunctionQName() {
         return funcname;
     }

     public int getMinimumNumberOfArguments() {
         return 1;
     }

     public int getMaximumNumberOfArguments() {
         return 1;
     }

     public SequenceType[] getArgumentTypes() {
         return new SequenceType[]{SequenceType.SINGLE_STRING};
     }

     public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
         return SequenceType.SINGLE_STRING;
     }

     public ExtensionFunctionCall makeCallExpression() {
         return new SystemPropertyCall();
     }

    private class SystemPropertyCall extends ExtensionFunctionCall {
         private StaticContext staticContext = null;

         public void supplyStaticContext(StaticContext context, int locationId, Expression[] arguments) throws XPathException {
             staticContext = context;
         }

         public SequenceIterator<?> call(SequenceIterator<?>[] arguments, XPathContext context) throws XPathException {
             StructuredQName propertyName = null;

             XProcRuntime runtime = tl_runtime.get();
             XStep step = runtime.getXProcData().getStep();
             // FIXME: this can't be the best way to do this...
             // FIXME: And what, exactly, is this even supposed to be doing!?
             if (step != null && !(step instanceof XCompoundStep)) {
                 throw XProcException.dynamicError(23);
             }

             try {
                 SequenceIterator<?> iter = arguments[0];
                 String lexicalQName = iter.next().getStringValue();
                 propertyName = StructuredQName.fromLexicalQName(
                      lexicalQName,
                      false,
                      context.getConfiguration().getNameChecker(),
                      staticContext.getNamespaceResolver());
             } catch (XPathException e) {
                 if (e.getErrorCodeLocalPart()==null || e.getErrorCodeLocalPart().equals("FOCA0002")
                         || e.getErrorCodeLocalPart().equals("FONS0004")) {
                     e.setErrorCode("XTDE1390");
                 }
                 throw e;
             }

             String uri = propertyName.getURI();
             String local = propertyName.getLocalPart();
             String value = "";

             if (uri.equals(XProcConstants.NS_XPROC)) {
                 if ("episode".equals(local)) {
                     value = runtime.getEpisode();
                 } else if ("language".equals(local)) {
                     value = runtime.getLanguage();
                 } else if ("product-name".equals(local)) {
                     value = runtime.getProductName();
                 } else if ("product-version".equals(local)) {
                     value = runtime.getProductVersion();
                 } else if ("vendor".equals(local)) {
                     value = runtime.getVendor();
                 } else if ("vendor-uri".equals(local)) {
                     value = runtime.getVendorURI();
                 } else if ("version".equals(local)) {
                     value = runtime.getXProcVersion();
                 } else if ("xpath-version".equals(local)) {
                     value = runtime.getXPathVersion();
                 } else if ("psvi-supported".equals(local)) {
                     value = runtime.getPSVISupported() ? "true" : "false";
                 }
             } else if (uri.equals(XProcConstants.NS_CALABASH_EX)) {
                 if ("transparent-json".equals(local)) {
                     value = runtime.transparentJSON() ? "true" : "false";
                 } else if ("json-flavor".equals(local)) {
                     value = runtime.jsonFlavor();
                 } else if ("general-values".equals(local)) {
                     value = runtime.getAllowGeneralExpressions() ? "true" : "false";
                 } else if ("xpointer-on-text".equals(local)) {
                     value = runtime.getAllowXPointerOnText() ? "true" : "false";
                 } else if ("use-xslt-1.0".equals(local) || "use-xslt-10".equals(local)) {
                     value = runtime.getUseXslt10Processor() ? "true" : "false";
                 } else if ("saxon-version".equals(local)) {
                     value = runtime.getConfiguration().getProcessor().getSaxonProductVersion();
                 } else if ("saxon-edition".equals(local)) {
                     value = runtime.getConfiguration().saxonProcessor;
                 }
             }

             return SingletonIterator.makeIterator(
                     new StringValue(value));
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
