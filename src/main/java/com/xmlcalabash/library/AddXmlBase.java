/*
 * AddXmlBase.java
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
import java.util.ArrayList;
import java.util.Stack;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TypeUtils;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;

import javax.xml.XMLConstants;

import com.xmlcalabash.runtime.XAtomicStep;
import net.sf.saxon.type.BuiltInAtomicType;
import org.w3c.dom.Attr;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:add-xml-base",
        type = "{http://www.w3.org/ns/xproc}add-xml-base")

public class AddXmlBase extends DefaultStep implements ProcessMatchingNodes {
    private static final QName xml_base = new QName("xml",XMLConstants.XML_NS_URI, "base");
    private static final QName _all = new QName("", "all");
    private static final QName _relative = new QName("", "relative");
    private ProcessMatch matcher = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private boolean all = false;
    private boolean relative = false;
    private Stack<URI> baseURIStack = new Stack<URI> ();
        
    /* Creates a new instance of AddXmlBase */
    public AddXmlBase(XProcRuntime runtime, XAtomicStep step) {
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

        all = getOption(_all, false);
        relative = getOption(_relative, true);

        if (all && relative) {
            throw XProcException.stepError(58);
        }

        matcher = new ProcessMatch(runtime, this);
        XdmNode idoc = source.read();
        matcher.match(idoc, new RuntimeValue("*", step.getNode()));

        XdmNode doc = matcher.getResult();
        result.write(doc);
    }

    public boolean processStartDocument(XdmNode node) {
        return true;
    }

    public void processEndDocument(XdmNode node) {
        // nop
    }

    @Override
    public AttributeMap processAttributes(XdmNode node, AttributeMap matchingAttributes, AttributeMap nonMatchingAttributes) {
        throw new UnsupportedOperationException("This can't happen");
    }

    @Override
    public boolean processStartElement(XdmNode node, AttributeMap attributes) {
        String xmlBase = node.getBaseURI().normalize().toASCIIString();
        boolean addXmlBase = all || baseURIStack.size() == 0;
        if (!addXmlBase) {
            addXmlBase = !baseURIStack.peek().equals(node.getBaseURI());
        }

        if (addXmlBase && relative && baseURIStack.size() > 0) {
            // FIXME: What about non-hierarchical URIs?
            // Java.net.URI.relativize doesn't do what you think
            URI relURI = node.getBaseURI();
            String p1 = baseURIStack.peek().toASCIIString();
            String p2 = relURI.toASCIIString();

            boolean commonancestor = false;
            int i1 = p1.indexOf("/");
            int i2 = p2.indexOf("/");
            while (i1 >= 0 && i2 >= 0 && p1.substring(0, i1).equals(p2.substring(0,i2))) {
                commonancestor = true;
                p1 = p1.substring(i1+1);
                p2 = p2.substring(i2+1);
                i1 = p1.indexOf("/");
                i2 = p2.indexOf("/");
            }

            if (commonancestor) {
                StringBuilder walkUp = new StringBuilder();
                i1 = p1.indexOf("/");
                while (i1 >= 0) {
                    walkUp.append("../");
                    p1 = p1.substring(i1+1);
                    i1 = p1.indexOf("/");
                }
                xmlBase = walkUp + p2;
                p1 = "5";
            } else {
                xmlBase = relURI.toASCIIString();
            }
        }

        baseURIStack.push(node.getBaseURI());

        ArrayList<AttributeInfo> alist = new ArrayList<>();
        FingerprintedQName fq_xml_base = TypeUtils.fqName(xml_base);
        boolean found = false;
        for (AttributeInfo ainfo : attributes) {
            if (ainfo.getNodeName().equals(fq_xml_base)) {
                found = true;
                if ((all || addXmlBase || !ainfo.getValue().equals(xmlBase)) && !"".equals(xmlBase)) {
                    alist.add(new AttributeInfo(fq_xml_base, BuiltInAtomicType.ANY_ATOMIC, xmlBase, null, ReceiverOption.NONE));
                }
            } else {
                alist.add(ainfo);
            }
        }

        if (!found && addXmlBase) {
            alist.add(new AttributeInfo(fq_xml_base, BuiltInAtomicType.ANY_ATOMIC, xmlBase, null, ReceiverOption.NONE));
        }

        matcher.addStartElement(node, S9apiUtils.mapFromList(alist));
        return true;
    }

    public void processEndElement(XdmNode node) {
        matcher.addEndElement();
        baseURIStack.pop();
    }

    public void processText(XdmNode node) {
        throw new UnsupportedOperationException("This can't happen");
    }

    public void processComment(XdmNode node) {
        throw new UnsupportedOperationException("This can't happen");
    }

    public void processPI(XdmNode node) {
        throw new UnsupportedOperationException("This can't happen");
    }
}
