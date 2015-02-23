package com.xmlcalabash.util;

import java.io.OutputStream;

import static com.xmlcalabash.util.Output.Kind.NONE;
import static com.xmlcalabash.util.Output.Kind.OUTPUT_STREAM;
import static com.xmlcalabash.util.Output.Kind.URI;

public class Output {
    private String uri;
    private OutputStream outputStream;
    private Kind kind = NONE;

    public Output(String uri) {
        this.uri = uri;
        kind = URI;
    }

    public Output(OutputStream outputStream) {
        this.outputStream = outputStream;
        kind = OUTPUT_STREAM;
    }

    public String getUri() {
        if (kind != URI) {
            throw new IllegalArgumentException("Output is not of kind 'URI'");
        }
        return uri;
    }

    public OutputStream getOutputStream() {
        if (kind != OUTPUT_STREAM) {
            throw new IllegalArgumentException("Output is not of kind 'OUTPUT_STREAM'");
        }
        return outputStream;
    }

    public Kind getKind() {
        return kind;
    }

    public enum Kind {
        NONE, URI, OUTPUT_STREAM
    }
}
