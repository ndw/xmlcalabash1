package com.xmlcalabash.functions;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XCompoundStep;
import com.xmlcalabash.runtime.XStep;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;
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
        registry.registerRuntime(this, runtime);
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
        return new SystemPropertyCall(this);
    }

    private class SystemPropertyCall extends ExtensionFunctionCall {
        private StaticContext staticContext = null;
        private XProcExtensionFunctionDefinition xdef = null;

        public SystemPropertyCall(XProcExtensionFunctionDefinition def) {
            xdef = def;
        }

        public void supplyStaticContext(StaticContext context, int locationId, Expression[] arguments) throws XPathException {
            staticContext = context;
        }

        public Sequence call(XPathContext xPathContext, Sequence[] sequences) throws XPathException {
            QName propertyName = null;

            XProcRuntime runtime = registry.getRuntime(xdef);
            XStep step = runtime.getXProcData().getStep();
            // FIXME: this can't be the best way to do this...
            // FIXME: And what, exactly, is this even supposed to be doing!?
            if (step != null && !(step instanceof XCompoundStep)) {
                throw XProcException.dynamicError(23);
            }

            try {
                String lexicalQName = sequences[0].head().getStringValue();
                StructuredQName qpropertyName = StructuredQName.fromLexicalQName(
                        lexicalQName,
                        false,
                        false,
                        staticContext.getNamespaceResolver());
                propertyName = new QName(qpropertyName);
            } catch (XPathException e) {
                if (e.getErrorCodeLocalPart()==null || e.getErrorCodeLocalPart().equals("FOCA0002")
                        || e.getErrorCodeLocalPart().equals("FONS0004")) {
                    e.setErrorCode("XTDE1390");
                }
                throw e;
            }

            String value = runtime.getSystemProperty(propertyName);
            if (value == null) {
                value = "";
            }

            return new StringValue(value);
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
