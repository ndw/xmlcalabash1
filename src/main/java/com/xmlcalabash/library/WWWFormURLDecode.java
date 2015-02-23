/*
 * WWWFormURLDecode.java
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

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.TypeUtils;
import com.xmlcalabash.io.WritablePipe;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */
public class WWWFormURLDecode extends DefaultStep {
    public static final QName _value = new QName("", "value");
    public static final QName _name = new QName("", "name");
    public static final QName c_paramset = new QName("c",XProcConstants.NS_XPROC_STEP,"param-set");
    public static final QName c_param = new QName("c", XProcConstants.NS_XPROC_STEP, "param");
    private WritablePipe result = null;

    /** Creates a new instance of FormURLDecode */
    public WWWFormURLDecode(XProcRuntime runtime, XAtomicStep step) {
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

        String value = getOption(_value).getString();

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_paramset);
        tree.startContent();

        String[] params = value.split("&");
        for (int idx = 0; idx < params.length; idx++) {
            String p = params[idx];
            int pos = p.indexOf("=");
            if (pos > 0) {
                String name = p.substring(0, pos);
                String val = p.substring(pos+1);

                try {
                    TypeUtils.checkType(runtime, name, XProcConstants.xs_NCName, null);
                } catch (XProcException e) {
                    throw XProcException.stepError(61);
                }

                tree.addStartElement(c_param);
                tree.addAttribute(_name, name);
                tree.addAttribute(_value, decode(val));
                tree.startContent();
                tree.addEndElement();
            } else {
                throw new XProcException(step.getNode(), "Badly formatted parameters");
            }
        }

        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }

    private String decode(String val) {
        int pos;
        String result = "";

        while ((pos = val.indexOf("%")) >= 0) {
            result += val.substring(0, pos);

            try {
                String digits = val.substring(pos+1,pos+3);
                int dec = Integer.parseInt(digits, 16);
                char ch = (char) dec;
                result += ch;
            } catch (StringIndexOutOfBoundsException ex) {
                throw new XProcException("Badly formatted parameters", ex);
            } catch (NumberFormatException ex) {
                throw new XProcException("Badly formatted parameters", ex);
            }

            val = val.substring(pos+3);
        }

        result += val;
        return result;
    }
}
