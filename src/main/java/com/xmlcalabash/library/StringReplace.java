/*
 * StringReplace.java
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

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.*;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.type.BuiltInAtomicType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:string-replace",
        type = "{http://www.w3.org/ns/xproc}string-replace")

public class StringReplace extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("", "match");
    private static final QName _replace = new QName("", "replace");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private RuntimeValue replace = null;
    private final HashMap<String,NamespaceUri> rns = new HashMap<>();
    private static final HashMap<QName,RuntimeValue> atomicStepsGetNoInScopeOptions = new HashMap<>();

    /* Creates a new instance of StringReplace */
    public StringReplace(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }


    public void run() throws SaxonApiException {
        super.run();

        RuntimeValue match = getOption(_match);
        replace = getOption(_replace);
        for (String prefix : replace.getNamespaceBindings().keySet()) {
            rns.put(prefix, replace.getNamespaceBindings().get(prefix));
        }

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(), match);

        result.write(matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) {
        return true;
    }

    public void processEndDocument(XdmNode node) {
        // nop?
    }

    @Override
    public AttributeMap processAttributes(XdmNode node, AttributeMap matchingAttributes, AttributeMap nonMatchingAttributes) {
        ArrayList<AttributeInfo> alist = new ArrayList<>();
        for (AttributeInfo attr : nonMatchingAttributes) {
            alist.add(attr);
        }

        for (AttributeInfo attr : matchingAttributes) {
            // This is kind of ugly; I need the XdmNode for the attribute
            XdmNode attrNode = null;
            for (XdmNode anode : new AxisNodes(node, Axis.ATTRIBUTE)) {
                NodeName aname = TypeUtils.fqName(anode.getNodeName());
                if (aname.equals(attr.getNodeName())) {
                    attrNode = anode;
                }
            }
            alist.add(new AttributeInfo(attr.getNodeName(), BuiltInAtomicType.UNTYPED_ATOMIC, computeReplacement(attrNode), attr.getLocation(), ReceiverOption.NONE));
        }

        return S9apiUtils.mapFromList(alist);
    }

    @Override
    public boolean processStartElement(XdmNode node, AttributeMap attributes) {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
        return false;
    }

    public void processEndElement(XdmNode node) {
        // nop?
    }

    public void processText(XdmNode node) {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
    }

    public void processComment(XdmNode node) {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
    }

    public void processPI(XdmNode node) {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
    }

    private String computeReplacement(XdmNode node) {
        Vector<XdmItem> values = evaluateXPath(node, rns, replace.getString(), atomicStepsGetNoInScopeOptions);
        StringBuilder newValue = new StringBuilder();
        for (XdmItem item : values) {
            newValue.append(item.getStringValue());
        }
        return newValue.toString();
    }

}

