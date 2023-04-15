/*
 * Error.java
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
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.util.TypeUtils;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.om.NamespaceMap;
import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;

import java.util.HashMap;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:error",
        type = "{http://www.w3.org/ns/xproc}error")

public class Error extends DefaultStep {
    private static final QName c_error = XProcConstants.qNameFor(XProcConstants.NS_XPROC_STEP, "error");
    private static final QName _name = new QName("name");
    private static final QName _code = new QName("code");
    private static final QName _code_prefix = new QName("code-prefix");
    private static final QName _code_namespace = new QName("code-namespace");
    private static final QName _type = new QName("type");
    private ReadablePipe source = null;

    /* Creates a new instance of Delete */
    public Error(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        // p:error always throws an exception, so who cares.
    }

    public void reset() {
        source.resetReader();
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode doc = source.read();
        if (doc == null) {
            logger.trace("Error step read empty");
        } else {
            logger.trace("Error step read " + doc.getDocumentURI());
        }

        RuntimeValue codeNameValue = getOption(_code);
        String codeNameStr = codeNameValue.getString();
        String cpfx = getOption(_code_prefix, (String) null);
        NamespaceUri cns = NamespaceUri.of(getOption(_code_namespace, (String) null));

        if (cpfx == null && cns != NamespaceUri.NULL) {
            cpfx = "ERR";
        }

        if (cpfx != null && cns == NamespaceUri.NULL) {
            throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace");
        }

        if (cns != NamespaceUri.NULL && codeNameStr.contains(":")) {
            throw XProcException.dynamicError(34, "You can't specify a namespace if the code name contains a colon");
        }

        QName errorCode = null;
        if (codeNameStr.contains(":")) {
            errorCode = new QName(codeNameStr, codeNameValue.getNode());
        } else {
            errorCode = new QName(cpfx == null ? "" : cpfx, cns.toString(), codeNameStr);
        }

        cpfx = errorCode.getPrefix();
        cns = errorCode.getNamespaceUri();

        TreeWriter treeWriter = new TreeWriter(runtime);
        treeWriter.startDocument(step.getNode().getBaseURI());

        AttributeMap attr = EmptyAttributeMap.getInstance();
        NamespaceMap nsmap = NamespaceMap.emptyMap();

        nsmap = nsmap.put(cpfx, cns);

        attr = attr.put(TypeUtils.attributeInfo(_name, step.getName()));
        attr = attr.put(TypeUtils.attributeInfo(_type, "p:error"));
        attr = attr.put(TypeUtils.attributeInfo(_code, errorCode.toString()));

        treeWriter.addStartElement(c_error, attr, nsmap);
        if (doc != null) {
            treeWriter.addSubtree(doc);
        }
        treeWriter.addEndElement();
        treeWriter.endDocument();

        step.reportError(treeWriter.getResult());

        if (doc == null) {
            throw new XProcException(errorCode);
        } else {
            throw new XProcException(errorCode, doc, doc.getStringValue());
        }
    }
}

