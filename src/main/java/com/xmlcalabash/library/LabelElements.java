/*
 * LabelElements.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TypeUtils;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NameOfNode;
import net.sf.saxon.om.NamespaceMap;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmAtomicValue;

import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import net.sf.saxon.type.BuiltInAtomicType;
import org.w3c.dom.Attr;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:label-elements",
        type = "{http://www.w3.org/ns/xproc}label-elements")

public class LabelElements extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _attribute = new QName("attribute");
    private static final QName _attribute_prefix = new QName("attribute-prefix");
    private static final QName _attribute_namespace = new QName("attribute-namespace");
    private static final QName _match = new QName("match");
    private static final QName _label = new QName("label");
    private static final QName _replace = new QName("replace");
    private static final QName p_index = new QName("p", XProcConstants.NS_XPROC, "index");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private QName attribute = null;
    private RuntimeValue label = null;
    private boolean replace = true;
    private int count = 1;

    /* Creates a new instance of LabelElements */
    public LabelElements(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    @Override
    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    @Override
    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    @Override
    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    @Override
    public void run() throws SaxonApiException {
        super.run();

        RuntimeValue attrNameValue = getOption(_attribute);
        String attrNameStr = attrNameValue.getString();
        String apfx = getOption(_attribute_prefix, (String) null);
        String ans = getOption(_attribute_namespace, (String) null);

        if (apfx != null && ans == null) {
            throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace");
        }

        if (ans != null && attrNameStr.contains(":")) {
            throw XProcException.dynamicError(34, "You can't specify a namespace if the attribute name contains a colon");
        }

        if (attrNameStr.contains(":")) {
            attribute = new QName(attrNameStr, attrNameValue.getNode());
        } else {
            // For Saxon 9.4, make sure there's some sort of prefix if there's a namespace;
            // Saxon will take care of resolving collisions, if necessary
            if (apfx == null && ans != null) {
                apfx = "_1";
            }
            attribute = new QName(apfx == null ? "" : apfx, ans, attrNameStr);
        }

        label = getOption(_label);
        replace = getOption(_replace).getBoolean();

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(), getOption(_match));

        result.write(matcher.getResult());
    }

    @Override
    public boolean processStartDocument(XdmNode node) {
        throw XProcException.stepError(24);
    }

    @Override
    public void processEndDocument(XdmNode node) {
        throw XProcException.stepError(24);
    }

    @Override
    public AttributeMap processAttributes(XdmNode node, AttributeMap matchingAttributes, AttributeMap nonMatchingAttributes) {
        throw XProcException.stepError(23);
    }

    @Override
    public boolean processStartElement(XdmNode node, AttributeMap attributes) {
        NamespaceMap nsmap = node.getUnderlyingNode().getAllNamespaces();
        AttributeMap amap = EmptyAttributeMap.getInstance();

        String prefix = prefixFor(nsmap, attribute.getPrefix(), attribute.getNamespaceURI());
        NodeName aname = new FingerprintedQName(prefix, attribute.getNamespaceURI(), attribute.getLocalName());

        boolean found = false;
        for (AttributeInfo ainfo : attributes) {
            if (aname.equals(ainfo.getNodeName())) {
                found = true;
                if (replace) {
                    amap = amap.put(new AttributeInfo(aname, BuiltInAtomicType.UNTYPED_ATOMIC, computedLabel(node), ainfo.getLocation(), ReceiverOption.NONE));
                } else {
                    amap = amap.put(ainfo);
                }
            } else {
                amap = amap.put(ainfo);
            }
        }

        if (!found) {
            amap = amap.put(new AttributeInfo(aname, BuiltInAtomicType.UNTYPED_ATOMIC, computedLabel(node), null, ReceiverOption.NONE));
        }

        matcher.addStartElement(node, amap);
        return true;
    }

    @Override
    public void processEndElement(XdmNode node) {
        matcher.addEndElement();
    }

    @Override
    public void processText(XdmNode node) {
        throw XProcException.stepError(23);
    }

    @Override
    public void processComment(XdmNode node) {
        throw XProcException.stepError(23);
    }

    @Override
    public void processPI(XdmNode node) {
        throw XProcException.stepError(23);
    }

    private String computedLabel(XdmNode node) {
        try {
            XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
            xcomp.setBaseURI(step.getNode().getBaseURI());

            // Make sure any namespace bindings in-scope for the label are available for the expression
            for (String prefix : label.getNamespaceBindings().keySet()) {
                xcomp.declareNamespace(prefix, label.getNamespaceBindings().get(prefix));
            }
            xcomp.declareVariable(p_index);

            XPathExecutable xexec = xcomp.compile(label.getString());
            XPathSelector selector = xexec.load();

            selector.setVariable(p_index,new XdmAtomicValue(count++));

            selector.setContextItem(node);

            Iterator<XdmItem> values = selector.iterator();

            XdmItem item = values.next();

            return item.getStringValue();
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
    }
}

