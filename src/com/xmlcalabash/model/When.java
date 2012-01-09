/*
 * When.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * Portions Copyright 2007 Sun Microsystems, Inc.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://xproc.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */

package com.xmlcalabash.model;

import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

import java.util.HashSet;

/**
 *
 * @author ndw
 */
public class When extends DeclareStep {
    private String testExpr = null;
    
    /** Creates a new instance of When */
    public When(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_when;
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    public void setTest(String test) {
        testExpr = test;
    }

    public String getTest() {
        return testExpr;
    }

    @Override
    public HashSet<String> getExcludeInlineNamespaces() {
        return ((DeclareStep) parent).getExcludeInlineNamespaces();
    }

    @Override
    protected void augmentIO() {
        if (getInput("#xpath-context") == null) {
            Input isource = new Input(runtime, node);
            isource.setPort("#xpath-context");
            addInput(isource);
        }
        super.augmentIO();
    }
    
    @Override
    public boolean valid() {
        boolean valid = true;
        
        if (testExpr == null || "".equals(testExpr)) {
            runtime.error(null, node, "Test expression on p:when must be specified.", XProcConstants.staticError(38));
            valid = false;
        }

        if (!super.valid()) {
            valid = false;
        }
        return valid;
    }
}
