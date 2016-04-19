/*
 * Pipe.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * Portions Copyright 2007 Sun Microsystems, Inc.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://xproc.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */

package com.xmlcalabash.io;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.util.MessageFormatter;
import net.sf.saxon.s9api.XdmNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ndw
 */
public class Pipe implements ReadablePipe, WritablePipe {
    private Logger logger = LoggerFactory.getLogger(Pipe.class);
    private static int idCounter = 0;
    private int id = 0;
    private XProcRuntime runtime = null;
    private DocumentSequence documents = null;
    private int pos = 0;
    private boolean readSeqOk = false;
    private boolean writeSeqOk = false;
    private Step writer = null;
    private Step reader = null;
    private String stepName = null;
    private String portName = null;

    /* Creates a new instance of Pipe */
    public Pipe(XProcRuntime xproc) {
        runtime = xproc;
        documents = new DocumentSequence(xproc);
        documents.addReader();
        id = idCounter++;
    }

    public Pipe(XProcRuntime xproc, DocumentSequence seq) {
        runtime = xproc;
        documents = seq;
        seq.addReader();
        id = ++idCounter;
    }

    public void setReader(Step step) {
        reader = step;
    }

    public void setWriter(Step step) {
        writer  = step;
    }

    // These are for debugging...
    public void setNames(String stepName, String portName) {
        this.stepName = stepName;
        this.portName = portName;
    }

    public void canWriteSequence(boolean sequence) {
        writeSeqOk = sequence;
    }

    public void resetReader() {
        pos = 0;
    }
    
    public void resetWriter() {
        documents.reset();
        pos = 0;
    }

    public void canReadSequence(boolean sequence) {
        readSeqOk = sequence;
    }

    public boolean readSequence() {
        return readSeqOk;
    }

    public boolean writeSequence() {
        return writeSeqOk;
    }

    public boolean moreDocuments() {
        return pos < documents.size();
    }

    public boolean closed() {
        return documents.closed();
    }

    public void close() {
        documents.close();
    }

    public int documentCount() {
        return documents.size();
    }

    public DocumentSequence documents() {
        return documents;
    }

    public XdmNode read () {
        if (pos > 0 && !readSeqOk) {
            dynamicError(6);
        }

        XdmNode doc = documents.get(pos++);

        if (reader != null) {
            logger.trace(MessageFormatter.nodeMessage(reader.getNode(),
                    reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this));
        }
        
        return doc;
    }

    public void write(XdmNode doc) {
        if (writer != null) {
            logger.trace(MessageFormatter.nodeMessage(writer.getNode(),
                    writer.getName() + " wrote '" + (doc == null ? "null" : doc.getBaseURI()) + "' to " + this));
        }
        documents.add(doc);

        if (documents.size() > 1 && !writeSeqOk) {
            dynamicError(7);
        }
    }

    public String toString() {
        return "[pipe #" + id + "] (" + documents + ")";
    }

    private void dynamicError(int errno) {
        String msg = null;
        if (stepName != null) {
            msg = "Reading " + portName + " on " + stepName;
        }
        throw XProcException.dynamicError(errno, (String) msg);

    }
}

