/*
 * EndPoint.java
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
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class EndPoint extends SourceArtifact {
    protected Step step = null;
    protected Vector<Binding> bindings = new Vector<Binding> ();
    
    /* Creates a new instance of EndPoint */
    public EndPoint(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }

    public void setStep(Step step) {
        this.step = step;
    }
    
    public Step getStep() {
        return step;
    }

    public void addBinding(Binding binding) {
        bindings.add(binding);
    }

    public void clearBindings() {
        bindings = new Vector<Binding> ();
    }

    public Vector<Binding> getBinding() {
        return bindings;
    }

    public PipeNameBinding findPipeBinding(String stepName, String portName) {
        for (Binding binding : getBinding()) {
            if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                PipeNameBinding pipe = (PipeNameBinding) binding;
                if (pipe.getStep().equals(stepName) && pipe.getPort().equals(portName)) {
                    return pipe;
                }
            }
        }
        
        return null;
    }
}
