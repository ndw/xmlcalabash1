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

import java.util.Vector;
import java.util.Hashtable;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.*;
import com.xmlcalabash.runtime.XAtomicStep;

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
    private Hashtable<String,String> rns = new Hashtable<String,String> ();
    private static Hashtable<QName,RuntimeValue> atomicStepsGetNoInScopeOptions = new Hashtable<QName,RuntimeValue> ();

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

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
        return false;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public void processText(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addAttribute(node, newValue);
    }

    private String computeReplacement(XdmNode node) {
        Vector<XdmItem> values = evaluateXPath(node, rns, replace.getString(), atomicStepsGetNoInScopeOptions);
        String newValue = "";
        for (XdmItem item : values) {
            newValue += item.getStringValue();
        }
        return newValue;
    }

}

