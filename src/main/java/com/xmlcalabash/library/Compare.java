/*
 * Compare.java
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

import java.util.Iterator;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmAtomicValue;

import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:compare",
        type = "{http://www.w3.org/ns/xproc}compare")

public class Compare extends DefaultStep {
    private static final QName c_result = new QName("c", XProcConstants.NS_XPROC_STEP, "result");
    private static final QName doca = new QName("","doca");
    private static final QName docb = new QName("","docb");
    private static final QName _fail_if_not_equal = new QName("","fail-if-not-equal");
    private ReadablePipe source = null;
    private ReadablePipe alternate = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Compare
     */
    public Compare(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            alternate = pipe;
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

        XdmNode sdoc = source.read();
        XdmNode adoc = alternate.read();

        XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
        xcomp.declareVariable(doca);
        xcomp.declareVariable(docb);

        XPathExecutable xexec = xcomp.compile("deep-equal($doca,$docb)");
        XPathSelector selector = xexec.load();

        selector.setVariable(doca,sdoc);
        selector.setVariable(docb,adoc);

        Iterator<XdmItem> values = selector.iterator();
        XdmAtomicValue item = (XdmAtomicValue) values.next();
        boolean same = item.getBooleanValue();
        if (!same && getOption(_fail_if_not_equal,false)) {
            throw XProcException.stepError(19);
        }

        TreeWriter treeWriter = new TreeWriter(runtime);
        treeWriter.startDocument(step.getNode().getBaseURI());
        treeWriter.addStartElement(c_result);
        treeWriter.startContent();
        treeWriter.addText(""+same);
        treeWriter.addEndElement();
        treeWriter.endDocument();

        result.write(treeWriter.getResult());
    }
}

