/*
 * DebugProcessMatchingNodes.java
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

import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.SaxonApiException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Apr 18, 2008
 * Time: 10:22:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class DebugProcessMatchingNodes implements ProcessMatchingNodes {
    public boolean processStartDocument(XdmNode node) {
        System.err.println("processStartDocument(" + node + ")");
        return true;
    }

    public void processEndDocument(XdmNode node) {
        System.err.println("processEndDocument(" + node + ")");
    }

    @Override
    public AttributeMap processAttributes(XdmNode node, AttributeMap matchingAttributes, AttributeMap nonMatchingAttributes) {
        System.err.println("processAttributes(" + node + ")");
        return null;
    }

    @Override
    public boolean processStartElement(XdmNode node, AttributeMap attributes) {
        System.err.println("processStartElement(" + node + ")");
        return true;
    }

    public void processEndElement(XdmNode node) {
        System.err.println("processEndElement(" + node + ")");
    }

    public void processText(XdmNode node) {
        System.err.println("processText(" + node + ")");
    }

    public void processComment(XdmNode node) {
        System.err.println("processComment(" + node + ")");
    }

    public void processPI(XdmNode node) {
        System.err.println("processPI(" + node + ")");
    }
}
