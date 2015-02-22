/*
 * Binding.java
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
public abstract class Binding extends SourceArtifact {
    public static final int NO_BINDING = 0;
    public static final int PIPE_NAME_BINDING = 1;
    public static final int INLINE_BINDING = 2;
    public static final int DOCUMENT_BINDING = 3;
    public static final int PIPE_BINDING = 4;
    public static final int EMPTY_BINDING = 5;
    public static final int STDIO_BINDING = 6;
    public static final int ERROR_BINDING = 7;
    public static final int DATA_BINDING = 8;

    protected int bindingType = NO_BINDING;

    /** Creates a new instance of Binding */
    public Binding(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }

    public int getBindingType() {
        return bindingType;
    }
    
    abstract protected void dump(int depth);
}
