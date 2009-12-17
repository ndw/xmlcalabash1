package com.xmlcalabash.functions;

import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.functions.ExtensionFunctionDefinition;
import net.sf.saxon.functions.ExtensionFunctionCall;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.AnyURIValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.om.*;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;

import java.net.URI;
import java.net.URISyntaxException;

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

public class ResolveURI extends ExtensionFunctionDefinition {
    private static StructuredQName funcname = new StructuredQName("p", XProcConstants.NS_XPROC, "resolve-uri");
    private XProcRuntime runtime = null;

    protected ResolveURI() {
        // you can't call this one
    }

    public ResolveURI(XProcRuntime runtime) {
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
        return new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.OPTIONAL_STRING};
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_ATOMIC;
    }

    public boolean dependsOnFocus() {
        return true;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new ResolveURICall();
    }

    private class ResolveURICall extends ExtensionFunctionCall {
        public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
            SequenceIterator iter = arguments[0];
            String relativeURI = iter.next().getStringValue();

            String baseURI = null;
            if (arguments.length > 1) {
                iter = arguments[1];
                baseURI = iter.next().getStringValue();
            } else {
                baseURI = runtime.getStaticBaseURI().toASCIIString();
            }

            String resolvedURI = "";

            try {
                URI uri = new URI(baseURI);
                resolvedURI = uri.resolve(relativeURI).toASCIIString();
            } catch (URISyntaxException use) {
                // FIXME: what should we do here?
            }

            return SingletonIterator.makeIterator(
                    new AnyURIValue(resolvedURI));
        }
    }
}