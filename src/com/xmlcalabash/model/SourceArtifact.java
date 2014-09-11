/*
 * SourceArtifact.java
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

import java.util.Hashtable;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ndw
 */
public abstract class SourceArtifact {
    protected Logger logger = null;
    protected XdmNode node = null;
    protected XProcRuntime runtime = null;
    protected Hashtable<QName,String> extnAttrs = null;

    /** Creates a new instance of SourceArtifact */
    public SourceArtifact(XProcRuntime runtime, XdmNode node) {
        logger = LoggerFactory.getLogger(this.getClass());
        this.runtime = runtime;
        this.node = node;
    }

    public XProcRuntime getXProc() {
        return runtime;
    }

    public XdmNode getNode() {
        return node;
    }

    public String xplFile() {
        if (node == null) {
            return "";
        } else if (node.getDocumentURI() == null) {
            return "";
        } else {
            return node.getDocumentURI().toASCIIString();
        }
    }

    public int xplLine() {
        if (node == null) {
            return -1;
        } else {
            return node.getLineNumber();
        }
    }

    public void addExtensionAttribute(XdmNode attr) {
        if (extnAttrs == null) {
            extnAttrs = new Hashtable<QName,String> ();
        }
        extnAttrs.put(attr.getNodeName(),attr.getStringValue());
    }

    public String getExtensionAttribute(QName name) {
        if (extnAttrs == null || !extnAttrs.containsKey(name)) {
            return null;
        }

        return extnAttrs.get(name);
    }

    public Set<QName> getExtensionAttributes() {
        if (extnAttrs == null) {
            extnAttrs = new Hashtable<QName,String> ();
        }
        return extnAttrs.keySet();
    }

    public void error(String message, QName code) {
        runtime.error(null, node, message, code);
    }

    public void error(XdmNode node, String message, QName code) {
        runtime.error(null, node, message, code);
    }
}
