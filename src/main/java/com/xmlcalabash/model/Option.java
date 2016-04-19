/*
 * Option.java
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
public class Option extends EndPoint implements ComputableValue {
    private QName name = null;
    private boolean required = false;
    private String select = null;
    private String type = null;
    private XdmNode typeNode = null;
    private Vector<NamespaceBinding> nsBindings = new Vector<NamespaceBinding>();

    /* Creates a new instance of Option */
    public Option(XProcRuntime xproc, XdmNode node) {
        super(xproc,node);
    }

    public void setName(QName name) {
        this.name = name;
    }
    
    public QName getName() {
        return name;
    }

    public void setType(String type, XdmNode node) {
        this.type = type;
        typeNode = node;
    }

    public String getType() {
        return type;
    }

    public QName getTypeAsQName() {
        return new QName(type,typeNode);
    }

    public void setRequired(String required) {
        this.required = "true".equals(required);
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean getRequired() {
        return required;
    }

    public void setSelect(String select) {
        this.select = select;
    }
    
    public String getSelect() {
        return select;
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
            error("Option can have at most one binding.", XProcConstants.dynamicError(8));
            valid = false;
        }

        if (required && (select != null)) {
            error("You can't specify a default value on a required option", XProcConstants.staticError(17));
        }
        
        return valid;
    }

    public String toString() {
        if (XProcConstants.p_option.equals(node.getNodeName())) {
            return "with-option " + name;
        } else {
            return "option " + name;
        }
    }

    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        if (select != null) {
            logger.trace(indent + "option " + getName() + " select=" + select);
        } else {
            logger.trace(indent + "option " + getName());
            if (getBinding().size() == 0) {
                if (XProcConstants.p_option.equals(node.getNodeName())) {
                    // System.err.println(indent + "  no binding allowed");
                } else {
                    logger.trace(indent + "  no binding");
                }
            }
        }
        for (Binding binding : getBinding()) {
            binding.dump(depth+2);
        }
    }
}
