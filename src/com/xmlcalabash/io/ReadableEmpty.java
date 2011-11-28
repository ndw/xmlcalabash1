package com.xmlcalabash.io;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.SaxonApiException;
import com.xmlcalabash.model.Step;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 19, 2009
 * Time: 4:00:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReadableEmpty implements ReadablePipe {
    public void canReadSequence(boolean sequence) {
        // nop;
    }

    public boolean readSequence() {
        return false;
    }

    public XdmNode read() throws SaxonApiException {
        return null;    }

    public void setReader(Step step) {
        // nop
    }

    public void resetReader() {
        // nop
    }

    public boolean moreDocuments() {
        return false;
    }

    public boolean closed() {
        return false;
    }

    public int documentCount() {
        return 0;
    }

    public DocumentSequence documents() {
        return null;
    }
}
