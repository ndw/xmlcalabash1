/*
 * PipeNameBinding.java
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
public class PipeNameBinding extends Binding {
    private String step = null;
    private String port = null;
    
    /**
     * Creates a new instance of PipeNameBinding
     */
    public PipeNameBinding(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
        bindingType = PIPE_NAME_BINDING;
    }

    public void setStep(String step) {
        this.step = step;
    }
    
    public void setPort(String port) {
        this.port = port;
    }
    
    public String getStep() {
        return step;
    }
    
    public String getPort() {
        return port;
    }
    
    public String toString() {
        return "pipe name binding to " + getPort() + " on " + getStep();
    }
    
    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        logger.trace(indent + toString());
    }
}
