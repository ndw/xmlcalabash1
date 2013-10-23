/*
 * WrapSequence.java
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

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.DocumentSequenceIterator;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import net.sf.saxon.s9api.*;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.sxpath.XPathDynamicContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.model.RuntimeValue;

/**
 *
 * @author ndw
 */
public class WrapSequence extends DefaultStep {
    private static QName _wrapper = new QName("", "wrapper");
    private static QName _wrapper_prefix = new QName("", "wrapper-prefix");
    private static QName _wrapper_namespace = new QName("", "wrapper-namespace");
    private static QName _group_adjacent = new QName("", "group-adjacent");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private QName wrapper = null;
    private RuntimeValue groupAdjacent = null;

    /** Creates a new instance of WrapSequence */
    public WrapSequence(XProcRuntime runtime, XAtomicStep step) {
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
            throw XProcException.dynamicError(34, step.getNode(), "You can't specify a prefix without a namespace");
        }

        if (wns != null && wrapperNameStr.contains(":")) {
            throw XProcException.dynamicError(34, step.getNode(), "You can't specify a namespace if the wrapper name contains a colon");
        }

        if (wrapperNameStr.contains(":")) {
            wrapper = new QName(wrapperNameStr, wrapperNameValue.getNode());
        } else {
            wrapper = new QName(wpfx == null ? "" : wpfx, wns, wrapperNameStr);
        }

        groupAdjacent = getOption(_group_adjacent);

        if (groupAdjacent != null) {
            runAdjacent();
        } else {
            runSimple();
        }
    }

    private void runSimple() throws SaxonApiException {
        TreeWriter treeWriter = new TreeWriter(runtime);
        treeWriter.startDocument(step.getNode().getBaseURI());
        treeWriter.addStartElement(wrapper);
        treeWriter.startContent();

        while (source.moreDocuments()) {
            XdmNode node = source.read();
            treeWriter.addSubtree(node);
        }

        treeWriter.addEndElement();
        treeWriter.endDocument();

        XdmNode doc = treeWriter.getResult();
        result.write(doc);
    }

    private void runAdjacent() throws SaxonApiException {
        TreeWriter treeWriter = null;
        String last = null;
        boolean open = false;

        int count = 0;
        while (source.moreDocuments()) {
            count++;
            source.read();
        }
        source.resetReader();

        DocumentSequenceIterator xsi = new DocumentSequenceIterator(); // See below
        xsi.setLast(count);
        
        int pos = 0;
        while (source.moreDocuments()) {
            XdmNode node = source.read();
            pos++;

            Item<?> item = null;

            try {
                XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
                xcomp.setBaseURI(step.getNode().getBaseURI());

                for (String prefix : groupAdjacent.getNamespaceBindings().keySet()) {
                    xcomp.declareNamespace(prefix, groupAdjacent.getNamespaceBindings().get(prefix));
                }

                XPathExecutable xexec = xcomp.compile(groupAdjacent.getString());

                // From Michael Kay: http://markmail.org/message/vkb2vaq2miylgndu
                //
                // Underneath the s9api XPathExecutable is a net.sf.saxon.sxpath.XPathExpression.

                XPathExpression xexpr = xexec.getUnderlyingExpression();

                // Call createDynamicContext() on this to get an XPathDynamicContext object;

                XPathDynamicContext xdc = xexpr.createDynamicContext(node.getUnderlyingNode());

                // call getXPathContextObject() on that to get the underlying XPathContext.

                XPathContext xc = xdc.getXPathContextObject();

                // Then call XPathContext.setCurrentIterator()
                // to supply a SequenceIterator whose current() and position() methods return
                // the context item and position respectively. If there's any risk that the
                // expression will call the last() method, then it's simplest to make your
                // iterator's getProperties() return LAST_POSITION_FINDER, and implement the
                // LastPositionFinder interface, in which case last() will be implemented by
                // calling the iterator's getLastPosition() method. (Otherwise last() is
                // implemented by calling getAnother() to clone the iterator and calling next()
                // on the clone until the end of the sequence is reached).

                xsi.setPosition(pos);
                xsi.setItem(node.getUnderlyingNode());
                xc.setCurrentIterator(xsi);

                // Then evaluate the expression by calling iterate() on the
                // net.sf.saxon.sxpath.XPathExpression object.

                SequenceIterator<?> results = xexpr.iterate(xdc);
                item = results.next();

                if (item == null) {
                    throw new XProcException(step.getNode(), "The group-adjacent expression returned nothing.");
                }

                if (results.next() != null) {
                    throw new XProcException(step.getNode(), "Didn't expect group-adjacent to return a sequence!");
                }
            } catch (XPathException xe) {
                throw new XProcException(xe);
            }

            //
            //  Good luck!
            //

            // FIXME: Compute effective boolean value in a more robust way
            String cur = item.getStringValue();

            if (last != null) {
                if (last.equals(cur)) {
                    treeWriter.addSubtree(node);
                } else {
                    if (open) {
                        open = false;
                        treeWriter.addEndElement();
                        treeWriter.endDocument();
                        result.write(treeWriter.getResult());
                    }
                }
            }

            if (last == null || !last.equals(cur)) {
                last = cur;
                open = true;
                treeWriter = new TreeWriter(runtime);
                treeWriter.startDocument(step.getNode().getBaseURI());
                treeWriter.addStartElement(wrapper);
                treeWriter.startContent();
                treeWriter.addSubtree(node);
            }
        }

        if (open) {
            open = false;
            treeWriter.addEndElement();
            treeWriter.endDocument();
            result.write(treeWriter.getResult());
        }
    }
}

