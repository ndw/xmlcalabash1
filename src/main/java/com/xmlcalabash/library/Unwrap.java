/*
 * Unwrap.java
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

import java.util.Map;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:unwrap",
        type = "{http://www.w3.org/ns/xproc}unwrap")

public class Unwrap extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("", "match");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Map<QName, RuntimeValue> inScopeOptions = null;
    private ProcessMatch matcher = null;

    /** Creates a new instance of Unwrap */
    public Unwrap(XProcRuntime runtime, XAtomicStep step) {
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

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(), getOption(_match));

        result.write(matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        // nop
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

