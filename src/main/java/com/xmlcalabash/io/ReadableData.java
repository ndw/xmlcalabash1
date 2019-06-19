/*
 * ReadableData.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.xmlcalabash.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import com.xmlcalabash.util.*;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.json.JSONTokener;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore.DataReader;
import com.xmlcalabash.model.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ndw
 */
public class ReadableData implements ReadablePipe {
    public static final QName _contentType = new QName("","content-type");
    public static final QName c_contentType = new QName("c",XProcConstants.NS_XPROC_STEP, "content-type");
    public static final QName _encoding = new QName("","encoding");
    public static final QName c_encoding = new QName("c",XProcConstants.NS_XPROC_STEP, "encoding");
    private String contentType = null;
    private String forcedContentType = null;
    private Logger logger = LoggerFactory.getLogger(ReadablePipe.class);
    private int pos = 0;
    private QName wrapper = null;
    private String uri = null;
    private InputStream inputStream = null;
    private String serverContentType = "content/unknown";
    private XProcRuntime runtime = null;
    private DocumentSequence documents = null;
    private Step reader = null;

    /* Creates a new instance of ReadableDocument */
    public ReadableData(XProcRuntime runtime, QName wrapper, String uri, String contentType) {
        this(runtime, wrapper, uri, null, contentType, null);
    }

    public ReadableData(XProcRuntime runtime, QName wrapper, String uri, String contentType, String forcedContentType) {
        this(runtime, wrapper, uri, null, contentType, forcedContentType);
    }

    public ReadableData(XProcRuntime runtime, QName wrapper, InputStream inputStream, String contentType) {
        this(runtime, wrapper, null, inputStream, contentType, null);
    }

    public ReadableData(XProcRuntime runtime, QName wrapper, InputStream inputStream, String contentType, String forcedContentType) {
        this(runtime, wrapper, null, inputStream, contentType, forcedContentType);
    }

    private ReadableData(XProcRuntime runtime, QName wrapper, String uri, InputStream inputStream, String contentType, String forcedContentType) {
        this.runtime = runtime;
        this.uri = uri;
        this.inputStream = inputStream;
        this.wrapper = wrapper;
        this.contentType = contentType;
        this.forcedContentType = forcedContentType;
    }

    private DocumentSequence ensureDocuments() {
        if (documents != null) {
            return documents;
        }

        documents = new DocumentSequence(runtime);

        if ((uri == null) && (inputStream == null)) {
            return documents;
        }

		try {
			final String userContentType = parseContentType(contentType);
			if (uri == null) {
			    try {
			        read(userContentType, null, inputStream, getContentType());
			    } finally {
			        // This is the only case where the inputStream should be
			        // closed.
			        inputStream.close();
			    }
			} else if ("-".equals(uri)) {
				read(userContentType, getDataUri("-"), System.in,
						getContentType());
				// No need to close the input stream here, since it's System.in.
			} else {
				String accept = "application/json, text/json, text/*, */*";
				if (userContentType != null) {
					accept = userContentType + ", */*";
				}
				DataStore store = runtime.getDataStore();
				store.readEntry(uri, uri, accept, null, new DataReader() {
					public void load(URI dataURI, String serverContentType,
							InputStream stream, long len) throws IOException {
						read(userContentType, dataURI, stream,
								serverContentType);
					}
				});
				// Also no need to close the input stream here, since
				// DataStore implementations close the input stream when
				// necessary.
			}
		} catch (IOException ioe) {
            throw new XProcException(XProcConstants.dynamicError(29), ioe);
        }
        return documents;
    }


    // This method does NOT close the InputStream; it relies on the caller to close
    // the InputStream, as appropriate.
	private void read(String userContentType, URI dataURI,
			InputStream stream, String serverContentType)
			throws IOException {
		this.serverContentType = serverContentType;

        contentType = serverContentType;

        // If the input is - or a file: URI, assume the user provided content type
        // and charset are correct.
        if ((uri != null) && ("-".equals(uri) || "file".equals(dataURI.getScheme()))) {
            if (userContentType != null) {
                contentType = userContentType;
            }
        }

        String charset = parseCharset(contentType);

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(dataURI);
		if ((forcedContentType != null) && !"content/unknown".equals(forcedContentType)) {
		    // pretend...
		    serverContentType = forcedContentType;
		} else if ((contentType != null) && "content/unknown".equals(serverContentType)) {
		    // pretend...
		    serverContentType = contentType;
		}

		String serverBaseContentType = parseContentType(serverContentType);
		String serverCharset = parseCharset(serverContentType);

		if (serverCharset != null) {
		    // HACK! Make the content type here consistent with the content type returned
		    // from the http-request tests, just to make the test suite results more
		    // consistent.
		    serverContentType = serverBaseContentType + "; charset=\"" + serverCharset + "\"";
		}

		if (runtime.transparentJSON() && HttpUtils.jsonContentType(contentType)) {
		    if (charset == null) {
		        // FIXME: Is this right? I think it is...
		        charset = "UTF-8";
		    }
		    InputStreamReader reader = new InputStreamReader(stream, charset);
		    JSONTokener jt = new JSONTokener(reader);
		    XdmNode jsonDoc = JSONtoXML.convert(runtime.getProcessor(), jt, runtime.jsonFlavor());
		    tree.addSubtree(jsonDoc);
		} else {
		    tree.addStartElement(wrapper);
		    if (XProcConstants.c_data.equals(wrapper)) {
		        if ("content/unknown".equals(contentType)) {
		            tree.addAttribute(_contentType, "application/octet-stream");
		        } else {
		            tree.addAttribute(_contentType, contentType);
		        }
		        if (!isText(contentType, charset)) {
		            tree.addAttribute(_encoding, "base64");
		        }
		    } else {
		        if ("content/unknown".equals(contentType)) {
		            tree.addAttribute(c_contentType, "application/octet-stream");
		        } else {
		            tree.addAttribute(c_contentType, contentType);
		        }
		        if (!isText(contentType, charset)) {
		            tree.addAttribute(c_encoding, "base64");
		        }
		    }
		    tree.startContent();

		    if (isText(contentType, charset)) {
		        BufferedReader bufread;
		        if (charset == null) {
		            // FIXME: Is this right? I think it is...
		            charset = "UTF-8";
		        }
		        BufferedReader bufreader = new BufferedReader(new InputStreamReader(stream, charset));
		        int maxlen = 4096 * 3;
		        char[] chars = new char[maxlen];
		        int read = bufreader.read(chars, 0, maxlen);
		        while (read >= 0) {
		            if (read > 0) {
		                String data = new String(chars, 0, read);
		                tree.addText(data);
		            }
		            read = bufreader.read(chars, 0, maxlen);
		        }
		        bufreader.close();
		    } else {
		        // Fill the buffer each time so that we get an even number of base64 lines
		        int maxlen = 4096 * 3;
		        byte[] bytes = new byte[maxlen];
		        int pos = 0;
		        int readlen = maxlen;
		        boolean done = false;
		        while (!done) {
		            int read = stream.read(bytes, pos, readlen);
		            if (read >= 0) {
		                pos += read;
		                readlen -= read;
		            } else {
		                done = true;
		            }

		            if ((readlen == 0) || done) {
		                String base64 = Base64.encodeBytes(bytes, 0, pos);
		                tree.addText(base64 + "\n");
		                pos = 0;
		                readlen = maxlen;
		            }
		        }
		        stream.close();
		    }
		    tree.addEndElement();
		}

        tree.endDocument();

        documents.add(tree.getResult());
	}

    public void canReadSequence(boolean sequence) {
        // nop; always false
    }

    public boolean readSequence() {
        return false;
    }

    public void resetReader() {
        pos = 0;
    }

    public void setReader(Step step) {
        reader = step;
    }

    public void setNames(String stepName, String portName) {
        // nop;
    }

    public boolean moreDocuments() {
        DocumentSequence docs = ensureDocuments();
        return pos < docs.size();
    }

    public boolean closed() {
        return true;
    }

    public int documentCount() {
        DocumentSequence docs = ensureDocuments();
        return docs.size();
    }

    public DocumentSequence documents() {
        return ensureDocuments();
    }

    public XdmNode read() throws SaxonApiException {
        DocumentSequence docs = ensureDocuments();
        XdmNode doc = docs.get(pos++);
        if (reader != null) {
            logger.trace(MessageFormatter.nodeMessage(reader.getNode(),
                    reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this));
        }
        return doc;
    }

    // =======================================================================

    protected URI getDataUri(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException use) {
            throw new XProcException(use);
        }
    }

    protected String getContentType() {
        return serverContentType;
    }

    // =======================================================================

    private boolean isText(String contentType, String charset) {
        return ("application/xml".equals(contentType)
                || contentType.endsWith("+xml")
                || contentType.startsWith("text/")
                || "utf-8".equals(charset));
    }

    private String parseContentType(String contentType) {
        if (contentType == null) {
            return null;
        }

        int pos = contentType.indexOf(";");
        if (pos > 0) {
            String type = contentType.substring(0,pos).trim();
            return type;
        } else {
            return contentType;
        }
    }

    private String parseCharset(String contentType) {
        String charset = HttpUtils.getCharset(contentType);

        if (charset != null) {
            return charset.toLowerCase();
        }

        return null;
    }
}


