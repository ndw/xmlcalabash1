/*
 * RelevantNodes.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;

import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Apr 23, 2008
 * Time: 12:39:28 PM
 *
 * All this really does is make XdmSequenceIterator Iterable.
 *
 * To change this template use File | Settings | File Templates.
 */
public class AxisNodes implements Iterable<XdmNode> {
    public static final int NO_PIS = 0x01;
    public static final int NO_COMMENTS = 0x02;
    public static final int NO_WHITESPACE = 0x04;
    public static final int NO_DOC = 0x08;
    public static final int USE_WHEN = 0x10;

    public static final int ALL = 0;
    public static final int SIGNIFICANT = NO_PIS | NO_COMMENTS | NO_WHITESPACE;
    public static final int PIPELINE = SIGNIFICANT | NO_DOC | USE_WHEN;
    private static final int VALID_BITS = PIPELINE;

    protected Logger logger = LoggerFactory.getLogger(AxisNodes.class);
    private static QName use_when = new QName("", "use-when");
    private static QName p_use_when = new QName(XProcConstants.NS_XPROC, "use-when");
    private AxisNodesIter iter = null;
    private XProcRuntime runtime = null;
    private int filter = ALL;

    public AxisNodes(XdmNode start, Axis axis) {
        iter = new AxisNodesIter(start, axis);
    }

    public AxisNodes(XdmNode start, Axis axis, int filter) {
        if ((filter | VALID_BITS) != VALID_BITS) {
            throw new XProcException("Invalid filter passed to AxisNodes");
        }
        if ((filter & USE_WHEN) == USE_WHEN) {
            throw new XProcException("Pointless use of USE_WHEN filter in AxisNodes");
        }
        this.filter = filter;
        iter = new AxisNodesIter(start, axis);
    }

    public AxisNodes(XProcRuntime runtime, XdmNode start, Axis axis, int filter) {
        if ((filter | VALID_BITS) != VALID_BITS) {
            throw new XProcException("Invalid filter passed to AxisNodes");
        }
        this.runtime = runtime;
        this.filter = filter;
        iter = new AxisNodesIter(start, axis);
    }

    public Iterator<XdmNode> iterator() {
        return iter;
    }

    private class AxisNodesIter implements Iterator<XdmNode> {
        private XdmSequenceIterator iter = null;
        private XdmNode next = null;
        private boolean finished = false;
        private boolean hasNext = false;

        public AxisNodesIter(XdmNode start, Axis axis) {
            iter = start.axisIterator(axis);
        }

        public boolean hasNext() {
            hasNext = true;

            // This code is funky because XdmSequenceIterator.hasNext() consumes an item
            if (next == null) {
                boolean hasNext = iter.hasNext();
                finished = finished || !hasNext;
                if (finished) {
                    return false;
                }

                // If we got here, there must be a next
                next = (XdmNode) iter.next();
                while (next != null && !ok(next)) {
                    if (iter.hasNext()) {
                        next = (XdmNode) iter.next();
                    } else {
                        next = null;
                    }
                }

                return ok(next);
            } else {
                return true;
            }
        }

        public XdmNode next() {
            if (!hasNext) {
                hasNext();
                hasNext = false;
            }
            XdmNode r = next;
            next = null;
            return r;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported!");
        }

        private boolean ok(XdmNode node) {
            if (node == null) {
                return false;
            }

            if (((filter & NO_DOC) == NO_DOC)
                && (node.getNodeKind() == XdmNodeKind.ELEMENT)
                && (XProcConstants.p_documentation.equals(node.getNodeName())
                    || XProcConstants.p_pipeinfo.equals(node.getNodeName()))) {
                return false;
            }

            if (((filter & NO_COMMENTS) == NO_COMMENTS)
                    && node.getNodeKind() == XdmNodeKind.COMMENT) {
                return false;
            }

            if (((filter & NO_PIS) == NO_PIS)
                    && node.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION) {
                return false;
            }

            if (node.getNodeKind() == XdmNodeKind.TEXT) {
                if ((filter & NO_WHITESPACE) == NO_WHITESPACE) {
                    return !"".equals(node.toString().trim());
                } else {
                    return true;
                }
            }

            if (((filter & USE_WHEN) == USE_WHEN)
                    && node.getNodeKind() == XdmNodeKind.ELEMENT) {
                String expr = null;

                if (XProcConstants.NS_XPROC.equals(node.getNodeName().getNamespaceURI())) {
                    expr = node.getAttributeValue(use_when);
                }

                if (!XProcConstants.NS_XPROC.equals(node.getNodeName().getNamespaceURI())) {
                    expr = node.getAttributeValue(p_use_when);
                }

                if (expr != null) {
                    return useWhen(node, expr);
                } else {
                    return true;
                }
            }

            return true;
        }

        private boolean useWhen(XdmNode element, String xpath) {
            boolean use = false;

            // FIXME: I don't think this is a good idea, but XProcConfiguration calls relevant nodes w/o a runtime...
            if (runtime == null) {
                logger.info("The use-when attribute has no effect on pipelines in a configuration file.");
                return true;
            }

            try {
                XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
                // FIXME: Static base URI here?

                XdmSequenceIterator nsIter = element.axisIterator(Axis.NAMESPACE);
                while (nsIter.hasNext()) {
                    XdmNode ns = (XdmNode) nsIter.next();
                    xcomp.declareNamespace(ns.getNodeName().getLocalName(),ns.getStringValue());
                }

                XPathExecutable xexec = null;

                try {
                    xexec = xcomp.compile(xpath);
                } catch (SaxonApiException sae) {
                    throw sae;
                }

                XPathSelector selector = xexec.load();

                try {
                    use = selector.effectiveBooleanValue();
                } catch (SaxonApiUncheckedException saue) {
                    Throwable sae = saue.getCause();
                    if (sae instanceof XPathException) {
                        XPathException xe = (XPathException) sae;
                        if ("http://www.w3.org/2005/xqt-errors".equals(xe.getErrorCodeNamespace()) && "XPDY0002".equals(xe.getErrorCodeLocalPart())) {
                            throw XProcException.dynamicError(26, element, "Expression refers to context when none is available: " + xpath);
                        } else {
                            throw saue;
                        }

                    } else {
                        throw saue;
                    }
                }
            } catch (SaxonApiException sae) {
                if (S9apiUtils.xpathSyntaxError(sae)) {
                    throw XProcException.dynamicError(23, element, sae.getCause().getMessage());
                } else {
                    throw new XProcException(sae);
                }
            }

            return use;
        }
    }
}
