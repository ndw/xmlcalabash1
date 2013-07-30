package com.xmlcalabash.io;

import net.sf.saxon.s9api.XdmNode;

import java.util.Vector;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.Log;
import com.xmlcalabash.util.S9apiUtils;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 2, 2008
 * Time: 6:56:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class DocumentSequence {
    protected static final String logger = "com.xmlcalabash.io.documentsequence";
    private XProcRuntime runtime = null;
    private Vector<XdmNode> documents = new Vector<XdmNode>();
    private boolean closed = false;
    private static int idCounter = 0;
    private int id = 0;
    private PipeLogger outputlog = null;
    private int readerCount = 0;

    public DocumentSequence(XProcRuntime xproc) {
        runtime = xproc;
        id = idCounter++;
        //runtime.finest(logger, null, "Created document-sequence #" + id);
    }

    public void addReader() {
        readerCount++;
        //System.err.println(this + ": " + readerCount);
    }

    public int getReaderCount() {
        return readerCount;
    }

    public void setLogger(Log log) {
        if (log != null) {
            outputlog = new PipeLogger(runtime, log);
        }
    }

    public void add(XdmNode document) {
        if (closed) {
            throw new XProcException("You can't add a document to a closed DocumentSequence.");
        } else {
            S9apiUtils.assertDocument(document);

            //runtime.finest(logger, null, "Wrote " + (document == null ? "null" : document.getBaseURI()) + " to " + toString());
            documents.add(document);
            if (outputlog != null) {
                outputlog.log(document);
            }
        }
    }

    public XdmNode get(int count) {
        if (count < documents.size()) {
            XdmNode doc = documents.get(count);
            //runtime.finest(logger, null, "Read " + (doc == null ? "null" : doc.getBaseURI()) + " from " + toString());
            return doc;
        } else {
            return null;
        }
    }

    public void close() {
        readerCount--;
        closed = true;
        if (outputlog != null) {
            outputlog.stopLogging();
        }
    }

    public boolean closed() {
        return closed;
    }

    public int size() {
        return documents.size();
    }

    public void reset() {
        documents.clear();
        closed = false;
        if (outputlog != null) {
            outputlog.stopLogging();
        }
    }

    public String toString() {
        return "[document-sequence #" + id + " (" + documents.size() + " docs)]";
    }
}
