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
import net.sf.saxon.s9api.XdmNode;

import java.util.logging.Logger;

/**
 *
 * @author ndw
 */
public class Pipe implements ReadablePipe, WritablePipe {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private static int idCounter = 0;
    private int id = 0;
    private XProcRuntime runtime = null;
    private DocumentSequence documents = null;
    private int pos = 0;
    private boolean readSeqOk = false;
    private boolean writeSeqOk = false;
    private Step writer = null;
    private Step reader = null;

    /** Creates a new instance of Pipe */
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
        /*
        This causes problems in a for-each if the for-each never runs...
        Plus I think I"m catching this error higher up now.
        if (documents.size() == 0 && !writeSeqOk) {
            throw XProcException.dynamicError(7);
        }
        */
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
            throw XProcException.dynamicError(6);
        }

        XdmNode doc = documents.get(pos++);

        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }
        
        return doc;
    }

    public void write(XdmNode doc) {
        if (writer != null) {
            runtime.finest(null, writer.getNode(), writer.getName() + " wrote '" + (doc == null ? "null" : doc.getBaseURI()) + "' to " + this);
        }
        documents.add(doc);

        if (documents.size() > 1 && !writeSeqOk) {
            throw XProcException.dynamicError(7);
        }
    }

    public String toString() {
        return "[pipe #" + id + "] (" + documents + ")";
    }
}

