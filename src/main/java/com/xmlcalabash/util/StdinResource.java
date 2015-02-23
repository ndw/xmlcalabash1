package com.xmlcalabash.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.ImmutableResourceException;

public class StdinResource extends Resource {
    public StdinResource() {
        super("<stdin>", true, 0, false);
    }

    @Override
    public InputStream getInputStream() {
        return System.in;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new ImmutableResourceException();
    }
}
