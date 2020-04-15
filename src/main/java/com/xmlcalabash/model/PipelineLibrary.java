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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author ndw
 */
public class PipelineLibrary extends Step implements DeclarationScope {
    Hashtable<QName,DeclareStep> declaredSteps = new Hashtable<QName,DeclareStep> ();
    Vector<DeclareStep> steps = new Vector<DeclareStep> ();
    private List<PipelineLibrary> importedLibs = new ArrayList<>();

    /* Creates a new instance of PipelineLibrary */
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
        declareStep(type, step);
    }

    public QName firstStep() {
        if (steps.size() > 0) {
            return steps.get(0).getDeclaredType();
        } else {
            return null;
        }
    }

    public void declareStep(QName type, DeclareStep step) {
        DeclareStep d = getDeclaration(type);
        if (d != null) {
            if (!d.equals(step))
                throw new XProcException(step, "Duplicate step type: " + type);
        } else {
            declaredSteps.put(type, step);
        }
    }

    public void addImport(PipelineLibrary lib) {
        importedLibs.add(lib);
    }

    private boolean circularImportGuard = false;

    public DeclareStep getDeclaration(QName type) {
        DeclareStep decl = null;
        if (!circularImportGuard) {
            circularImportGuard = true;
            try {
                for (PipelineLibrary lib : importedLibs) {
                    DeclareStep d = lib.getDeclaration(type);
                    if (d != null) {
                        if (decl == null)
                            decl = d;
                        else if (!decl.equals(d))
                            throw new XProcException(d, "Duplicate step type: " + type);
                    }
                }
                {
                    DeclareStep d = declaredSteps.get(type);
                    if (d != null) {
                        if (decl == null)
                            decl = d;
                        else if (!decl.equals(d))
                            throw new XProcException(d, "Duplicate step type: " + type);
                    }
                }
            } finally {
                circularImportGuard = false;
            }
        }
        return decl;
    }

    public Set<QName> getInScopeTypes() {
        Set<QName> decls = new HashSet<>();
        if (!circularImportGuard) {
            circularImportGuard = true;
            try {
                decls.addAll(declaredSteps.keySet());
                for (PipelineLibrary lib : importedLibs)
                    decls.addAll(lib.getInScopeTypes());
            } finally {
                circularImportGuard = false;
            }
        }
        return decls;
    }
}
