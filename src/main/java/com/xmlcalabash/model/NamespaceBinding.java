/*
 * NamespaceBinding.java
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
package com.xmlcalabash.model;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Jul 21, 2008
 * Time: 8:45:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class NamespaceBinding {
    private XdmNode node = null;
    private XProcRuntime runtime = null;
    private String binding = null;
    private String expr = null;
    private HashSet<NamespaceUri> except = new HashSet<> (); // Default is nothing excluded
    private HashMap<String,NamespaceUri> nsBindings = new HashMap<> ();

    public NamespaceBinding(XProcRuntime xproc, XdmNode node) {
        runtime = xproc;
        this.node = node;

        XdmSequenceIterator<XdmNode> nsIter = node.axisIterator(Axis.NAMESPACE);
        while (nsIter.hasNext()) {
            XdmNode ns = nsIter.next();
            nsBindings.put((ns.getNodeName()==null ? "" : ns.getNodeName().getLocalName()),NamespaceUri.of(ns.getStringValue()));
        }
    }

    public XdmNode getNode() {
        return node;
    }

    public void setBinding(String binding) {
        nsBindings = null;
        this.binding = binding;
        if (binding != null && expr != null) {
            throw XProcException.staticError(41);
        }
    }

    public String getBinding() {
        return binding;
    }

    public void setXPath(String expr) {
        nsBindings = null;
        this.expr = expr;
        if (binding != null && expr != null) {
            throw XProcException.staticError(41);
        }
    }

    public String getXPath() {
        return expr;
    }

    public HashMap<String, NamespaceUri> getNamespaceBindings() {
        return nsBindings;
    }

    public void addExcludedNamespace(NamespaceUri exclude) {
        except.add(exclude);
    }

    public HashSet<NamespaceUri> getExcludedNamespaces() {
        return except;
    }
}
