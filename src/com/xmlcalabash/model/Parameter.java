/*
 * Parameter.java
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

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ndw
 */
public class Parameter extends EndPoint implements ComputableValue {
    private String port = null;
    private QName name = null;
    private boolean required = false;
    private String select = null;
    protected Vector<Binding> bindings = new Vector<Binding> ();
    private int position = 0;
    private Vector<NamespaceBinding> nsBindings = new Vector<NamespaceBinding> ();

    /** Creates a new instance of Parameter */
    public Parameter(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }
    
    public void setPort(String port) {
        this.port = port;
    }

    public String getPort() {
        return port;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public QName getName() {
        return name;
    }

    public String getType() {
        return null;
    }

    public QName getTypeAsQName() {
        return null;
    }

    public void setPosition(int pos) {
        position = pos;
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setSelect(String select) {
        this.select = select;
    }
    
    public String getSelect() {
        return select;
    }

    public void addBinding(Binding binding) {
        bindings.add(binding);
    }

    public Vector<Binding> getBinding() {
        return bindings;
    }

    public void addNamespaceBinding(NamespaceBinding binding) {
        nsBindings.add(binding);
    }

    public Vector<NamespaceBinding> getNamespaceBindings() {
        return nsBindings;
    }

    public boolean valid(Environment env) {
        boolean valid = true;
        
        if (bindings.size() > 1) {
            error("Parameter can have at most one binding.", XProcConstants.dynamicError(8));
            valid = false;
        }

        return valid;
    }

    public String toString() {
        return "with-param " + name;
    }

    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        logger.trace(indent + "parameter " + getName());
        if (bindings.size() == 0) {
            logger.trace(indent + "  no binding");
        }
        for (Binding binding : getBinding()) {
            binding.dump(depth+2);
        }
    }
    
}
