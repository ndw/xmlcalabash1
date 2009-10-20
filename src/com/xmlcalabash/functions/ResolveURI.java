package com.xmlcalabash.functions;

import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.AnyURIValue;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
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

public class ResolveURI extends SystemFunction {
    private XProcRuntime runtime;
    private transient boolean checked = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public ResolveURI(XProcRuntime runtime) {
        this.runtime = runtime;
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(visitor);
    }

    /**
     * preEvaluate: this method performs compile-time evaluation for properties in the XSLT namespace only
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        NodeInfo node;

        URI baseURI = null;
        String relative = argument[0].evaluateItem(context).getStringValue();

        if (argument.length > 1) {
            try {
                baseURI = new URI(argument[1].evaluateItem(context).getStringValue());
            } catch (URISyntaxException use) {
                throw new XProcException(use);
            }
        } else {
            node = (NodeInfo) context.getContextItem();
            if (node == null) {
                baseURI = runtime.getStaticBaseURI();
            } else {
                String s = node.getBaseURI();
                if (s == null) {
                    baseURI = runtime.getStaticBaseURI();
                } else {
                    try {
                        baseURI = new URI(s);
                    } catch (URISyntaxException use) {
                        baseURI = runtime.getStaticBaseURI();
                    }
                }
            }
        }

        return new StringValue(baseURI.resolve(relative).toString());
    }
}