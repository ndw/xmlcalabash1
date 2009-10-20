package com.xmlcalabash.library;

/*
 * HttpRequest.java
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

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.XdmNodeKind;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.MIMEReader;
import com.xmlcalabash.util.Base64;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import java.util.Vector;
import java.util.List;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.ProxySelector;
import java.net.Proxy;
import java.net.InetSocketAddress;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

import org.xml.sax.InputSource;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.methods.*;

public class HttpRequest extends DefaultStep {
    public static final QName c_request = new QName("c", XProcConstants.NS_XPROC_STEP, "request");
    public static final QName cx_timeout = new QName("cx",XProcConstants.NS_CALABASH_EX,"timeout");

    public static final QName _href = new QName("", "href");
    public static final QName _detailed = new QName("", "detailed");
    public static final QName _status_only = new QName("", "status-only");
    public static final QName _username = new QName("", "username");
    public static final QName _password = new QName("", "password");
    public static final QName _auth_method = new QName("", "auth-method");
    public static final QName _send_authorization = new QName("", "send-authorization");
    public static final QName _override_content_type = new QName("", "override-content-type");
    public static final QName _content_type = new QName("", "content-type");
    public static final QName _name = new QName("", "name");
    public static final QName _value = new QName("", "value");
    public static final QName _id = new QName("", "id");
    public static final QName _description = new QName("", "description");
    public static final QName _status = new QName("", "status");
    public static final QName _boundary = new QName("", "boundary");

    private static final int bufSize = 14400; // A multiple of 3, 4, and 75 for base64 line breaking

    private HttpClient client = null;
    private boolean statusOnly = false;
    private boolean detailed = false;
    private String method = null;
    private URI requestURI = null;
    private Vector<Header> headers = new Vector<Header> ();
    private String contentType = null;
    private String overrideContentType = null;

    private ReadablePipe source = null;
    private WritablePipe result = null;

    /** Creates a new instance of HttpRequest */
    public HttpRequest(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode requestDoc = source.read();
        XdmNode start = S9apiUtils.getDocumentElement(requestDoc);

        if (!c_request.equals(start.getNodeName())) {
            throw new UnsupportedOperationException("Not a c:http-request!");
        }

        // Check for valid attributes
        XdmSequenceIterator iter = start.axisIterator(Axis.ATTRIBUTE);
        boolean ok = true;
        while (iter.hasNext()) {
            XdmNode attr = (XdmNode) iter.next();
            QName name = attr.getNodeName();
            if (_method.equals(name) || _href.equals(name) || _detailed.equals(name)
                    || _status_only.equals(name) || _username.equals(name) || _password.equals(name)
                    || _auth_method.equals(name) || _send_authorization.equals(name)
                    || _override_content_type.equals(name)) {
                // nop
            } else {
                if (XMLConstants.DEFAULT_NS_PREFIX.equals(name.getNamespaceURI())) {
                    throw new XProcException("Unsupported attribute on c:request for p:http-request: " + name);
                }
            }
        }

        method = start.getAttributeValue(_method);
        statusOnly = "true".equals(start.getAttributeValue(_status_only));
        detailed = "true".equals(start.getAttributeValue(_detailed));
        overrideContentType = start.getAttributeValue(_override_content_type);

        if (start.getAttributeValue(_href) == null) {
            throw new XProcException("The 'href' attribute must be specified on c:request for p:http-request");
        }

        requestURI = start.getBaseURI().resolve(start.getAttributeValue(_href));

        if ("file".equals(requestURI.getScheme())) {
            doFile();
            return;
        }

        client = new HttpClient();

        String timeOutStr = step.getExtensionAttribute(cx_timeout);
        if (timeOutStr != null) {
            HttpMethodParams params = client.getParams();
            params.setSoTimeout(Integer.parseInt(timeOutStr));
        }

        ProxySelector proxySelector = ProxySelector.getDefault();
        List<Proxy> plist = proxySelector.select(requestURI);
        // I have no idea what I'm expected to do if I get more than one...
        if (plist.size() > 0) {
            Proxy proxy = plist.get(0);
            switch (proxy.type()) {
                case DIRECT:
                    // nop;
                    break;
                case HTTP:
                    // This can't cause a ClassCastException, right?
                    InetSocketAddress addr = (InetSocketAddress) proxy.address();
                    String host = addr.getHostName();
                    int port = addr.getPort();
                    client.getHostConfiguration().setProxy(host,port);
                    break;
                default:
                    // FIXME: send out a log message
                    break;
            }
        }

        if (start.getAttributeValue(_username) != null) {
            String user = start.getAttributeValue(_username);
            String pass = start.getAttributeValue(_password);
            String meth = start.getAttributeValue(_auth_method);

            if (meth == null || !("basic".equals(meth.toLowerCase()) || "digest".equals(meth.toLowerCase()))) {
                throw XProcException.stepError(3, "Unsupported auth-method: " + meth);
            }

            String host = requestURI.getHost();
            int port = requestURI.getPort();
            AuthScope scope = new AuthScope(host,port);

            UsernamePasswordCredentials cred = new UsernamePasswordCredentials(user, pass);

            client.getState().setCredentials(scope, cred);

            if ("basic".equals(meth.toLowerCase())) {
                client.getParams().setAuthenticationPreemptive(true);
            }
        }

        iter = start.axisIterator(Axis.CHILD);
        XdmNode body = null;
        while (iter.hasNext()) {
            XdmNode event = (XdmNode) iter.next();
            // FIXME: What about non-whitespace text nodes?
            if (event.getNodeKind() == XdmNodeKind.ELEMENT) {
                if (body != null) {
                    throw new UnsupportedOperationException("Elements follow c:multipart or c:body");
                }

                if (XProcConstants.c_header.equals(event.getNodeName())) {
                    headers.add(new Header(event.getAttributeValue(_name), event.getAttributeValue(_value)));
                } else if (XProcConstants.c_multipart.equals(event.getNodeName())
                           || XProcConstants.c_body.equals(event.getNodeName())) {
                    body = event;
                } else {
                    throw new UnsupportedOperationException("Unexpected request element: " + event.getNodeName());
                }
            }
        }

        HttpMethodBase httpResult;

        if (method == null) {
            throw new XProcException("Method must be specified.");
        }

        if ("get".equals(method.toLowerCase())) {
            httpResult = doGet();
        } else if ("post".equals(method.toLowerCase())) {
            httpResult = doPost(body);
        } else if ("put".equals(method.toLowerCase())) {
            httpResult = doPut(body);
        } else if ("head".equals(method.toLowerCase())) {
            httpResult = doHead();
        } else if ("delete".equals(method.toLowerCase())) {
            httpResult = doDelete();
        } else {
            throw new UnsupportedOperationException("Unrecognized http method: " + method);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(requestURI);

        try {
            // Execute the method.
            int statusCode = client.executeMethod(httpResult);

            String contentType = getContentType(httpResult);
            if (overrideContentType != null) {
                if ((xmlContentType(contentType) && overrideContentType.startsWith("image/"))
                    || (contentType.startsWith("text/") && overrideContentType.startsWith("image/"))
                    || (contentType.startsWith("image/") && xmlContentType(overrideContentType))
                    || (contentType.startsWith("image/") && overrideContentType.startsWith("text/"))
                    || (contentType.startsWith("multipart/") && !overrideContentType.startsWith("multipart/"))
                    || (!contentType.startsWith("multipart/") && overrideContentType.startsWith("multipart/"))) {
                    throw XProcException.stepError(30);
                }

                //System.err.println(overrideContentType + " overrides " + contentType);
                contentType = overrideContentType;
            }

            if (detailed) {
                tree.addStartElement(XProcConstants.c_response);
                tree.addAttribute(_status, "" + statusCode);
                tree.startContent();

                for (Header header : httpResult.getResponseHeaders()) {
                    // I don't understand why/how HeaderElement parsing works. I get very weird results.
                    // So I'm just going to go the long way around...
                    String h = header.toString();
                    int cp = h.indexOf(":");
                    String name = header.getName();
                    String value = h.substring(cp+1).trim();

                    tree.addStartElement(XProcConstants.c_header);
                    tree.addAttribute(_name, name);
                    tree.addAttribute(_value, value);
                    tree.startContent();
                    tree.addEndElement();
                }

                if (statusOnly) {
                    // Skip reading the result
                } else {
                    // Read the response body.
                    InputStream bodyStream = httpResult.getResponseBodyAsStream();
                    readBodyContent(tree, bodyStream, httpResult);
                }

                tree.addEndElement();
            } else {
                if (statusOnly) {
                    // Skip reading the result
                } else {
                    // Read the response body.
                    InputStream bodyStream = httpResult.getResponseBodyAsStream();
                    readBodyContent(tree, bodyStream, httpResult);
                }
            }
        } catch (Exception e) {
            throw new XProcException(e);
        } finally {
            // Release the connection.
            httpResult.releaseConnection();
        }

        tree.endDocument();

        XdmNode resultNode = tree.getResult();

        result.write(resultNode);
    }

    private GetMethod doGet() {
        GetMethod method = new GetMethod(requestURI.toASCIIString());

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        for (Header header : headers) {
            method.addRequestHeader(header);
        }

        return method;
    }

    private HeadMethod doHead() {
        HeadMethod method = new HeadMethod(requestURI.toASCIIString());

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        for (Header header : headers) {
            method.addRequestHeader(header);
        }

        return method;
    }

    private DeleteMethod doDelete() {
        DeleteMethod method = new DeleteMethod(requestURI.toASCIIString());

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        for (Header header : headers) {
            method.addRequestHeader(header);
        }

        return method;
    }

    private PutMethod doPut(XdmNode body) {
        PutMethod method = new PutMethod(requestURI.toASCIIString());
        doPutOrPost(method,body);
        return method;
    }


    private PostMethod doPost(XdmNode body) {
        PostMethod method = new PostMethod(requestURI.toASCIIString());
        doPutOrPost(method,body);
        return method;
    }

    private void doPutOrPost(EntityEnclosingMethod method, XdmNode body) {
        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        for (Header header : headers) {
            method.addRequestHeader(header);
        }

        contentType = body.getAttributeValue(_content_type);
        if (contentType == null) {
            throw new XProcException("Content-type on c:body is required.");
        }

        // FIXME: This sucks rocks. I want to write the data to be posted, not provide some way to read it
        String postContent = null;
        try {
            if (xmlContentType(contentType)) {
                Serializer serializer = makeSerializer();

                Vector<XdmNode> content = new Vector<XdmNode> ();
                XdmSequenceIterator iter = body.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode node = (XdmNode) iter.next();
                    content.add(node);
                }

                // FIXME: set serializer properties appropriately!
                StringWriter writer = new StringWriter();
                serializer.setOutputWriter(writer);
                S9apiUtils.serialize(runtime, content, serializer);
                writer.close();
                postContent = writer.toString();
            } else {
                StringWriter writer = new StringWriter();
                XdmSequenceIterator iter = body.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode node = (XdmNode) iter.next();
                    writer.write(node.getStringValue());
                }
                writer.close();
                postContent = writer.toString();
            }

            StringRequestEntity requestEntity = new StringRequestEntity(postContent, contentType,"UTF-8");
            method.setRequestEntity(requestEntity);

        } catch (IOException ioe) {
            throw new XProcException(ioe);
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
    }

    private String getFullContentType(HttpMethodBase method) {
        Header contentTypeHeader = method.getResponseHeader("Content-Type");
        return getFullContentType(contentTypeHeader);
    }

    private String getFullContentType(Header contentTypeHeader) {
        if (contentTypeHeader == null) {
            // This should never happen
            return null;
        }

        HeaderElement[] contentTypes = contentTypeHeader.getElements();
        if (contentTypes == null || contentTypes.length == 0) {
            // This should never happen
            return null;
        }

        String ctype = contentTypes[0].getName();
        NameValuePair[] params = contentTypes[0].getParameters();
        if (params != null) {
            for (NameValuePair pair : params) {
                ctype = ctype + "; " + pair.getName() + "=\"" + pair.getValue() + "\"";
            }
        }

        return ctype;
    }

    private String getHeaderValue(Header header) {
        if (header == null) {
            // This should never happen
            return null;
        }

        HeaderElement[] elems = header.getElements();
        if (elems == null || elems.length == 0) {
            // This should never happen
            return null;
        }

        return elems[0].getName();
    }

    private String getContentType(HttpMethodBase method) {
        Header contentTypeHeader = method.getResponseHeader("Content-Type");
        return getContentType(contentTypeHeader);
    }

    private String getContentType(Header contentTypeHeader) {
        return getHeaderValue(contentTypeHeader);
    }

    private String getContentBoundary(HttpMethodBase method) {
        Header contentTypeHeader = method.getResponseHeader("Content-Type");
        return getContentBoundary(contentTypeHeader);
    }

    private String getContentBoundary(Header contentTypeHeader) {
        if (contentTypeHeader == null) {
            // This should never happen
            return null;
        }

        HeaderElement[] contentTypes = contentTypeHeader.getElements();
        if (contentTypes == null || contentTypes.length == 0) {
            // This should never happen
            return null;
        }

        String boundary = contentTypes[0].getParameterByName("boundary").getValue();
        return boundary;
    }

    private String getContentCharset(HttpMethodBase method) {
        Header contentTypeHeader = method.getResponseHeader("Content-Type");
        return getContentCharset(contentTypeHeader);
    }

    private String getContentCharset(Header contentTypeHeader) {
        if (contentTypeHeader == null) {
            // This should never happen
            return null;
        }

        HeaderElement[] contentTypes = contentTypeHeader.getElements();
        if (contentTypes == null || contentTypes.length == 0) {
            // This should never happen
            return null;
        }

        NameValuePair cpair = contentTypes[0].getParameterByName("charset");
        if (cpair == null) {
            return "US-ASCII";
        } else {
            return cpair.getValue();
        }
    }

    private boolean xmlContentType(String contentType) {
        return contentType != null
                && ("application/xml".equals(contentType)
                    || contentType.startsWith("application/xml;")
                    || "text/xml".equals(contentType)
                    || contentType.startsWith("text/xml;")
                    || contentType.endsWith("+xml"));
    }

    private boolean textContentType(String contentType) {
        return contentType != null && contentType.startsWith("text/");
    }

    private void readBodyContent(TreeWriter tree, InputStream bodyStream, HttpMethodBase method) throws SaxonApiException, IOException {
        // Find the content type
        String contentType = getContentType(method);
        String charset = method.getResponseCharSet();
        String boundary = null;

        if (overrideContentType != null) {
            contentType = overrideContentType;
        }

        if (contentType.startsWith("multipart/")) {
            boundary = getContentBoundary(method);

            tree.addStartElement(XProcConstants.c_multipart);
            tree.addAttribute(_content_type, getFullContentType(method));
            tree.addAttribute(_boundary, boundary);
            tree.startContent();

            for (Header header : method.getResponseHeaders()) {
                // FIXME: what about parameters?
                if (header.getName().toLowerCase().equals("transfer-encoding")) {
                    // nop;
                } else {
                    tree.addStartElement(XProcConstants.c_header);
                    tree.addAttribute(_name, header.getName());
                    tree.addAttribute(_value, header.getValue());
                    tree.startContent();
                    tree.addEndElement();
                }
            }

            MIMEReader reader = new MIMEReader(bodyStream, boundary);
            boolean done = false;
            while (reader.readHeaders()) {
                Header pctype = reader.getHeader("Content-Type");
                Header pclen  = reader.getHeader("Content-Length");

                contentType = getHeaderValue(pctype);

                charset = getContentCharset(pctype);
                String partType = getHeaderValue(pctype);
                InputStream partStream = null;

                if (pclen != null) {
                    int len = Integer.parseInt(getHeaderValue(pclen));
                    partStream = reader.readBodyPart(len);
                } else {
                    partStream = reader.readBodyPart();
                }

                tree.addStartElement(XProcConstants.c_body);
                tree.addAttribute(_content_type, contentType);
                if (!xmlContentType(contentType) && !textContentType(contentType)) {
                    tree.addAttribute(_encoding, "base64");
                }
                tree.startContent();

                if (xmlContentType(partType)) {
                    BufferedReader preader = new BufferedReader(new InputStreamReader(partStream, charset));
                    // Read it as XML
                    SAXSource source = new SAXSource(new InputSource(preader));
                    DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
                    tree.addSubtree(builder.build(source));
                } else if (textContentType(partType)) {
                    BufferedReader preader = new BufferedReader(new InputStreamReader(partStream, charset));
                    // Read it as text
                    char buf[] = new char[bufSize];
                    int len = preader.read(buf, 0, bufSize);
                    while (len >= 0) {
                        // I'm unsure about this. If I'm reading text and injecting it into XML,
                        // I think I need to change CR/LF pairs (and CR not followed by LF) into
                        // plain LFs.

                        char fbuf[] = new char[bufSize];
                        char flen = 0;
                        for (int pos = 0; pos < len; pos++) {
                            if (buf[pos] == '\r') {
                                if (pos+1 == len) {
                                    // FIXME: Check for CR/LF pairs that cross a buffer boundary!
                                    // Assume it's part of a CR/LF pair...
                                } else {
                                    if (buf[pos+1] == '\n') {
                                        // nop
                                    } else {
                                        fbuf[flen++] = '\n';
                                    }
                                }
                            } else {
                                fbuf[flen++] = buf[pos];
                            }
                        }

                        tree.addText(new String(fbuf,0,flen));
                        len = preader.read(buf, 0, bufSize);
                    }
                } else {
                    // Read it as binary
                    byte bytes[] = new byte[bufSize];
                    int pos = 0;
                    int readLen = bufSize;
                    int len = partStream.read(bytes, 0, bufSize);
                    while (len >= 0) {
                        pos += len;
                        readLen -= len;
                        if (readLen == 0) {
                            tree.addText(Base64.encodeBytes(bytes));
                            pos = 0;
                            readLen = bufSize;
                        }

                        len = partStream.read(bytes, pos, readLen);
                    }

                    if (pos > 0) {
                        byte lastBytes[] = new byte[pos];
                        System.arraycopy(bytes, 0, lastBytes, 0, pos);
                        tree.addText(Base64.encodeBytes(lastBytes));
                    }

                    tree.addText("\n"); // FIXME: should we be doing this?
                }

                tree.addEndElement();
            }

            tree.addEndElement();
        } else {
            if (!detailed && xmlContentType(contentType)) {
                readBodyContentPart(tree, bodyStream, contentType, charset);
            } else {
                tree.addStartElement(XProcConstants.c_body);
                tree.addAttribute(_content_type, getFullContentType(method));
                if (!xmlContentType(contentType) && !textContentType(contentType)) {
                    tree.addAttribute(_encoding, "base64");
                }
                tree.startContent();
                readBodyContentPart(tree, bodyStream, contentType, charset);
                tree.addEndElement();
            }
        }
    }

    public void readBodyContentPart(TreeWriter tree, InputStream bodyStream, String contentType, String charset) throws SaxonApiException, IOException {
          if (xmlContentType(contentType)) {
              // Read it as XML
              SAXSource source = new SAXSource(new InputSource(bodyStream));
              DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
              tree.addSubtree(builder.build(source));
          } else if (textContentType(contentType)) {
              // Read it as text

              InputStreamReader reader = new InputStreamReader(bodyStream, charset);

              char buf[] = new char[bufSize];
              int len = reader.read(buf, 0, bufSize);
              while (len >= 0) {
                  tree.addText(new String(buf,0,len));
                  len = reader.read(buf, 0, bufSize);
              }
          } else {
              // Read it as binary
              byte bytes[] = new byte[bufSize];
              int pos = 0;
              int readLen = bufSize;
              int len = bodyStream.read(bytes, 0, bufSize);
              while (len >= 0) {
                  pos += len;
                  readLen -= len;
                  if (readLen == 0) {
                      System.err.println("Encoding " + readLen + " bytes (from " + pos + ")");
                      tree.addText(Base64.encodeBytes(bytes));
                      pos = 0;
                      readLen = bufSize;
                  }

                  len = bodyStream.read(bytes, pos, readLen);
              }

              if (pos > 0) {
                  byte lastBytes[] = new byte[pos];
                  System.arraycopy(bytes, 0, lastBytes, 0, pos);
                  tree.addText(Base64.encodeBytes(lastBytes));
              }

              tree.addText("\n"); // FIXME: should we be doing this?
          }
      }

    public void readBodyContentPart(TreeWriter tree, BufferedReader reader, String contentType, int contentLength, String charset, String boundary) throws SaxonApiException, IOException {
        String content = null;

        // FIXME: this must be wrong, contentLength is in bytes not characters for binary, right?
        if (contentLength >= 0) {
            char buf[] = new char[contentLength];
            int pos = 0;
            int left = contentLength;
            while (left > 0) {
                int len = reader.read(buf, pos, left);
                pos += len;
                left -= len;
            }

            String line = reader.readLine();
            while ("".equals(line)) {
                line = reader.readLine();
            }

            if (!boundary.equals(line)) {
                throw new UnsupportedOperationException("Expected boundary: " + line + " (expected " + boundary + ")");
            }
            content = new String(buf,0,contentLength);
        } else {
            content = "";
            String line = reader.readLine();
            while (!boundary.equals(line)) {
                content += line + "\n";
                line = reader.readLine();
            }
        }

        if (xmlContentType(contentType)) {
            // Read it as XML
            StringReader sreader = new StringReader(content);
            SAXSource source = new SAXSource(new InputSource(sreader));
            DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
            tree.addSubtree(builder.build(source));
        } else if (textContentType(contentType)) {
            // Read it as text
            tree.addText(content);
        } else {
            // Read it as binary
            byte bytes[] = content.getBytes(charset);
            tree.addText(Base64.encodeBytes(bytes));
        }
    }

    private void doFile() {
        // Find the content type
        String contentType = overrideContentType;
        String charset = "utf-8"; // FIXME: what's the real answer?

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(requestURI);

        try {
            File file = new File(requestURI.getPath());
            FileInputStream bodyStream = null;
            bodyStream = new FileInputStream(file);

            if (xmlContentType(contentType)) {
                readBodyContentPart(tree, bodyStream, contentType, charset);
            } else {
                tree.addStartElement(XProcConstants.c_body);
                tree.addAttribute(_content_type, contentType);
                if (!xmlContentType(contentType) && !textContentType(contentType)) {
                    tree.addAttribute(_encoding, "base64");
                }
                tree.startContent();
                readBodyContentPart(tree, bodyStream, contentType, charset);
                tree.addEndElement();
            }

            tree.endDocument();

            XdmNode doc = tree.getResult();
            result.write(doc);
        } catch (FileNotFoundException fnfe) {
            throw new XProcException(fnfe);
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }
}
