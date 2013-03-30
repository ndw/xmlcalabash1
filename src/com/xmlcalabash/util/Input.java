package com.xmlcalabash.util;

import java.io.InputStream;

import static com.xmlcalabash.util.Input.Kind.INPUT_STREAM;
import static com.xmlcalabash.util.Input.Kind.NONE;
import static com.xmlcalabash.util.Input.Kind.URI;
import static com.xmlcalabash.util.Input.Type.XML;

public class Input {
    private String uri;
    private InputStream inputStream;
    private Type type;
    private Kind kind = NONE;

    public Input(String uri) {
        this(uri, XML);
    }

    public Input(String uri, Type type) {
        this.uri = uri;
        this.type = type;
        kind = URI;
    }

    public Input(InputStream inputStream) {
        this(inputStream, XML);
    }

    public Input(InputStream inputStream, Type type) {
        this.inputStream = inputStream;
        this.type = type;
        kind = INPUT_STREAM;
    }

    public String getUri() {
        if (kind != URI) {
            throw new IllegalArgumentException("Input is not of kind 'URI'");
        }
        return uri;
    }

    public InputStream getInputStream() {
        if (kind != INPUT_STREAM) {
            throw new IllegalArgumentException("Input is not of kind 'INPUT_STREAM'");
        }
        return inputStream;
    }

    public Type getType() {
        return type;
    }

    public Kind getKind() {
        return kind;
    }

    public enum Type {
        XML, DATA
    }

    public enum Kind {
        NONE, URI, INPUT_STREAM
    }
}
