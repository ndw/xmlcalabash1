/*
 * Catch.java
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

/**
 *
 * @author ndw
 */
public class Catch extends DeclareStep {
    
    /** Creates a new instance of Catch */
    public Catch(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_catch;
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    @Override
    protected void augmentIO() {
        // Output bindings on a compound step are really the input half of an input/output binding;
        // create the other half
        for (Output output : outputs) {
            Input input = getInput("|" + output.getPort());
            if (input == null) {
                input = new Input(runtime, output.getNode());
                input.setPort("|" + output.getPort());
                input.setSequence(true); // the other half will check
                input.setPrimary(output.getPrimary());
                addInput(input);
            }
        }
        
        // The only input on catch is errors and it's special
    }
}
