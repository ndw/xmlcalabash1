/*
 * Parameters.java
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

package com.xmlcalabash.library;

import java.util.Hashtable;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.io.WritablePipe;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:parameters",
        type = "{http://www.w3.org/ns/xproc}parameters")

public class Parameters extends DefaultStep {
    private static final QName c_param_set = new QName("c", XProcConstants.NS_XPROC_STEP, "param-set");
    private static final QName c_param = new QName("c", XProcConstants.NS_XPROC_STEP, "param");
    private static final QName cx_item = new QName("cx", XProcConstants.NS_CALABASH_EX, "item");
    private static final QName _name = new QName("name");
    private static final QName _namespace = new QName("namespace");
    private static final QName _value = new QName("value");
    private static final QName _type = new QName("type");
    private WritablePipe result = null;
    Hashtable<QName,RuntimeValue> parameters = new Hashtable<QName,RuntimeValue> ();

    /** Creates a new instance of Count */
    public Parameters(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(String port, QName name, RuntimeValue value) {
        parameters.put(name, value);
    }

    public void setParameter(QName name, RuntimeValue value) {
        parameters.put(name, value);
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        TreeWriter treeWriter = new TreeWriter(runtime);
        treeWriter.startDocument(step.getNode().getBaseURI());
        treeWriter.addStartElement(c_param_set);
        treeWriter.startContent();

        for (QName param : parameters.keySet()) {
            String value = parameters.get(param).getStringValue().getStringValue();
            treeWriter.addStartElement(c_param);
            treeWriter.addAttribute(_name, param.getLocalName());
            if (param.getNamespaceURI() != null && !"".equals(param.getNamespaceURI())) {
                treeWriter.addAttribute(_namespace, param.getNamespaceURI());
            } else {
                // I'm not really sure about this...
                treeWriter.addAttribute(_namespace, "");
            }

            if (runtime.getAllowGeneralExpressions()) {
                XdmValue xdmvalue = parameters.get(param).getValue();
                XdmAtomicValue atom = null;
                if (xdmvalue.size() == 1) {
                    XdmItem item = xdmvalue.itemAt(0);
                    if (item.isAtomicValue()) {
                        atom = (XdmAtomicValue) item;
                    }
                }

                if (atom != null && xdmvalue.size() == 1) {
                    treeWriter.addAttribute(_value, value);
                    treeWriter.startContent();
                } else {
                    treeWriter.startContent();
                    XdmSequenceIterator iter = xdmvalue.iterator();
                    while (iter.hasNext()) {
                        XdmItem next = iter.next();
                        QName type = next.isAtomicValue() ? ((XdmAtomicValue) next).getPrimitiveTypeName() : null;

                        treeWriter.addStartElement(cx_item);

                        if (type != null) {
                            if ("http://www.w3.org/2001/XMLSchema".equals(type.getNamespaceURI())) {
                                treeWriter.addAttribute(_type, type.getLocalName());
                            } else {
                                treeWriter.addAttribute(_type, type.getClarkName());
                            }
                        }

                        if (next.isAtomicValue()) {
                            treeWriter.addAttribute(_value, next.getStringValue());
                            treeWriter.startContent();
                        } else {
                            treeWriter.startContent();
                            treeWriter.addSubtree((XdmNode) next);
                        }
                        
                        treeWriter.addEndElement();
                    }

                }
            } else {
                treeWriter.addAttribute(_value, value);
                treeWriter.startContent();
            }
            treeWriter.addEndElement();
        }

        treeWriter.addEndElement();
        treeWriter.endDocument();

        result.write(treeWriter.getResult());
    }
}

