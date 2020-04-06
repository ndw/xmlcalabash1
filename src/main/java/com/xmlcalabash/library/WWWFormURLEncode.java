/*
 * WWWFormURLEncode.java
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
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.ProcessMatchingNodes;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.type.BuiltInAtomicType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Vector;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:www-form-urlencode",
        type = "{http://www.w3.org/ns/xproc}www-form-urlencode")

public class WWWFormURLEncode extends DefaultStep implements ProcessMatchingNodes {
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Vector<Tuple> params = new Vector<>();
    private static final QName _match = new QName("", "match");
    private ProcessMatch matcher = null;
    private String encoded = "";

    /* Creates a new instance of FormURLEncode.
     */
    public WWWFormURLEncode(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        int pos = -1;
        int count = -1;
        for (Tuple t : params) {
            count++;
            if (name.equals(t.name)) {
                pos = count;
            }
        }

        if (pos >= 0) {
            params.remove(pos);
        }

        params.add(new Tuple(name, value));
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        for (Tuple t : params) {
            if (!"".equals(encoded)) {
                encoded += "&";
            }
            encoded += t.name.getLocalName() + "=" + encode(t.value.getString());
        }
        
        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(), getOption(_match));

        if (source.moreDocuments()) {
            throw XProcException.dynamicError(6, "Reading source on " + getStep().getName());
        }

        result.write(matcher.getResult());
    }
    
    public boolean processStartDocument(XdmNode node) {
        return true;
    }

    public void processEndDocument(XdmNode node) {
        // nop?
    }

    @Override
    public AttributeMap processAttributes(XdmNode node, AttributeMap matchingAttributes, AttributeMap nonMatchingAttributes) {
        ArrayList<AttributeInfo> alist = new ArrayList<>();
        for (AttributeInfo attr : nonMatchingAttributes) {
            alist.add(attr);
        }
        for (AttributeInfo attr : matchingAttributes) {
            alist.add(new AttributeInfo(attr.getNodeName(), BuiltInAtomicType.ANY_ATOMIC, encoded, attr.getLocation(), ReceiverOption.NONE));
        }
        return AttributeMap.fromList(alist);
    }

    @Override
    public boolean processStartElement(XdmNode node, AttributeMap attributes) {
        matcher.addText(encoded);
        return false;
    }

    public void processEndElement(XdmNode node) {
        // nop?
    }

    public void processText(XdmNode node) {
        matcher.addText(encoded);
    }

    public void processComment(XdmNode node) {
        matcher.addComment(encoded);
    }

    public void processPI(XdmNode node) {
        matcher.addPI(node.getNodeName().getLocalName(),encoded);
    }

    private String encode(String src) {
        String genDelims = ":/?#[]@";
        String subDelims = "!$'()*,;="; // N.B. NO & and no + !
        String unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~";
        String okChars = genDelims + subDelims + unreserved;

        StringBuilder encoded = new StringBuilder();
        byte[] bytes = src.getBytes(StandardCharsets.UTF_8);
        for (byte aByte : bytes) {
            if (okChars.indexOf(aByte) >= 0) {
                encoded.append((char) aByte);
            } else {
                if (aByte == ' ') {
                    encoded.append("+");
                } else {
                    encoded.append(String.format("%%%02X", aByte));
                }
            }
        }

        return encoded.toString();
    }

    private static class Tuple {
        public QName name;
        public RuntimeValue value;
        public Tuple(QName name, RuntimeValue value) {
            this.name = name;
            this.value = value;
        }
    }
}
