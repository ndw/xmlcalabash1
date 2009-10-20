package com.xmlcalabash.functions;

import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.Item;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.ItemTypeFactory;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.type.BuiltInAtomicType;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XStep;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.model.DeclareStep;

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

public class StepAvailable extends SystemFunction {
    private XProcRuntime runtime;
    private NamespaceResolver nsContext;
    private StructuredQName stepName;
    private XStep xstep;
    private transient boolean checked = false;
    // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public StepAvailable(XProcRuntime runtime, XStep xstep) {
        this.runtime = runtime;
        this.xstep = xstep;
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(visitor);
        if (argument[0] instanceof StringLiteral) {
            try {
                stepName = StructuredQName.fromLexicalQName(
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
     * preEvaluate: step-available can never be resolved at compile-time
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        StructuredQName qName = stepName;
        if (qName == null) {
            CharSequence name = argument[0].evaluateItem(context).getStringValueCS();
            try {
                qName = StructuredQName.fromLexicalQName(name,
                        false,
                        context.getConfiguration().getNameChecker(),
                        nsContext);
            } catch (XPathException err) {
                dynamicError("Invalid step name. " + err.getMessage(), "XTDE1390", context);
                return null;
            }
        }

        QName stepType = new QName("x", qName.getNamespaceURI(), qName.getLocalName());

        // FIXME: This doesn't seem terribly efficient...

        XStep step = xstep;
        while (! (step instanceof XPipeline)) {
            step = step.getParent();
        }

        DeclareStep decl = step.getDeclareStep();

        try {
            decl = decl.getStepDeclaration(stepType);
        } catch (XProcException e) {
            decl = null;
        }

        if (decl != null) {
            if (decl.isAtomic()) {
                String className = runtime.getConfiguration().implementationClass(decl.getDeclaredType());
                return new BooleanValue(className != null, BuiltInAtomicType.BOOLEAN);
            } else {
                return new BooleanValue(true, BuiltInAtomicType.BOOLEAN); 
            }
        } else {
            return new BooleanValue(false, BuiltInAtomicType.BOOLEAN);
        }
    }
}