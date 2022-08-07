/*
 * SetAttributes.java
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
import java.util.Map;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
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

@XMLCalabash(
        name = "p:set-attributes",
        type = "{http://www.w3.org/ns/xproc}set-attributes")

public class SetAttributes extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("match");
    private ReadablePipe attributes = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Map<QName, RuntimeValue> inScopeOptions = null;
    private ProcessMatch matcher = null;
    private XdmNode root = null;
    private AttributeMap attrs = null;

    /* Creates a new instance of SetAttributes */
    public SetAttributes(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            attributes = pipe;
        }
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

        root = S9apiUtils.getDocumentElement(attributes.read());
        assert root != null;
        attrs = root.getUnderlyingNode().attributes();

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(), getOption(_match));

        if (source.moreDocuments()) {
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }

        result.write(matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) {
        throw XProcException.stepError(23);
    }

    public void processEndDocument(XdmNode node) {
        // nop
    }

    @Override
    public AttributeMap processAttributes(XdmNode node, AttributeMap matchingAttributes, AttributeMap nonMatchingAttributes) {
        throw XProcException.stepError(23);
    }

    @Override
    public boolean processStartElement(XdmNode node, AttributeMap attributes) {
        ArrayList<AttributeInfo> alist = new ArrayList<>();
        for (AttributeInfo ainfo : attrs) {
            alist.add(ainfo);
        }
        for (AttributeInfo ainfo : attributes) {
            if (attrs.get(ainfo.getNodeName()) == null) {
                alist.add(ainfo);
            }
        }

        matcher.addStartElement(node, S9apiUtils.mapFromList(alist));
        return true;
    }

    public void processEndElement(XdmNode node) {
        matcher.addEndElement();
    }

    public void processText(XdmNode node) {
        throw XProcException.stepError(23);
    }

    public void processComment(XdmNode node) {
        throw XProcException.stepError(23);
    }

    public void processPI(XdmNode node) {
        throw XProcException.stepError(23);
    }
}

