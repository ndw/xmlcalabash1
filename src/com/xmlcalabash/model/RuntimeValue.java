/*
 * RuntimeValue.java
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
import java.util.Hashtable;
import java.util.Vector;
import java.util.Iterator;

import net.sf.saxon.s9api.*;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.trans.XPathException;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.S9apiUtils;

/**
 *
 * @author ndw
 */
public class RuntimeValue {
    private Vector<XdmItem> generalValue = null;
    private String value = null;
    private XdmNode node = null;
    private ComputableValue val = null;
    private Hashtable<String,String> nsBindings = null;
    
    public RuntimeValue(String value, XdmNode node) {
        this.value = value;
        this.node = node;

        nsBindings = new Hashtable<String,String> ();
        XdmSequenceIterator nsIter = node.axisIterator(Axis.NAMESPACE);
        while (nsIter.hasNext()) {
            XdmNode ns = (XdmNode) nsIter.next();
            QName nodeName = ns.getNodeName();
            String uri = ns.getStringValue();

            if (nodeName == null) {
                // Huh?
                nsBindings.put("", uri);
            } else {
                String localName = nodeName.getLocalName();
                nsBindings.put(localName,uri);
            }
        }
    }

    public RuntimeValue(String value, XdmNode node, Hashtable<String,String> nsBindings) {
        this.value = value;
        this.node = node;
        this.nsBindings = nsBindings;
    }

    public RuntimeValue(String value, Vector<XdmItem> generalValue, XdmNode node, Hashtable<String,String> nsBindings) {
        this.value = value;
        this.generalValue = generalValue;
        this.node = node;
        this.nsBindings = nsBindings;
    }

    public RuntimeValue(String value) {
        this.value = value;
    }

    public void setComputableValue(ComputableValue value) {
        val = value;
    }

    public XdmAtomicValue getUntypedAtomic(XProcRuntime runtime) {
        try {
            ItemTypeFactory itf = new ItemTypeFactory(runtime.getProcessor());
            ItemType untypedAtomic = itf.getAtomicType(new QName(NamespaceConstant.SCHEMA, "xs:untypedAtomic"));
            XdmAtomicValue val = new XdmAtomicValue(value, untypedAtomic);
            return val;
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
    }

    public String getString() {
        return value;
    }

    public XdmValue getValue() {
        if (generalValue == null) {
            throw new XProcException("Unexpexted null value in getValue()");
        }
        if (generalValue.size() == 1) {
            return generalValue.get(0);
        } else {
            return new XdmValue(generalValue);
        }
    }

    public StringValue getStringValue() {
        return new StringValue(value);
    }

    public QName getQName() {
        // FIXME: Check the type
        // TypeUtils.checkType(runtime, value, )
        if (value.contains(":")) {
            return new QName(value, node);
        } else {
            return new QName("", value);
        }
    }

    public URI getBaseURI() {
        return node.getBaseURI();
    }

    public Hashtable<String,String> getNamespaceBindings() {
        return nsBindings;
    }

    public boolean getBoolean() {
        if ("true".equals(value) || "1".equals(value)) {
            return true;
        } else if ("false".equals(value) || "0".equals(value)) {
            return false;
        } else {
            throw new XProcException("Non boolean string: " + value);
        }
    }

    public int getInt() {
        int result = Integer.parseInt(value);
        return result;
    }

    public XdmSequenceIterator getNamespaces() {
        return node.axisIterator(Axis.NAMESPACE);
    }
}
