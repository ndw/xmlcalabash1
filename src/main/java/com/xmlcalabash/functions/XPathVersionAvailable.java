package com.xmlcalabash.functions;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XCompoundStep;
import com.xmlcalabash.runtime.XStep;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.DoubleValue;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;

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
 * Implementation of the XSLT system-property() function
 */

public class XPathVersionAvailable extends XProcExtensionFunctionDefinition {
    private static StructuredQName funcname = new StructuredQName("p", XProcConstants.NS_XPROC, "xpath-version-available");

    protected XPathVersionAvailable() {
        // you can't call this one
    }

    public XPathVersionAvailable(XProcRuntime runtime) {
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
        return new SequenceType[]{SequenceType.SINGLE_DOUBLE};
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_BOOLEAN;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new XPathVersionAvailableCall(this);
    }

    private class XPathVersionAvailableCall extends ExtensionFunctionCall {
        private XProcExtensionFunctionDefinition xdef = null;

        public XPathVersionAvailableCall(XProcExtensionFunctionDefinition def) {
            xdef = def;
        }

        public Sequence call(XPathContext xPathContext, Sequence[] sequences) throws XPathException {
            XProcRuntime runtime = registry.getRuntime(xdef);
            XStep step = runtime.getXProcData().getStep();
            // FIXME: this can't be the best way to do this...
            // step == null in use-when
            if (step != null && !(step instanceof XCompoundStep)) {
                throw XProcException.dynamicError(23);
            }

            DoubleValue value = (DoubleValue) sequences[0].head();
            double reqVer = value.getDoubleValue();

            return (reqVer == 1.0 || reqVer == 2.0) ? BooleanValue.TRUE : BooleanValue.FALSE;
        }
    }
}