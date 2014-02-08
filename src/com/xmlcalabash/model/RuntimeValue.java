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

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.ItemTypeFactory;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.value.StringValue;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;


/**
 *
 * @author ndw
 */
public class RuntimeValue {
    private Vector<XdmItem> generalValue = null;
    private String value = null;
    private XdmNode node = null;
    private boolean initialized = false;
    private Hashtable<String,String> nsBindings = null;

    public RuntimeValue() {
        // nop; returns an uninitialized value
    }

    public RuntimeValue(String value, XdmNode node) {
        this.value = value;
        this.node = node;
        initialized = true;

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
        initialized = true;
    }

    public RuntimeValue(String value, Vector<XdmItem> generalValue, XdmNode node, Hashtable<String,String> nsBindings) {
        this.value = value;
        this.generalValue = generalValue;
        this.node = node;
        this.nsBindings = nsBindings;
        initialized = true;
    }

    public RuntimeValue(String value) {
        this.value = value;
        initialized = true;
    }

    /*
    public void setComputableValue(ComputableValue value) {
        val = value;
        initialized = true;
    }
    */

    public boolean initialized() {
        return initialized;
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

    public boolean hasGeneralValue() {
        return generalValue != null;
    }

    public XdmValue getValue() {
        if (generalValue == null) {
            // Turn the string value into an XdmValue
            return new XdmAtomicValue(value);
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

    public XdmNode getNode() {
        return node;
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
            throw new XProcException(node, "Non boolean string: " + value);
        }
    }

    public int getInt() {
        int result = Integer.parseInt(value);
        return result;
    }

    public long getLong() {
        long result = Long.parseLong(value);
        return result;
    }

    public XdmSequenceIterator getNamespaces() {
        return node.axisIterator(Axis.NAMESPACE);
    }
}
