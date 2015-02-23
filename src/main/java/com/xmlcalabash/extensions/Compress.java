/*
 * Store.java
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
import com.xmlcalabash.library.Store;
import com.xmlcalabash.runtime.XAtomicStep;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

/**
 *
 * @author ndw
 *
 * N.B. This step implements both pxp:gzip and pxp:compress
 */
public class Compress extends Store {
    private static final QName _compression_method = new QName("compression-method");

    public Compress(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void run() throws SaxonApiException {
        String cmethod = getOption(_compression_method, "gzip");
        if ("gzip".equals(cmethod)) {
            method = CompressionMethod.GZIP;
        } else {
            throw XProcException.stepError(999, "The only compression method supported is 'gzip'.");
        }

        super.run();
    }
}

