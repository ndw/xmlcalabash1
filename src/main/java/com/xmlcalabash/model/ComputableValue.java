/*
 * ComputableValue.java
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

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: May 16, 2008
 * Time: 8:22:30 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ComputableValue {
    public QName getName();
    public String getType();
    public QName getTypeAsQName();
    public XdmNode getNode();
    public String getSelect();
    public void addNamespaceBinding(NamespaceBinding nsbinding);
    public Vector<NamespaceBinding> getNamespaceBindings();
    // FIXME: this doesn't need to be a vector, does it?
    public Vector<Binding> getBinding();
}
