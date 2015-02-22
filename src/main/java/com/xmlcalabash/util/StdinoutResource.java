package com.xmlcalabash.util;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.types.Resource;

public class StdinoutResource extends Resource {
    public StdinoutResource() {
        super("<stdinout>", true, 0, false);
    }

    @Override
    public InputStream getInputStream() {
        return System.in;
    }

    @Override
    public OutputStream getOutputStream() {
        return System.out;
    }
}
