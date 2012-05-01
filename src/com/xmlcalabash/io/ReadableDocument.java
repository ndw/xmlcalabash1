/*
 * ReadableDocument.java
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

import com.xmlcalabash.util.JSONtoXML;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.SaxonApiException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.util.XPointer;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Vector;

/**
 *
 * @author ndw
 */
public class ReadableDocument implements ReadablePipe {
    protected DocumentSequence documents = null;
    protected String uri = null;
    protected XProcRuntime runtime = null;
    private int pos = 0;
    private String base = null;
    private XdmNode node = null;
    private boolean readDoc = false;
    private Step reader = null;
    private Pattern pattern = null;

    public ReadableDocument(XProcRuntime runtime) {
        // This is an empty document sequence (p:empty)
        this.runtime = runtime;
        documents = new DocumentSequence(runtime);
    }

    /** Creates a new instance of ReadableDocument */
    public ReadableDocument(XProcRuntime runtime, XdmNode node, String uri, String base, String mask) {
        this.runtime = runtime;
        this.node = node;
        this.uri = uri;
        this.base = base;

        if (mask != null) {
            pattern = Pattern.compile(mask);
        }

        documents = new DocumentSequence(runtime);
    }

    public void canReadSequence(boolean sequence) {
        // nop; always false
    }

    public boolean readSequence() {
        return false;
    }
    
    public void resetReader() {
        pos = 0;
        // 6 Feb 2009: removed "readDoc = false;" because we don't want to re-read the document
        // if this happens in a loop. We just want to reset ourselves back to the beginning.
        // A readable document can only have a single doc, so it should be ok.
    }

    public void setReader(Step step) {
        reader = step;
    }

    public boolean moreDocuments() {
        if (!readDoc) {
            readDoc();
        }
        return pos < documents.size();
    }

    public boolean closed() {
        return true;
    }

    public int documentCount() {
        return documents.size();
    }

    public DocumentSequence documents() {
        return documents;
    }

    public XdmNode read() throws SaxonApiException {
        if (!readDoc) {
            readDoc();
        }

        XdmNode doc = documents.get(pos++);

        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " select read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }

        return doc;
    }

    protected void readDoc() {
        XdmNode doc;

        readDoc = true;
        if (uri != null) {
            try {
                // What if this is a directory?
                String fn = uri;
                if (fn.startsWith("file:")) {
                    fn = fn.substring(5);
                    if (fn.startsWith("///")) {
                        fn = fn.substring(2);
                    }
                }

                File f = new File(fn);
                if (f.isDirectory()) {
                    if (pattern == null) {
                        pattern = Pattern.compile("^.*\\.xml$");
                    }
                    for (File file : f.listFiles(new RegexFileFilter(pattern))) {
                        doc = runtime.parse(file.getCanonicalPath(), base);
                        documents.add(doc);
                    }
                } else {
                    doc = null;

                    try {
                        doc = runtime.parse(uri, base);
                    } catch (XProcException xe) {
                        if (runtime.transparentJSON()) {
                            try {
                                URI baseURI = new URI(base);
                                URL url = baseURI.resolve(uri).toURL();
                                URLConnection conn = url.openConnection();
                                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                                JSONTokener jt = new JSONTokener(reader);
                                doc = JSONtoXML.convert(runtime.getProcessor(), jt, runtime.jsonFlavor());
                                documents.add(doc);
                                return;
                            } catch (Exception e) {
                                throw xe;
                            }
                        } else {
                            throw xe;
                        }
                    }

                    if (fn.contains("#")) {
                        int pos = fn.indexOf("#");
                        String ptr = fn.substring(pos+1);

                        if (ptr.matches("^[\\w]+$")) {
                            ptr = "element(" + ptr + ")";
                        }

                        XPointer xptr = new XPointer(ptr);
                        Vector<XdmNode> nodes = xptr.selectNodes(runtime, doc);

                        if (nodes.size() == 1) {
                            doc = nodes.get(0);
                        } else if (nodes.size() != 0) {
                            throw new XProcException(node, "XPointer matches more than one node!?");
                        }
                    }

                    documents.add(doc);
                }
            } catch (Exception except) {
                throw XProcException.dynamicError(11, node, except, "Could not read: " + uri);
            }
        }
    }

    private class RegexFileFilter implements FileFilter {
        Pattern pattern = null;

        public RegexFileFilter(Pattern p) {
            this.pattern = p;
        }

        public boolean accept(File pathname) {
            Matcher matcher = pattern.matcher(pathname.getName());
            return matcher.matches();
        }
    }
}
