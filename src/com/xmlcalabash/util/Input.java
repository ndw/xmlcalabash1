package com.xmlcalabash.util;

import java.io.InputStream;

import static com.xmlcalabash.util.Input.Kind.INPUT_STREAM;
import static com.xmlcalabash.util.Input.Kind.NONE;
import static com.xmlcalabash.util.Input.Kind.URI;
import static com.xmlcalabash.util.Input.Type.DATA;
import static com.xmlcalabash.util.Input.Type.XML;

public class Input {
    private String uri;
    private InputStream inputStream;
    private Type type;
    private String contentType;
    private Kind kind = NONE;

    public Input(String uri) {
        this(uri, XML);
    }

    public Input(String uri, Type type) {
        this(uri, type, null);
    }

    public Input(String uri, Type type, String contentType) {
        this.uri = uri;
        this.type = type;
        this.contentType = contentType;
        kind = URI;
    }

    public Input(InputStream inputStream, String uri) {
        this(inputStream, uri, XML);
    }

    public Input(InputStream inputStream, String uri, Type type) {
        this(inputStream, uri, type, null);
    }

    public Input(InputStream inputStream, String uri, Type type, String contentType) {
        this(uri, type, contentType);
        this.inputStream = inputStream;
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

    public String getInputStreamUri() {
        if (kind != INPUT_STREAM) {
            throw new IllegalArgumentException("Input is not of kind 'INPUT_STREAM'");
        }
        return uri;
    }

    public Type getType() {
        return type;
    }

    public String getContentType() {
        if ((contentType != null) && (type != DATA)) {
            throw new IllegalStateException("contentType of input can only be set if type is DATA");
        }
        return contentType;
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
