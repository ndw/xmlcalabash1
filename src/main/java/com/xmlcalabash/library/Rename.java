/*
 * Rename.java
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
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TypeUtils;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NamespaceMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:rename",
        type = "{http://www.w3.org/ns/xproc}rename")

public class Rename extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("", "match");
    private static final QName _new_name = new QName("", "new-name");
    private static final QName _new_prefix = new QName("", "new-prefix");
    private static final QName _new_namespace = new QName("", "new-namespace");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Map<QName, RuntimeValue> inScopeOptions = null;
    private ProcessMatch matcher = null;
    private QName newName = null;

    /* Creates a new instance of Rename */
    public Rename(XProcRuntime runtime, XAtomicStep step) {
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

        RuntimeValue nameValue = getOption(_new_name);
        String nameStr = nameValue.getString();
        String npfx = getOption(_new_prefix, (String) null);
        String nns = getOption(_new_namespace, (String) null);

        if (npfx != null && nns == null) {
            throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace");
        }

        if (nns != null && nameStr.contains(":")) {
            throw XProcException.dynamicError(34, "You can't specify a namespace if the new-name contains a colon");
        }

        if (nameStr.contains(":")) {
            newName = new QName(nameStr, nameValue.getNode());
        } else {
            newName = new QName(npfx == null ? "" : npfx, nns, nameStr);
        }

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(), getOption(_match));

        if (source.moreDocuments()) {
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }

        result.write(matcher.getResult());
    }

    @Override
    public boolean processStartDocument(XdmNode node) {
        return true;
    }

    @Override
    public void processEndDocument(XdmNode node) {
        // nop
    }

    @Override
    public AttributeMap processAttributes(XdmNode node, AttributeMap matchingAttributes, AttributeMap nonMatchingAttributes) {
        ArrayList<AttributeInfo> alist = new ArrayList<>();
        for (AttributeInfo attr : nonMatchingAttributes) {
            alist.add(attr);
        }

        if (matchingAttributes.size() > 1) {
            throw new XProcException("Cannot rename multiple attributes to the same name.");
        }

        // Make sure we construct a unique prefix/URI mapping for the attribute
        // if the prefix is already in the namespace map with a different URI.
        NamespaceMap nsmap = node.getUnderlyingNode().getAllNamespaces();
        for (AttributeInfo attr : matchingAttributes) {
            String prefix = newName.getPrefix();
            String uri = newName.getNamespaceURI();
            String localName = newName.getLocalName();

            if (uri == null || "".equals(uri)) {
                FingerprintedQName fqName = new FingerprintedQName("", "", localName);
                AttributeInfo ainfo = new AttributeInfo(fqName, attr.getType(), attr.getValue(), attr.getLocation(), attr.getProperties());
                alist.add(ainfo);
            } else {
                if (prefix == null || "".equals(prefix)) {
                    prefix = "_";
                }

                int count = 1;
                String checkPrefix = prefix;
                String nsURI = nsmap.getURI(checkPrefix);
                while (nsURI != null && !nsURI.equals(uri)) {
                    count += 1;
                    checkPrefix = prefix + count;
                    nsURI = nsmap.getURI(checkPrefix);
                }

                prefix = checkPrefix;
                FingerprintedQName fqName = new FingerprintedQName(prefix, uri, localName);
                AttributeInfo ainfo = new AttributeInfo(fqName, attr.getType(), attr.getValue(), attr.getLocation(), attr.getProperties());
                alist.add(ainfo);
            }
        }

        return AttributeMap.fromList(alist);
    }

    @Override
    public boolean processStartElement(XdmNode node, AttributeMap attributes) {
        matcher.addStartElement(node, newName, node.getBaseURI(), attributes);
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
        if (!"".equals(newName.getNamespaceURI())) {
            throw XProcException.stepError(13);
        }
        matcher.addPI(newName.getLocalName(), node.getStringValue());
    }
}

