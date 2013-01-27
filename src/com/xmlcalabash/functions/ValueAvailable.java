package com.xmlcalabash.functions;

import com.xmlcalabash.runtime.XCompoundStep;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XStep;

import java.util.Hashtable;

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

/**
 * Implementation of the XProc p:step-available function
 */

public class ValueAvailable extends XProcExtensionFunctionDefinition {
    private static StructuredQName funcname = new StructuredQName("p", XProcConstants.NS_XPROC, "value-available");

    protected ValueAvailable() {
        // you can't call this one
    }

    public ValueAvailable(XProcRuntime runtime) {
        tl_runtime.set(runtime);
    }

    public StructuredQName getFunctionQName() {
        return funcname;
    }

    public int getMinimumNumberOfArguments() {
        return 1;
    }

    public int getMaximumNumberOfArguments() {
        return 2;
    }

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.OPTIONAL_BOOLEAN};
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_ATOMIC;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new ValueAvailableCall();
    }

    private class ValueAvailableCall extends ExtensionFunctionCall {
        private StaticContext staticContext = null;

        public void supplyStaticContext(StaticContext context, int locationId, Expression[] arguments) throws XPathException {
            staticContext = context;
        }

        public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
            StructuredQName sVarName = null;

            XProcRuntime runtime = tl_runtime.get();
            XStep step = runtime.getXProcData().getStep();
            // FIXME: this can't be the best way to do this...
            // step == null in use-when
            if (step != null && !(step instanceof XCompoundStep)) {
                throw XProcException.dynamicError(23);
            }

            try {
                SequenceIterator iter = arguments[0];
                String lexicalQName = iter.next().getStringValue();
                sVarName = StructuredQName.fromLexicalQName(
                     lexicalQName,
                     false,
                     context.getConfiguration().getNameChecker(),
                     staticContext.getNamespaceResolver());
            } catch (XPathException e) {
                // FIXME: bad formatting
                throw new XProcException("Invalid variable/option name. " + e.getMessage() + "XTDE1390");
            }

            boolean failIfUnknown = true;

            if (arguments.length > 1) {
                SequenceIterator iter = arguments[1];
                iter = iter.next().getTypedValue();
                Item item = iter.next();
                failIfUnknown = ((BooleanValue) item).effectiveBooleanValue();
            }

            boolean value = false;
            QName varName = new QName(sVarName.getURI(), sVarName.getLocalPart());

            value = step.hasInScopeVariableBinding(varName);

            if (!value) {
                if (failIfUnknown) {
                    throw XProcException.dynamicError(33);
                }
            } else {
                value = step.hasInScopeVariableValue(varName);
            }
            
            return SingletonIterator.makeIterator(value ? BooleanValue.TRUE : BooleanValue.FALSE);
        }
    }
}