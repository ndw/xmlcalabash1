/*
 * Environment.java
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

import java.util.Vector;

/**
 *
 * @author ndw
 */
public class Environment {
    private Vector<Step> visibleSteps = new Vector<Step> ();
    private Port defaultReadablePort = null;
    private Step pipeline = null;
    private Environment parent = null;

    // ignored namespaces are only used at parse time
    
    /* Creates a new instance of Environment */
    public Environment(Step pipeline) {
        this.pipeline = pipeline;
        visibleSteps.add(pipeline);
    }
    
    public Environment(Environment env) {
        parent = env;
        pipeline = env.pipeline;
        defaultReadablePort = env.defaultReadablePort;
    }

    public Environment getParent() {
        return parent;
    }

    public void addStep(Step step) {
        visibleSteps.add(step);
    }
    
    public void setDefaultReadablePort(Port port) {
        defaultReadablePort = port;
    }
    
    public Port getDefaultReadablePort() {
        return defaultReadablePort;
    }
    
    public int countVisibleSteps(String stepName) {
        int count = 0;
        for (Step step : visibleSteps) {
            if (step.getName().equals(stepName)) {
                count++;
            }
        }
        
        if (parent != null) {
            count += parent.countVisibleSteps(stepName);
        }
        
        return count;
    }
    
    public Step visibleStep(String stepName) {
        for (Step step : visibleSteps) {
            if (step.getName().equals(stepName)) {
                return step;
            }
        }
        
        if (parent != null) {
            return parent.visibleStep(stepName);
        }
        
        return null;
    }    
    
    public Output readablePort(String stepName, String portName) {
        Step step = visibleStep(stepName);
        if (step != null) {
            return step.getOutput(portName);
        }
        return null;
    }
}
