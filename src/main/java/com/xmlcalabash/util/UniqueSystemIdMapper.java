package com.xmlcalabash.util;

import net.sf.saxon.om.NodeInfo;

import java.util.function.Function;

/**
 * Create unique system identifiers.
 *
 * <p>Starting in version 11, Saxon enforces the constraint that no two documents may
 * have the same "document URI". This is, effectively, the system identifier. That's
 * a problem for XProc because we assign all p:inline documents the same system identifier
 * as the pipeline that contains them.</p>
 * <p>To work around that problem, this system id mapper adds a unique query string
 * to each system identifier. That shouldn't effect how it's stored, but will avoid
 * the "cannot have two different documents with the same document-uri" exception.</p>
 */

public class UniqueSystemIdMapper implements Function<NodeInfo, String> {
    @Override
    public String apply(NodeInfo node) {
        return URIUtils.makeUnique(node.getSystemId());
    }
}
