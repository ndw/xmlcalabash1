/*
 * Log.java
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
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class Log extends SourceArtifact {
    private String port = null;
    private URI href = null;
    
    /* Creates a new instance of Journal */
    public Log(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }

    public void setPort(String port) {
        this.port = port;
    }
    
    public String getPort() {
        return port;
    }
    
    public void setHref(URI href) {
        this.href = href;
    }
    
    public URI getHref() {
        return href;
    }

    public String toString() {
        String result = "log for \"" + port + "\"";
        if (href != null) {
            result += " to \"" + href + "\"";
        }
        if (node.getLineNumber() > 0) {
            result += " at " + node.getDocumentURI() + ":" + node.getLineNumber();
        } else {
            result += " in " + node.getDocumentURI();
        }
        return result;
    }
}
