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

package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;

/**
 *
 * @author ndw
 */
public class Uncompress extends DefaultStep {
    protected static final String logger = "com.xmlcalabash.extensions.gunzip";
    private static final QName _compression_method = new QName("compression-method");

    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Load
     */
    public Uncompress(XProcRuntime runtime, XAtomicStep step) {
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

        String cmethod = getOption(_compression_method, "gzip");
        if (!"gzip".equals(cmethod)) {
            throw XProcException.stepError(999, "The only compression method supported is 'gzip'.");
        }

        XdmNode root = S9apiUtils.getDocumentElement(source.read());
        byte[] decoded = null;

        // N.B. The Base64.decode() method *automatically* expands gzipped data!
        if ("base64".equals(root.getAttributeValue(_encoding))) {
            decoded = Base64.decode(root.getStringValue());
        } else {
            throw XProcException.stepError(999, "Input to cx:gunzip must be binary encoded data.");
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            InputSource is = new InputSource(bais);
            XdmNode doc = runtime.parse(is);
            result.write(doc);
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }
}

