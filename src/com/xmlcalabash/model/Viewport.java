/*
 * Viewport.java
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
public class Viewport extends DeclareStep {
    RuntimeValue match = null;
    
    /** Creates a new instance of Viewport */
    public Viewport(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_viewport;

        Output current = new Output(xproc, node);
        current.setPort("#current");
        addOutput(current);
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    @Override
    public HashSet<String> getExcludeInlineNamespaces() {
        return ((DeclareStep) parent).getExcludeInlineNamespaces();
    }

    public boolean loops() {
        return true;
    }
    
    public void setMatch(RuntimeValue match) {
        this.match = match;
    }
    
    public RuntimeValue getMatch() {
        return match;
    }

    @Override
    protected void augmentIO() {
        if (getInput("#viewport-source") == null) {
            Input isource = new Input(runtime, node);
            isource.setPort("#viewport-source");
            addInput(isource);
        }
        super.augmentIO();
    }

    public Output getOutput(String portName) {
        if ("current".equals(portName)) {
            return getOutput("#current");
        } else if ("result".equals(portName)) {
            for (Output output: outputs) {
                if (!"#current".equals(output.getPort())) {
                    return output;
                }
            }
            return null;
        } else {
            return super.getOutput(portName);
        }
    }

    @Override
    public void patchEnvironment(Environment env) {
        env.setDefaultReadablePort(getOutput("#current"));
    }
    
    @Override
    public boolean valid() {
        boolean valid = true;
        
        if (match == null || "".equals(match)) {
            error(node, "Match expression on p:viewport must be specified.", XProcConstants.staticError(38));
            valid = false;
        }

        if (outputs.size() == 1) {
            error(node, "A viewport step must have a primary output", XProcConstants.staticError(6));
        }

        if (!super.valid()) {
            valid = false;
        }
        return valid;
    }
}
