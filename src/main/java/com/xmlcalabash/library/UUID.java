/*
 * UUID.java
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
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:uuid",
        type = "{http://www.w3.org/ns/xproc}uuid")

public class UUID extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _version = new QName("", "version");
    private static final QName _match = new QName("", "match");
    protected static final String logger = "org.xproc.library.hash";
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private String uuid = null;

    /*
     * Creates a new instance of UUID
     */
    public UUID(XProcRuntime runtime, XAtomicStep step) {
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

        String version = null;
        if (getOption(_version) != null) {
            version = getOption(_version).getString();
        }

        if (version != null && !"4".equals(version)) {
            throw XProcException.stepError(60);
        }

        java.util.UUID id = java.util.UUID.randomUUID();
        uuid = id.toString();

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(), getOption(_match));

        if (source.moreDocuments()) {
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }

        result.write(matcher.getResult());

    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        matcher.addText(uuid);
        return false;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public void processText(XdmNode node) throws SaxonApiException {
        matcher.addText(uuid);
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        matcher.addComment(uuid);
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        matcher.addPI(node.getNodeName().getLocalName(),uuid);
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        matcher.addAttribute(node.getNodeName(), uuid);
    }
}

