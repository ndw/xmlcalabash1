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

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
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

/**
 *
 * @author ndw
 */
public class AddAttribute extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("", "match");
    private static final QName _attribute_name = new QName("", "attribute-name");
    private static final QName _attribute_value = new QName("", "attribute-value");
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

        attrName = getOption(_attribute_name).getQName();
        attrValue = getOption(_attribute_value).getString();

        if ("xmlns".equals(attrName.getLocalName())) {
            // The xmlns:x case can't occur...
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
        boolean found = false;

        matcher.addStartElement(node);

        XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            String value =  child.getStringValue();
            if (child.getNodeName().equals(attrName)) {
                value = attrValue;
                found = true;
            }
            matcher.addAttribute(child, value);
        }
        if (!found) {
            matcher.addAttribute(attrName,attrValue);
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
