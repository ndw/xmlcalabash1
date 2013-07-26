package com.xmlcalabash.util;

import java.io.OutputStream;

import org.apache.tools.ant.types.Resource;

public class StdoutResource extends Resource {
    public StdoutResource() {
        super("<stdout>", true, 0, false);
    }

    @Override
    public OutputStream getOutputStream() {
        return System.out;
    }
}
