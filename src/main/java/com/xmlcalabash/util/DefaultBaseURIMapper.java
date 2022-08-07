package com.xmlcalabash.util;

import net.sf.saxon.om.NodeInfo;

import java.util.function.Function;

public class DefaultBaseURIMapper implements Function<NodeInfo, String> {
    private final String defaultBase;

    public DefaultBaseURIMapper() {
        defaultBase = null;
    }

    public DefaultBaseURIMapper(String defaultBase) {
        this.defaultBase = defaultBase;
    }

    @Override
    public String apply(NodeInfo node) {
        String base = node.getBaseURI();
        if (defaultBase != null && (base == null) || "".equals(base)) {
            return defaultBase;
        }
        return base;
    }
}
