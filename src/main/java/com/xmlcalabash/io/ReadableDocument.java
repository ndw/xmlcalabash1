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

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore.DataInfo;
import com.xmlcalabash.io.DataStore.DataReader;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.util.HttpUtils;
import com.xmlcalabash.util.JSONtoXML;
import com.xmlcalabash.util.MessageFormatter;
import com.xmlcalabash.util.XPointer;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 *
 * @author ndw
 */
public class ReadableDocument implements ReadablePipe {
    private static final String ACCEPT_XML = "application/xml, text/xml, application/xml-external-parsed-entity, text/xml-external-parsed-entity";
    private static final String ACCEPT_JSON = "application/json, application/javascript, text/javascript, text/*, */*";
    private Logger logger = LoggerFactory.getLogger(ReadableDocument.class);
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

    /* Creates a new instance of ReadableDocument */
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

    public void setNames(String stepName, String portName) {
        // nop;
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
            logger.trace(MessageFormatter.nodeMessage(reader.getNode(),
                    reader.getName() + " select read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this));
        }

        return doc;
    }

    protected void readDoc() {
        readDoc = true;
        if (uri != null) {
            try {
                boolean sameDocumentReference = uri.startsWith("#");
                // What if this is a directory?
                URI baseURI = URI.create(base);
                if (!sameDocumentReference && "file".equalsIgnoreCase(baseURI.resolve(uri).getScheme())) {
                    final DataStore store = runtime.getDataStore();
                    store.infoEntry(uri, base, "*/*", new DataInfo() {
                        public void list(URI id, String media, long lastModified)
                                throws IOException {
                            final String accept = pattern == null ? ACCEPT_XML : "*/*";
                            final DataReader handler = new DataReader() {
                                public void load(URI id, String media,
                                        InputStream content, long len)
                                        throws IOException {
                                    content.close();
                                    String name = new File(id).getName();
                                    if (pattern == null || pattern.matcher(name).matches()) {
                                        documents.add(parse(id.toASCIIString(), base));
                                    }
                                }
                            };
                            String entry = id.toASCIIString();
                            if (media == null) {
                                store.listEachEntry(entry, entry, accept, new DataInfo() {
                                    public void list(URI id, String media, long lastModified)
                                            throws IOException {
                                        String entry = id.toASCIIString();
                                        store.readEntry(entry, entry, accept, null, handler);
                                    }
                                });
                            } else {
                                store.readEntry(entry, entry, accept, null, handler);
                            }
                        }
                    });
                } else {
                    try {
                        documents.add(parse(uri, base));
                    } catch (XProcException xe) {
                        if (runtime.transparentJSON()) {
                            try {
                                DataStore store = runtime.getDataStore();
                                store.readEntry(uri, base, ACCEPT_JSON, null, new DataReader() {
                                    public void load(URI id, String media, InputStream content, long len)
                                            throws IOException {
                                        String cs = HttpUtils.getCharset(media);
                                        if (cs == null) {
                                            cs = Charset.defaultCharset().name();
                                        }
                                        InputStreamReader reader = new InputStreamReader(content, cs);
                                        JSONTokener jt = new JSONTokener(reader);
                                        Processor processor = runtime.getProcessor();
                                        String flavor = runtime.jsonFlavor();
                                        documents.add(JSONtoXML.convert(processor, jt, flavor));
                                    }
                                });
                                return;
                            } catch (Exception e) {
                                throw xe;
                            }
                        } else {
                            throw xe;
                        }
                    }
                }
            } catch (Exception except) {
                throw XProcException.dynamicError(11, node, except, "Could not read: " + uri);
            }
        }
    }

    private XdmNode parse(String uri, String base) {
        XdmNode doc = runtime.parse(uri, base);

        if (uri.contains("#")) {
            int pos = uri.indexOf("#");
            String ptr = uri.substring(pos+1);

            if (ptr.matches("^[\\w]+$")) {
                ptr = "element(" + ptr + ")";
            }

            XPointer xptr = new XPointer(runtime, ptr, 1024 * 1000); // does this need to be configurable? No, because there can be only one fragid, right?
            Vector<XdmNode> nodes = xptr.selectNodes(runtime, doc);

            if (nodes.size() == 1) {
                doc = nodes.get(0);
            } else if (nodes.size() != 0) {
                throw new XProcException(node, "XPointer matches more than one node!?");
            }
        }
        return doc;
    }
}
