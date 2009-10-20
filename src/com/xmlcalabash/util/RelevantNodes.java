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

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.QName;

import java.util.Iterator;

import com.xmlcalabash.core.XProcConstants;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Apr 23, 2008
 * Time: 12:39:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class RelevantNodes implements Iterable<XdmNode> {
    private RelevantNodesIter iter = null;

    public RelevantNodes(XdmNode start, Axis axis) {
        iter = new RelevantNodesIter(start, axis);
    }

    public RelevantNodes(XdmNode start, Axis axis, boolean ignore) {
        iter = new RelevantNodesIter(start, axis, ignore);
    }

    public RelevantNodes(XdmNode start, QName name) {
        iter = new RelevantNodesIter(start, Axis.CHILD, name);
    }

    public Iterator<XdmNode> iterator() {
        return iter;
    }

    private class RelevantNodesIter implements Iterator<XdmNode> {
        private XdmSequenceIterator iter = null;
        private XdmNode next = null;
        private boolean finished = false;
        private QName onlyMatch = null;
        private boolean ignoreInfo = true;

        public RelevantNodesIter(XdmNode start, Axis axis) {
            iter = start.axisIterator(axis);
        }

        public RelevantNodesIter(XdmNode start, Axis axis, boolean ignore) {
            ignoreInfo = ignore;
            iter = start.axisIterator(axis);
        }

        public RelevantNodesIter(XdmNode start, Axis axis, QName name) {
            iter = start.axisIterator(axis);
            onlyMatch = name;
        }

        public boolean hasNext() {
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
            XdmNode r = next;
            next = null;
            return r;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported!");
        }

        private boolean ok(XdmNode node) {
            if (node == null
                || (ignoreInfo && (XProcConstants.p_documentation.equals(node.getNodeName())
                                   || XProcConstants.p_pipeinfo.equals(node.getNodeName())))) {
                return false;
            }

            if (node.getNodeKind() == XdmNodeKind.COMMENT
                || node.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION) {
                return false;
            }

            if (node.getNodeKind() == XdmNodeKind.TEXT) {
                return !"".equals(node.toString().trim());
            }

            if (node.getNodeKind() == XdmNodeKind.ELEMENT || node.getNodeKind() == XdmNodeKind.ATTRIBUTE) {
                return onlyMatch == null || onlyMatch.equals(node.getNodeName());
            }

            return false; // What can this be anyway?
        }
    }
}
