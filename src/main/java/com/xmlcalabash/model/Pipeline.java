/*
 * Pipeline.java
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
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

import java.util.Vector;

public class Pipeline extends CompoundStep {
    private Vector<Import> imports = new Vector<Import> ();
    protected boolean psviRequired = false;
    protected String xpathVersion = "2.0";
    private QName declaredType = null;
    private DeclareStep declaration = null;

    /* Creates a new instance of DeclareStep */
    public Pipeline(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, XProcConstants.p_pipeline, name);
    }

    public void setPsviRequired(boolean psvi) {
        psviRequired = psvi;
    }

    public void setXPathVersion(String version) {
        xpathVersion = version;
    }

    public void setDeclaredType(QName type) {
        declaredType = type;
    }

    public QName getDeclaredType() {
        return declaredType;
    }

    public void setDeclaration(DeclareStep step) {
        declaration = step;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    public void addStep(Step step) {
        super.addStep(step);
    }

    public void addImport(Import importelem) {
        imports.add(importelem);
    }

    public void setupEnvironment() {
        setEnvironment(new Environment(this));
    }

    protected void patchEnvironment(Environment env) {
        // See if there's exactly one "ordinary" input
        int count = 0;
        Input defin = null;
        boolean foundPrimary = false;
        for (Input input : inputs) {
            if (!input.getPort().startsWith("|") && !input.getParameterInput()) {
                count++;
                foundPrimary |= input.getPrimary();
                if (defin == null || input.getPrimary()) {
                    defin = input;
                }
            }
        }

        if (count == 1 || foundPrimary) {
            env.setDefaultReadablePort(defin);
        }
    }
}

