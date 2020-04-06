/*
 * MakeAbsoluteURIs.java
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.MessageFormatter;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.URIUtils;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;
import net.sf.saxon.type.BuiltInAtomicType;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:make-absolute-uris",
        type = "{http://www.w3.org/ns/xproc}make-absolute-uris")

public class MakeAbsoluteURIs extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("", "match");
    private static final QName _base_uri = new QName("", "base-uri");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private URI setBaseURI = null;

    /* Creates a new instance of MakeAbsoluteURIs */
    public MakeAbsoluteURIs(XProcRuntime runtime, XAtomicStep step) {
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

        XdmNode doc = source.read();
        RuntimeValue base = getOption(_base_uri);
        if (base != null) {
            setBaseURI = base.getBaseURI().resolve(base.getString());
        }

        logger.trace(MessageFormatter.nodeMessage(doc, "Making URIs absolute against: " + setBaseURI));

        matcher = new ProcessMatch(runtime, this);
        matcher.match(doc, getOption(_match));

        if (source.moreDocuments()) {
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }

        result.write(matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) {
        return true;
    }

    public void processEndDocument(XdmNode node) {
        // nop
    }

    @Override
    public AttributeMap processAttributes(XdmNode node, AttributeMap matchingAttributes, AttributeMap nonMatchingAttributes) {
        ArrayList<AttributeInfo> alist = new ArrayList<>();
        for (AttributeInfo attr : nonMatchingAttributes) {
            alist.add(attr);
        }

        for (AttributeInfo attr : matchingAttributes) {
            String value = attr.getValue();
            value = URIUtils.encode(value);
            URI baseURI = setBaseURI == null ? node.getBaseURI() : setBaseURI;
            String resolved = baseURI.resolve(value).toString();
            alist.add(new AttributeInfo(attr.getNodeName(), BuiltInAtomicType.ANY_ATOMIC, resolved, attr.getLocation(), ReceiverOption.NONE));
        }

        return AttributeMap.fromList(alist);
    }

    @Override
    public boolean processStartElement(XdmNode node, AttributeMap attributes) {
        matcher.addStartElement(node, attributes);

        String value = node.getStringValue();
        value = URIUtils.encode(value);

        URI baseURI = setBaseURI == null ? node.getBaseURI() : setBaseURI;

        String resolved = baseURI.resolve(value).toString();
        matcher.addText(resolved);
        return false;
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
