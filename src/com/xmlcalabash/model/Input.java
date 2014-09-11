/*
 * Input.java
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
public class Input extends Port {
    private String select = null;
    private boolean debugReader = false;
    private boolean debugWriter = false;
    private boolean parameterInput = false;
    private int position = 0;

    /** Creates a new instance of Input */
    public Input(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }

    public void setSelect(String select) {
        this.select = select;
    }

    public String getSelect() {
        return select;
    }

    public void setParameterInput() {
        parameterInput = true;
    }
    
    public void setParameterInput(boolean value) {
        parameterInput = value;
    }

    public boolean getParameterInput() {
        return parameterInput;
    }

    public void setPosition(int pos) {
        position = pos;
    }
    
    public int getPosition() {
        return position;
    }

    public void setDebugReader(boolean debug) {
        debugReader = debug;
    }
    
    public void setDebugWriter(boolean debug) {
        debugWriter = debug;
    }
    
    public boolean getDebugReader() {
        return debugReader;
    }

    public boolean getDebugWriter() {
        return debugWriter;
    }
    
    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        logger.trace(indent + "input " + getPort());
        for (Binding binding : getBinding()) {
            binding.dump(depth+2);
        }
    }

    @Override
    public String toString() {
        if (getStep() == null) {
            return "[input " + getPort() + " on null]";
        } else {
            return "[input " + getPort() + " on " + getStep().getName() + "]";
        }
    }        
    
}
