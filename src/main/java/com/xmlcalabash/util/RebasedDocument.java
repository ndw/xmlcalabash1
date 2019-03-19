////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.xmlcalabash.util;

import net.sf.saxon.om.GenericTreeInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.TreeInfo;

import java.util.function.Function;


/**
 * A <tt>RebasedDocument</tt> represents a view of a real Document in which all nodes are mapped to a different
 * base URI and/or system ID using supplied mapping functions.
 *
 * <p>It is possible to map either base URIs or system IDs or both.</p>
 *
 * <p>All properties of the nodes other than the base URI and system ID are unchanged.</p>
 *
 * <p>The user-supplied functions supplied to compute the base URI and system ID will be applied
 * to the underlying node in the "real" document. It is of course possible to supply a function that
 * ignores the supplied input.
 *
 * @since 9.9.0.2
 */

public class RebasedDocument extends GenericTreeInfo {

    private TreeInfo underlyingTree;
    private Function<NodeInfo, String> baseUriMapper;
    private Function<NodeInfo, String> systemIdMapper;


    /**
     * Create a rebased view of a document
     * @param doc the underlying document
     * @param baseUriMapper a function that is applied to a node in the original document
     *                      to deliver the base URI of the corresponding node in the rebased document
     * @param systemIdMapper a function that is applied to a node in the original document
     *                       to deliver the system ID of the corresponding node in the rebased document
     */

    public RebasedDocument(TreeInfo doc, Function<NodeInfo, String> baseUriMapper, Function<NodeInfo, String> systemIdMapper) {
        super(doc.getConfiguration());
        this.baseUriMapper = baseUriMapper;
        this.systemIdMapper = systemIdMapper;
        setRootNode(wrap(doc.getRootNode()));
        this.underlyingTree = doc;
    }

    /**
     * Create a wrapped node within this document
     * @param node the node to be wrapped - must be a node in the base document
     * @return the rebased node
     */

    public RebasedNode wrap(NodeInfo node) {
        return RebasedNode.makeWrapper(node, this, null);
    }

    /**
     * Ask whether the document contains any nodes whose type annotation is anything other than
     * UNTYPED. (This will be true if and only if the underlying document is untyped).
     *
     * <p>Note: in XSD 1.1 it is possible to define assertions such that the validity of a node
     * depends on its base URI. This class assumes that no-one would be quite so perverse. The validity
     * and type annotation of a virtual node are therefore the same as the validity and type annotation
     * of its underlying node.</p>
     *
     * @return true if the document contains elements whose type is other than UNTYPED
     */
    public boolean isTyped() {
        return underlyingTree.isTyped();
    }

    /**
     * Get the element with a given ID, if any
     *
     * @param id        the required ID value
     * @param getParent true if the required element is the parent of an element annotated as xs:ID, false
     *                  if the required node is the element itself
     * @return the element with the given ID value, or null if there is none.
     */

    /*@Nullable*/
    public NodeInfo selectID(String id, boolean getParent) {
        NodeInfo n = underlyingTree.selectID(id, false);
        if (n == null) {
            return null;
        } else {
            return wrap(n);
        }
    }

    /**
     * Get the underlying tree (the one whose nodes are being mapped to a new base URI and system ID)
     * @return the underlying tree
     */
    public TreeInfo getUnderlyingTree() {
        return underlyingTree;
    }

    /**
     * Get the function that is used to map nodes in the underlying tree to a new base URI
     * @return the base URI mapping function
     */

    public Function<NodeInfo, String> getBaseUriMapper() {
        return baseUriMapper;
    }

    /**
     * Get the function that is used to map nodes in the underlying tree to a new system ID
     *
     * @return the system ID mapping function
     */

    public Function<NodeInfo, String> getSystemIdMapper() {
        return systemIdMapper;
    }

}

