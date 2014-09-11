/*
 * DocumentBinding.java
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
public class DocumentBinding extends Binding {
    private String href = null;
    
    /** Creates a new instance of DocumentBinding */
    public DocumentBinding() {
        super(null, null);
        bindingType = DOCUMENT_BINDING;
    }

    public DocumentBinding(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
        bindingType = DOCUMENT_BINDING;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        logger.trace(indent + "URI binding to " + getHref());
    }
}
