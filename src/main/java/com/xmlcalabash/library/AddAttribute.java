/*
 * AddAttribute.java
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
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;
import com.xmlcalabash.runtime.XAtomicStep;

import javax.xml.XMLConstants;
import java.util.Hashtable;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:add-attribute",
        type = "{http://www.w3.org/ns/xproc}add-attribute")

public class AddAttribute extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("", "match");
    private static final QName _attribute_name = new QName("", "attribute-name");
    private static final QName _attribute_value = new QName("", "attribute-value");
    private static final QName _attribute_prefix = new QName("", "attribute-prefix");
    private static final QName _attribute_namespace = new QName("", "attribute-namespace");
    private QName attrName = null;
    private String attrValue = null;
    private ProcessMatch matcher = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of AddAttribute
     */
    public AddAttribute(XProcRuntime runtime, XAtomicStep step) {
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

        RuntimeValue attrNameValue = getOption(_attribute_name);
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
            attrName = new QName(attrNameStr, attrNameValue.getNode());
        } else {
            attrName = new QName(apfx == null ? "" : apfx, ans, attrNameStr);
        }

        attrValue = getOption(_attribute_value).getString();

        if ("xmlns".equals(attrName.getLocalName())
                || "xmlns".equals(attrName.getPrefix())
                || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attrName.getNamespaceURI())
                || (!"xml".equals(attrName.getPrefix())
                        && XMLConstants.XML_NS_URI.equals(attrName.getNamespaceURI()))
                || ("xml".equals(attrName.getPrefix())
                        && !XMLConstants.XML_NS_URI.equals(attrName.getNamespaceURI()))) {
            throw XProcException.stepError(59);
        }

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(), getOption(_match));

        result.write(matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(13);
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(13);
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        // We're going to have to loop through the attributes several times, so let's grab 'em
        Hashtable<QName, String> attrs = new Hashtable<QName, String> ();

        XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            String value =  child.getStringValue();

            if (!child.getNodeName().equals(attrName)) {
                attrs.put(child.getNodeName(), value);
            }
        }

        QName instanceAttrName = attrName;

        if (attrName.getNamespaceURI() != null && !"".equals(attrName.getNamespaceURI())) {
            // If the requested prefix is already bound to something else, drop it
            String prefix = attrName.getPrefix();
            for (QName attr : attrs.keySet()) {
                if (prefix.equals(attr.getPrefix())
                        && !attrName.getNamespaceURI().equals(attr.getNamespaceURI())) {
                    prefix = "";
                }
            }

            // If there isn't a prefix, we have to make one up
            if ("".equals(prefix)) {
                int acount = 0;
                String aprefix = "_0";
                boolean done = false;

                while (!done) {
                    acount++;
                    aprefix = "_" + acount;
                    done = true;

                    for (QName attr : attrs.keySet()) {
                        if (aprefix.equals(attr.getPrefix())) {
                            done = false;
                        }
                    }
                }

                instanceAttrName = new QName(aprefix, attrName.getNamespaceURI(), attrName.getLocalName());
            }
        }

        // Now put the "new" one in, with it's instance-valid QName
        attrs.put(instanceAttrName, attrValue);

        matcher.addStartElement(node);

        for (QName attr : attrs.keySet()) {
            matcher.addAttribute(attr, attrs.get(attr));
        }

        return true;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
    }

    public void processText(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }
}
