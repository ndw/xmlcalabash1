/*
 * Count.java
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
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:count",
        type = "{http://www.w3.org/ns/xproc}count")

public class Count extends DefaultStep {
    private static final QName c_result = new QName("c", XProcConstants.NS_XPROC_STEP, "result");
    private static final QName _limit = new QName("limit");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    
    /** Creates a new instance of Count */
    public Count(XProcRuntime runtime, XAtomicStep step) {
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

        String limitStr = getOption(_limit).getString();
        int limit = 0;

        try {
            limit = Integer.parseInt(limitStr);
        } catch (NumberFormatException nfe) {
            throw XProcException.dynamicError(19, "The limit on p:count must be an integer");
        }

        boolean done = false;
        int count = 0;
        while (!done && source.moreDocuments()) {
            XdmNode node = source.read();
            count++;
            done = (limit > 0 && count == limit);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_result);
        tree.startContent();
        tree.addText("" + count);
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }
}
