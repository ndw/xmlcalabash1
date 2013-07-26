package com.xmlcalabash.util;

import java.io.OutputStream;

import org.apache.tools.ant.types.Resource;

public class StderrResource extends Resource {
    public StderrResource() {
        super("<stderr>", true, 0, false);
    }

    @Override
    public OutputStream getOutputStream() {
        return System.err;
    }
}
