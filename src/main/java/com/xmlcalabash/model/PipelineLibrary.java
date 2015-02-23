/*
 * PipelineLibrary.java
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
import com.xmlcalabash.core.XProcException;

import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author ndw
 */
public class PipelineLibrary extends Step {
    Hashtable<QName,DeclareStep> declaredSteps = new Hashtable<QName,DeclareStep> ();
    Vector<DeclareStep> steps = new Vector<DeclareStep> ();

    /** Creates a new instance of PipelineLibrary */
    public PipelineLibrary(XProcRuntime xproc, XdmNode node) {
        super(xproc, node, XProcConstants.p_library);
    }

    public void addStep(DeclareStep step) {
        QName type = step.getDeclaredType();
        if (type == null) {
            // It can't be called so it doesn't really matter...
            return;
        }

        if (declaredSteps.contains(type)) {
            throw new XProcException(step.getNode(), "You aren't allowed to do this");
        }

        steps.add(step);
        declaredSteps.put(type, step);
    }

    public QName firstStep() {
        if (steps.size() > 0) {
            return steps.get(0).getDeclaredType();
        } else {
            return null;
        }
    }

    public Set<QName> declaredTypes() {
        return declaredSteps.keySet();
    }

    public DeclareStep getDeclaration(QName type) {
        if (declaredSteps.containsKey(type)) {
            return declaredSteps.get(type);
        } else {
            return null;
        }
    }
}
