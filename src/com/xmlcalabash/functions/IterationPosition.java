package com.xmlcalabash.functions;

import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.om.Item;
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
 * Implementation of the XProc p:iteration-position function
 */

public class IterationPosition extends SystemFunction {
    private XProcRuntime runtime;
    private transient boolean checked = false;
    private int position = 1;
    // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public IterationPosition(XProcRuntime runtime) {
        this.runtime = runtime;
    }

    public void setPosition(int pos) {
        position = pos;
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(visitor);
    }

    /**
     * preEvaluate: iteration-position can never be resolved at compile-time
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return new DecimalValue(position);
    }
}