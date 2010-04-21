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

package com.xmlcalabash.library;

import java.net.URI;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.Serializer;

import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;

/**
 *
 * @author ndw
 */
public class Store extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _encoding = new QName("encoding");
    private static final QName c_encoding = new QName("c", XProcConstants.NS_XPROC_STEP, "encoding");
    private static final QName cx_decode = new QName("cx", XProcConstants.NS_CALABASH_EX, "decode");

    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Store
     */
    public Store(XProcRuntime runtime, XAtomicStep step) {
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

        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        URI href = null;
        RuntimeValue hrefOpt = getOption(_href);

        XdmNode doc = source.read();

        if (hrefOpt != null) {
            href = hrefOpt.getBaseURI().resolve(hrefOpt.getString());
        } else {
            href = doc.getBaseURI();
        }

        fine(hrefOpt.getNode(), "Storing to \"" + href + "\".");

        String decode = step.getExtensionAttribute(cx_decode);
        XdmNode root = S9apiUtils.getDocumentElement(doc);
        if (("true".equals(decode) || "1".equals(decode))
             && ((XProcConstants.NS_XPROC_STEP.equals(root.getNodeName().getNamespaceURI())
                  && "base64".equals(root.getAttributeValue(_encoding)))
                 || ("".equals(root.getNodeName().getNamespaceURI())
                     && "base64".equals(root.getAttributeValue(c_encoding))))) {
            storeBinary(doc, href);
        } else {
            storeXML(doc, href);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText(href.toString());
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }

    private void storeXML(XdmNode doc, URI href) throws SaxonApiException {
        Serializer serializer = makeSerializer();

        Processor qtproc = runtime.getProcessor();
        XQueryCompiler xqcomp = qtproc.newXQueryCompiler();
        XQueryExecutable xqexec = xqcomp.compile(".");
        XQueryEvaluator xqeval = xqexec.load();
        xqeval.setContextItem(doc);

        try {
            File output = new File(href);

            File path = new File(output.getParent());
            if (!path.isDirectory()) {
                if (!path.mkdirs()) {
                    throw XProcException.stepError(50);
                }
            }

            FileOutputStream outstr = new FileOutputStream(output);
            serializer.setOutputStream(outstr);
            xqeval.setDestination(serializer);
            xqeval.run();
            outstr.close();
        } catch (IOException ioe) {
            throw XProcException.stepError(50, ioe);
        }

    }

    private void storeBinary(XdmNode doc, URI href) {
        try {
            byte[] decoded = Base64.decode(doc.getStringValue());
            File output = new File(href);

            File path = new File(output.getParent());
            if (!path.isDirectory()) {
                if (!path.mkdirs()) {
                    throw XProcException.stepError(50);
                }
            }

            FileOutputStream outstr = new FileOutputStream(output);
            outstr.write(decoded);
            outstr.close();
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }

    }
}

