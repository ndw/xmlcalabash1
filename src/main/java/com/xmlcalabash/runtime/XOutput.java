package com.xmlcalabash.runtime;

import com.xmlcalabash.io.*;
import com.xmlcalabash.model.Log;
import com.xmlcalabash.model.Output;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import net.sf.saxon.s9api.XdmNode;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 7, 2008
 * Time: 8:00:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class XOutput {
    private DocumentSequence documents = null;
    private XProcRuntime runtime = null;
    private String port = null;
    private XdmNode node = null;
    private boolean sequenceOk = false;
    private WritablePipe writer = null;
    private WritablePipe inputWriter = null;
    private Vector<ReadablePipe> readers = null;

    public XOutput(XProcRuntime runtime, Output output) {
        this.runtime = runtime;
        node = output.getNode();
        port = output.getPort();
        sequenceOk = output.getSequence();
        documents = new DocumentSequence(runtime);
        readers = new Vector<ReadablePipe> ();
    }

    public void setLogger(Log log) {
        documents.setLogger(log);
    }

    public XdmNode getNode() {
        return node;
    }

    public String getPort() {
        return port;
    }

    public boolean getSequence() {
        return sequenceOk;
    }

    public ReadablePipe getReader() {
        ReadablePipe pipe = new Pipe(runtime, documents);
        readers.add(pipe);
        return pipe;
    }

    public WritablePipe getWriter() {
        if (writer != null) {
            throw new XProcException(node, "Attempt to create two writers for the same output.");
        }
        if (inputWriter != null) {
            writer = inputWriter;
        } else {
            writer = new Pipe(runtime, documents);
        }
        writer.canWriteSequence(sequenceOk);
        return writer;
    }
}
