package com.xmlcalabash.util;

import net.sf.saxon.expr.parser.Location;

/**
 * Implementation of {@link Location} with no info.
 *
 * @author Florent Georges
 */
public class VoidLocation
        implements Location
{
    static public VoidLocation instance() {
        return INSTANCE;
    }

    @Override
    public int getColumnNumber() {
        return -1;
    }

    @Override
    public int getLineNumber() {
        return -1;
    }

    @Override
    public String getPublicId() {
        return null;
    }

    @Override
    public String getSystemId() {
        return null;
    }

    @Override
    public Location saveLocation() {
        return this;
    }

    private VoidLocation() {
        // nothing
    }

    private static final VoidLocation INSTANCE = new VoidLocation();
}
