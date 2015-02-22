/*
 * PipeBinding.java
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ndw
 */
public class PipeBinding extends Binding {
    private EndPoint input = null;
    private Output output = null;
    private static int idcounter = 0;
    private int id = -1;
    
    /** Creates a new instance of PipeBinding */
    public PipeBinding(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
        bindingType = PIPE_BINDING;
        id = idcounter++;
    }
    
    public void setInput(EndPoint input) {
        this.input = input;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public Output getOutput() {
        return output;
    }
    
    public EndPoint getInput() {
        return input;
    }
    
    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        logger.trace(indent + this + " from " + output + " to " + input);
    }
    
    public String toString() {
        return "[pipe binding #" + id + "]";
    }
}
