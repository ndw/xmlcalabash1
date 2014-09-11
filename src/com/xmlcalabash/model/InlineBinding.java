/*
 * InlineBinding.java
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
import java.util.HashSet;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import com.xmlcalabash.core.XProcRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ndw
 */
public class InlineBinding extends Binding {
    XdmNode root = null;
    Vector<XdmValue> nodes = null;
    HashSet<String> excludeNS = null;
    
    /** Creates a new instance of InlineBinding */
    public InlineBinding(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
        bindingType = INLINE_BINDING;
        nodes = new Vector<XdmValue> ();
    }

    public void addNode(XdmNode node) {
        nodes.add(node);
    }

    public void excludeNamespaces(HashSet<String> exclude) {
        excludeNS = exclude;
    }

    public HashSet<String> getExcludedNamespaces() {
        return excludeNS;
    }

    public Vector<XdmValue> nodes() {
        return nodes;
    }

    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        logger.trace(indent + "inline binding");
    }
}
