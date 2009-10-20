/*
 * Port.java
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

/**
 *
 * @author ndw
 */
public class Port extends EndPoint {
    private String port = null;
    private boolean sequence = false;
    private boolean primary = false;
    private boolean setPrimary = false;
    
    public Port(XProcRuntime xproc, XdmNode node) {
        super(xproc,node);
    }
    
    public void setPort(String port) {
        this.port = port;
    }
    
    public String getPort() {
        return port;
    }

    public void setSequence(String sequence) {
        this.sequence = "true".equals(sequence);
    }

    public void setSequence(boolean sequence) {
        this.sequence = sequence;
    }

    public boolean getSequence() {
        return sequence;
    }

    public void setPrimary(String primary) {
        if (primary != null) {
            if ("true".equals(primary)) {
                setPrimary(true);
            } else if ("false".equals(primary)) {
                setPrimary(false);
            } else {
                throw new UnsupportedOperationException("Primary '" + primary + "' not allowed; must be 'true' or 'false'");
            }
        }
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
        setPrimary = true;
    }

    public boolean getPrimary() {
        return primary;
    }
    
    public boolean getPrimarySet() {
        return setPrimary;
    }

    public String toString() {
        if (getStep() == null) {
            return "[port " + port + " on null]";
        } else {
            return "[port " + port + " on " + getStep().getName() + "]";
        }
    }        
}
