/*
 * Pack.java
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
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:pack",
        type = "{http://www.w3.org/ns/xproc}pack")

public class Pack extends DefaultStep {
    protected static final String logger = "org.xproc.library.identity";
    private static final QName _wrapper = new QName("wrapper");
    private static final QName _wrapper_prefix = new QName("wrapper-prefix");
    private static final QName _wrapper_namespace = new QName("wrapper-namespace");
    private ReadablePipe source = null;
    private ReadablePipe alternate = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Pack
     */
    public Pack(XProcRuntime runtime, XAtomicStep step) {
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

        RuntimeValue wrapperNameValue = getOption(_wrapper);
        String wrapperNameStr = wrapperNameValue.getString();
        String wpfx = getOption(_wrapper_prefix, (String) null);
        String wns = getOption(_wrapper_namespace, (String) null);

        if (wpfx != null && wns == null) {
            throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace");
        }

        if (wns != null && wrapperNameStr.contains(":")) {
            throw XProcException.dynamicError(34, "You can't specify a namespace if the wrapper name contains a colon");
        }

        QName wrapper = null;
        if (wrapperNameStr.contains(":")) {
            wrapper = new QName(wrapperNameStr, wrapperNameValue.getNode());
        } else {
            wrapper = new QName(wpfx == null ? "" : wpfx, wns, wrapperNameStr);
        }

        while (source.moreDocuments() || alternate.moreDocuments()) {
            XdmNode sdoc = null;
            XdmNode adoc = null;
            if (source.moreDocuments()) {
                sdoc = source.read();
            }
            if (alternate.moreDocuments()) {
                adoc = alternate.read();
            }

            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(wrapper);
            tree.startContent();
            if (sdoc != null) {
                tree.addSubtree(sdoc);
            }
            if (adoc != null) {
                tree.addSubtree(adoc);
            }
            tree.endDocument();
            result.write(tree.getResult());
        }
    }
}

