package com.xmlcalabash.functions;

import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.functions.ExtensionFunctionDefinition;
import net.sf.saxon.functions.ExtensionFunctionCall;
import net.sf.saxon.expr.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.type.BuiltInAtomicType;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XStep;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.RuntimeValue;

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

public class ValueAvailable extends ExtensionFunctionDefinition {
    private XProcRuntime runtime;
    private static StructuredQName funcname = new StructuredQName("p", XProcConstants.NS_XPROC, "value-available");

    protected ValueAvailable() {
        // you can't call this one
    }

    public ValueAvailable(XProcRuntime runtime) {
        this.runtime = runtime;
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
            XStep step = runtime.getXProcData().getStep();
            QName varName = new QName(sVarName.getNamespaceURI(), sVarName.getLocalName());

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