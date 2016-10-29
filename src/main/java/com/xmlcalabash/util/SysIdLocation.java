package com.xmlcalabash.util;

import net.sf.saxon.expr.parser.Location;

/**
 * Implementation of {@link Location} with only System ID.
 *
 * @author Florent Georges
 */
public class SysIdLocation
        implements Location
{
    public SysIdLocation(String sysid) {
        this.sysid = sysid;
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
        return sysid;
    }

    @Override
    public Location saveLocation() {
        return this;
    }

    private final String sysid;
}
