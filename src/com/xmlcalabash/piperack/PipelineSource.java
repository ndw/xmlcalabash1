package com.xmlcalabash.piperack;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class PipelineSource {
    public String uri = null;
    public String name = null;
    public int expires = -1;

    public PipelineSource(String uri, String name, int expires) {
        this.uri = uri;
        this.name = name;
        this.expires = expires;
    }
}
