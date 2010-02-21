/*
 * Wrap.java
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

import java.util.Map;
import java.util.Iterator;
import java.util.Stack;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.*;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.StandardNames;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 *
 * @author ndw
 */
public class Wrap extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("match");
    private static final QName _wrapper = new QName("wrapper");
    private static final QName _wrapper_prefix = new QName("wrapper-prefix");
    private static final QName _wrapper_namespace = new QName("wrapper-namespace");
    private static final QName _group_adjacent = new QName("group-adjacent");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private Map<QName, RuntimeValue> inScopeOptions = null;
    private QName wrapper = null;
    private int wrapperCode = 0;
    private String groupAdjacent = null;
    private Stack<Boolean> inGroup = new Stack<Boolean> ();

    /** Creates a new instance of Wrap */
    public Wrap(XProcRuntime runtime, XAtomicStep step) {
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

        if (wrapperNameStr.contains(":")) {
            wrapper = new QName(wrapperNameStr, wrapperNameValue.getNode());
        } else {
            wrapper = new QName(wpfx == null ? "" : wpfx, wns, wrapperNameStr);
        }

        groupAdjacent = getOption(_group_adjacent, (String) null);

        inGroup.push(false);

        XdmNode doc = source.read();
        NodeInfo inode = doc.getUnderlyingNode();
        NamePool pool = inode.getNamePool();
        wrapperCode = pool.allocate(wrapper.getPrefix(),wrapper.getNamespaceURI(),wrapper.getLocalName());

        matcher = new ProcessMatch(runtime, this);
        matcher.match(doc,getOption(_match));

        if (source.moreDocuments()) {
            throw XProcException.dynamicError(6);
        }

        result.write(matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        matcher.startDocument(node.getBaseURI());
        matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        matcher.startContent();
        matcher.addSubtree(node);
        matcher.addEndElement();
        matcher.endDocument();
        return false;

    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        if (!inGroup.peek()) {
            matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        }

        if (groupAdjacent != null && nextMatches(node)) {
            inGroup.pop();
            inGroup.push(true);
        } else {
            inGroup.pop();
            inGroup.push(false);
        }

        matcher.addStartElement(node);
        matcher.addAttributes(node);

        inGroup.push(false);
        return true;
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
        inGroup.pop();
        if (!inGroup.peek()) {
            matcher.addEndElement();
        }
    }

    public void processText(XdmNode node) throws SaxonApiException {
        if (!inGroup.peek()) {
            matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        }

        matcher.addText(node.getStringValue());

        if (groupAdjacent != null && nextMatches(node)) {
            inGroup.pop();
            inGroup.push(true);
        } else {
            matcher.addEndElement();
            inGroup.pop();
            inGroup.push(false);
        }
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        if (!inGroup.peek()) {
            matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        }

        matcher.addComment(node.getStringValue());

        if (groupAdjacent != null && nextMatches(node)) {
            inGroup.pop();
            inGroup.push(true);
        } else {
            matcher.addEndElement();
            inGroup.pop();
            inGroup.push(false);
        }
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        if (!inGroup.peek()) {
            matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        }

        matcher.addPI(node.getNodeName().getLocalName(),node.getStringValue());

        if (groupAdjacent != null && nextMatches(node)) {
            inGroup.pop();
            inGroup.push(true);
        } else {
            matcher.addEndElement();
            inGroup.pop();
            inGroup.push(false);
        }
    }

    private boolean nextMatches(XdmNode node) {
        XdmItem nodeValue = computeGroup(node);

        if (nodeValue == null) {
            return false;
        }

        XdmSequenceIterator iter = node.axisIterator(Axis.FOLLOWING_SIBLING);

        while (iter.hasNext()) {
            XdmNode chk = (XdmNode) iter.next();

            boolean skippable
                    = (chk.getNodeKind() == XdmNodeKind.COMMENT
                       || chk.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION);

            if (chk.getNodeKind() == XdmNodeKind.TEXT) {
                if ("".equals(chk.toString().trim())) {
                    skippable = true;
                }
            }

            if (matcher.matches(chk)) {
                XdmItem nextValue = computeGroup(chk);
                boolean same = S9apiUtils.xpathEqual(runtime.getProcessor(), nodeValue, nextValue);
                return same;
            }

            if (!skippable) {
                return false;
            }
        }

        return false;
    }

    private XdmItem computeGroup(XdmNode node) {
        try {
            XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
            XPathExecutable xexec = xcomp.compile(groupAdjacent);
            XPathSelector selector = xexec.load();
            selector.setContextItem(node);

            Iterator<XdmItem> values = selector.iterator();
            if (values.hasNext()) {
                return values.next();
            } else {
                return null;
            }
        } catch (SaxonApiException sae) {
            // WTF?
            return null;
        }
    }
}

