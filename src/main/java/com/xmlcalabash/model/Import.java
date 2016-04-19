/*
 * Import.java
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

import java.net.URI;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

/**
 *
 * @author ndw
 */
public class Import extends Step {
    URI href = null;
    PipelineLibrary library = null;
    XdmNode root = null;

    /* Creates a new instance of Import */
    public Import(XProcRuntime xproc, XdmNode node) {
        super(xproc, node, XProcConstants.p_import);
        //String x = node.getAttributeValue(new QName("", "href"));
        //System.err.println(x);
    }

    public void setHref(URI href) {
        this.href = href;
    }

    public URI getHref() {
        return href;
    }

    public void setRoot(XdmNode root) {
        this.root = root;
    }

    public XdmNode getRoot() {
        return root;
    }

    public void setLibrary(PipelineLibrary library) {
        this.library = library;
    }
}
