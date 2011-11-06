/*
 * Load.java
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

import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.model.RuntimeValue;

/**
 *
 * @author ndw
 */
public class Load extends DefaultStep {
    protected static final String logger = "org.xproc.library.load";
    private static final QName _href = new QName("href");
    private static final QName _dtd_validate = new QName("dtd-validate");
    private static final QName err_XD0011 = new QName("err", XProcConstants.NS_XPROC_ERROR, "XD0011");

    private WritablePipe result = null;
    private URI href = null;

    /**
     * Creates a new instance of Load
     */
    public Load(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        try {
            XdmNode doc = runtime.getConfigurer().getXMLCalabashConfigurer().loadDocument(this);
            result.write(doc);
        } catch (XProcException e) {
            if (runtime.getDebug()) {
                e.printStackTrace();
            }
            if (err_XD0011.equals(e.getErrorCode())) {
                RuntimeValue href = getOption(_href);
                String baseURI = href.getBaseURI().toASCIIString();
                boolean validate = getOption(_dtd_validate, false);
                throw XProcException.stepError(11, "Could not load " + href.getString() + " (" + baseURI + ") dtd-validate=" + validate);
            }
            throw e;
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }
}

