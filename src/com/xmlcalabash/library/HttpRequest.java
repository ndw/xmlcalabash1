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
 * https://runtime.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.xml.XMLConstants;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONTokener;
import org.xml.sax.InputSource;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.HttpUtils;
import com.xmlcalabash.util.JSONtoXML;
import com.xmlcalabash.util.MIMEReader;
import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.XMLtoJSON;

public class HttpRequest extends DefaultStep {
    private static final QName c_request = new QName("c", XProcConstants.NS_XPROC_STEP, "request");
    private static final QName cx_timeout = new QName("cx",XProcConstants.NS_CALABASH_EX,"timeout");
    private static final QName cx_cookies = new QName("cx",XProcConstants.NS_CALABASH_EX,"cookies");
    private static final QName cx_save_cookies = new QName("cx",XProcConstants.NS_CALABASH_EX,"save-cookies");
    private static final QName cx_use_cookies = new QName("cx",XProcConstants.NS_CALABASH_EX,"use-cookies");
    private static final QName cx_send_binary = new QName("cx", XProcConstants.NS_CALABASH_EX, "send-binary");

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
    public static final QName _disposition = new QName("", "disposition");
    public static final QName _status = new QName("", "status");
    public static final QName _boundary = new QName("", "boundary");
    public static final QName _charset = new QName("", "charset");

    private static final int bufSize = 912 * 8; // A multiple of 3, 4, and 75 for base64 line breaking

    private boolean statusOnly = false;
    private boolean detailed = false;
    private String method = null;
    private URI requestURI = null;
    private Vector<Header> headers = new Vector<Header> ();
    private String contentType = null;
    private String overrideContentType = null;
    private String headerContentType = null;
    private boolean encodeBinary = false;

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
            throw XProcException.stepError(40);
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
                    throw new XProcException(step.getNode(), "Unsupported attribute on c:request for p:http-request: " + name);
                }
            }
        }

        String send = step.getExtensionAttribute(cx_send_binary);
        encodeBinary = !"true".equals(send);

        method = start.getAttributeValue(_method);
        statusOnly = "true".equals(start.getAttributeValue(_status_only));
        detailed = "true".equals(start.getAttributeValue(_detailed));
        overrideContentType = start.getAttributeValue(_override_content_type);

        if (method == null) {
            throw XProcException.stepError(6);
        }

        if (statusOnly && !detailed) {
            throw XProcException.stepError(4);
        }

        if (start.getAttributeValue(_href) == null) {
            throw new XProcException(step.getNode(), "The 'href' attribute must be specified on c:request for p:http-request");
        }

        requestURI = start.getBaseURI().resolve(start.getAttributeValue(_href));

        if ("file".equals(requestURI.getScheme())) {
            doFile();
            return;
        }

        HttpParams params = new BasicHttpParams();
        HttpContext localContext = new BasicHttpContext();

        // The p:http-request step should follow redirect requests if they are returned by the server.
        params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);

        // What about cookies
        String saveCookieKey = step.getExtensionAttribute(cx_save_cookies);
        String useCookieKeys = step.getExtensionAttribute(cx_use_cookies);
        String cookieKey = step.getExtensionAttribute(cx_cookies);

        if (saveCookieKey == null) {
            saveCookieKey = cookieKey;
        }

        if (useCookieKeys == null) {
            useCookieKeys = cookieKey;
        }

        // If a redirect response includes cookies, those cookies should be forwarded
        // as appropriate to the redirected location when the redirection is followed.
        CookieStore cookieStore = new BasicCookieStore();
        if (useCookieKeys != null && useCookieKeys.equals(saveCookieKey)) {
            cookieStore = runtime.getCookieStore(useCookieKeys);
        } else if (useCookieKeys != null) {
            CookieStore useCookieStore = runtime.getCookieStore(useCookieKeys);
            for (Cookie cookie : useCookieStore.getCookies()) {
                cookieStore.addCookie(cookie);
            }
        }
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        // FIXME: Is browser compatability the right thing? It's the right thing for my unit test...
        params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

        String timeOutStr = step.getExtensionAttribute(cx_timeout);
        if (timeOutStr != null) {
            params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.parseInt(timeOutStr));
        }

        if (start.getAttributeValue(_username) != null) {
            String user = start.getAttributeValue(_username);
            String pass = start.getAttributeValue(_password);
            String meth = start.getAttributeValue(_auth_method);

            List<String> authpref;
            if ("basic".equalsIgnoreCase(meth)) {
            	authpref = Collections.singletonList(AuthPolicy.BASIC);
            } else if ("digest".equalsIgnoreCase(meth)) {
            	authpref = Collections.singletonList(AuthPolicy.DIGEST);
            } else {
                throw XProcException.stepError(3, "Unsupported auth-method: " + meth);
            }

            String host = requestURI.getHost();
            int port = requestURI.getPort();
            AuthScope scope = new AuthScope(host,port);

            UsernamePasswordCredentials cred = new UsernamePasswordCredentials(user, pass);

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(scope, cred);
            localContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);
            params.setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, true);
            params.setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);
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
                    String name = event.getAttributeValue(_name);
                    if (name == null) {
                        continue; // this can't happen, right?
                    }
                    if (name.toLowerCase().equals("content-type")) {
                        // We'll deal with the content-type header later
                        headerContentType = event.getAttributeValue(_value).toLowerCase();
                    } else {
                        headers.add(new BasicHeader(event.getAttributeValue(_name), event.getAttributeValue(_value)));
                    }
                } else if (XProcConstants.c_multipart.equals(event.getNodeName())
                           || XProcConstants.c_body.equals(event.getNodeName())) {
                    body = event;
                } else {
                    throw new UnsupportedOperationException("Unexpected request element: " + event.getNodeName());
                }
            }
        }

        String lcMethod = method.toLowerCase();

        // You can only have a body on PUT or POST
        if (body != null && !("put".equals(lcMethod) || "post".equals(lcMethod))) {
            throw XProcException.stepError(5);
        }

        HttpUriRequest httpRequest;
        HttpResponse httpResult = null;
        if ("get".equals(lcMethod)) {
            httpRequest = doGet();
        } else if ("post".equals(lcMethod)) {
            httpRequest = doPost(body);
        } else if ("put".equals(lcMethod)) {
            httpRequest = doPut(body);
        } else if ("head".equals(lcMethod)) {
            httpRequest = doHead();
        } else if ("delete".equals(lcMethod)) {
            httpRequest = doDelete();
        } else {
            throw new UnsupportedOperationException("Unrecognized http method: " + method);
        }

        TreeWriter tree = new TreeWriter(runtime);

        try {
            // Execute the method.
            HttpClient httpClient = runtime.getHttpClient();
            if (httpClient == null) {
                throw new XProcException("HTTP requests have been disabled");
            }
            httpRequest.setParams(params);
            httpResult = httpClient.execute(httpRequest, localContext);
            int statusCode = httpResult.getStatusLine().getStatusCode();
            HttpHost host = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            HttpUriRequest req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            URI root = new URI(host.getSchemeName(), null, host.getHostName(), host.getPort(), "/", null, null);
            tree.startDocument(root.resolve(req.getURI()));

            // Deal with cookies
            if (saveCookieKey != null) {
                runtime.setCookieStore(saveCookieKey, cookieStore);
            }

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

                for (Header header : httpResult.getAllHeaders()) {
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
                } else if (httpResult.getEntity() != null) {
                    // Read the response body.
                    InputStream bodyStream = httpResult.getEntity().getContent();
                    if (bodyStream != null) {
                        readBodyContent(tree, bodyStream, httpResult);
                    }
                }

                tree.addEndElement();
            } else {
                if (statusOnly) {
                    // Skip reading the result
                } else {
                    // Read the response body.
                    if (httpResult.getEntity() != null) {
                        InputStream bodyStream = httpResult.getEntity().getContent();
                        readBodyContent(tree, bodyStream, httpResult);
                    } else {
                        throw XProcException.dynamicError(6);
                    }
                }
            }
        } catch (XProcException e) {
            throw e;
        } catch (Exception e) {
            throw new XProcException(e);
        } finally {
            // Release the connection.
            if (httpResult != null) {
                EntityUtils.consumeQuietly(httpResult.getEntity());
            }
        }

        tree.endDocument();

        XdmNode resultNode = tree.getResult();

        result.write(resultNode);
    }

    private HttpGet doGet() {
        HttpGet method = new HttpGet(requestURI);

        for (Header header : headers) {
            method.addHeader(header);
        }

        return method;
    }

    private HttpHead doHead() {
        HttpHead method = new HttpHead(requestURI);

        for (Header header : headers) {
            method.addHeader(header);
        }

        return method;
    }

    private HttpDelete doDelete() {
        HttpDelete method = new HttpDelete(requestURI);

        for (Header header : headers) {
            method.addHeader(header);
        }

        return method;
    }

    private HttpPut doPut(XdmNode body) {
        HttpPut method = new HttpPut(requestURI);
        doPutOrPost(method,body);
        return method;
    }


    private HttpPost doPost(XdmNode body) {
        HttpPost method = new HttpPost(requestURI);
        doPutOrPost(method,body);
        return method;
    }

    private void doPutOrPost(HttpEntityEnclosingRequest method, XdmNode body) {
        if (XProcConstants.c_multipart.equals(body.getNodeName())) {
            doPutOrPostMultipart(method,body);
        } else {
            doPutOrPostSinglepart(method,body);
        }
    }

    private void doPutOrPostSinglepart(HttpEntityEnclosingRequest method, XdmNode body) {
        // ATTENTION: This doesn't handle multipart, that's done entirely separately

        // Check for consistency of content-type
        contentType = body.getAttributeValue(_content_type);
        if (contentType == null) {
            throw new XProcException(step.getNode(), "Content-type on c:body is required.");
        }

        String bodyId = body.getAttributeValue(_id);
        String bodyDescription = body.getAttributeValue(_description);
        String bodyDisposition = body.getAttributeValue(_disposition);

        boolean descriptionHeader = false;
        boolean idHeader = false;
        boolean dispositionHeader = false;

        if (bodyDescription != null) {
            for (Header header : headers) {
                if (header.getName().toLowerCase().equals("content-description")) {
                    String headDescription = header.getValue();
                    descriptionHeader = true;
                    if (!bodyDescription.equals(headDescription)) {
                        throw XProcException.stepError(20);
                    }
                }
            }

            if (!descriptionHeader) {
                headers.add(new BasicHeader("Content-Description", bodyDescription));
            }
        }

        if (bodyId != null) {
            for (Header header : headers) {
                if (header.getName().toLowerCase().equals("content-id")) {
                    String headId = header.getValue();
                    idHeader = true;
                    if (!bodyId.equals(headId)) {
                        throw XProcException.stepError(20);
                    }
                }
            }

            if (!idHeader) {
                headers.add(new BasicHeader("Content-Id", bodyId));
            }
        }

        if (bodyDisposition != null) {
            for (Header header : headers) {
                if (header.getName().toLowerCase().equals("content-disposition")) {
                    String headDisposition = header.getValue();
                    dispositionHeader = true;
                    if (!bodyDisposition.equals(headDisposition)) {
                        throw XProcException.stepError(20);
                    }
                }
            }

            if (!dispositionHeader) {
                headers.add(new BasicHeader("Content-Disposition", bodyDisposition));
            }
        }

        if (headerContentType != null && !headerContentType.equals(contentType.toLowerCase())) {
            throw XProcException.stepError(20);
        }

        for (Header header : headers) {
            method.addHeader(header);
        }

        // FIXME: This sucks rocks. I want to write the data to be posted, not provide some way to read it
        String postContent = null;
        String encoding = body.getAttributeValue(_encoding);

        if (encoding != null && !"base64".equals(encoding)) {
            throw XProcException.stepError(52);
        }

        try {
            if ("base64".equals(encoding)) {
                String charset = body.getAttributeValue(_charset);
                // FIXME: is utf-8 the right default?
                if (charset == null) { charset = "utf-8"; }

                // Make sure it's all characters
                XdmSequenceIterator iter = body.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode node = (XdmNode) iter.next();
                    if (node.getNodeKind() != XdmNodeKind.TEXT) {
                        throw XProcException.stepError(28);
                    }
                }

                String escapedContent = decodeBase64(body, charset);
                StringWriter writer = new StringWriter();
                writer.write(escapedContent);
                writer.close();
                postContent = writer.toString();
            } else {
                if (jsonContentType(contentType)) {
                    postContent = XMLtoJSON.convert(body);
                } else if (xmlContentType(contentType)) {
                    Serializer serializer = makeSerializer();

                    try {
                        S9apiUtils.assertDocumentContent(body.axisIterator(Axis.CHILD));
                    } catch (XProcException xe) {
                        throw XProcException.stepError(22);
                    }

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
                        if (node.getNodeKind() != XdmNodeKind.TEXT) {
                            throw XProcException.stepError(28);
                        }
                        writer.write(node.getStringValue());
                    }
                    writer.close();
                    postContent = writer.toString();
                }
            }

            StringEntity requestEntity = new StringEntity(postContent, ContentType.create(contentType, "UTF-8"));
            method.setEntity(requestEntity);

        } catch (IOException ioe) {
            throw new XProcException(ioe);
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
    }

    private void doPutOrPostMultipart(HttpEntityEnclosingRequest method, XdmNode multipart) {
        // The Apache HTTP libraries just don't handle this case...we treat it as a "single part"
        // and build the body ourselves, using the boundaries etc.

        // Check for consistency of content-type
        contentType = multipart.getAttributeValue(_content_type);
        if (contentType == null) {
            contentType = "multipart/mixed";
        }

        if (headerContentType != null && !headerContentType.equals(contentType.toLowerCase())) {
            throw XProcException.stepError(20);
        }

        if (!contentType.startsWith("multipart/")) {
            throw new UnsupportedOperationException("Multipart content-type must be multipart/...");
        }

        for (Header header : headers) {
            method.addHeader(header);
        }

        String boundary = multipart.getAttributeValue(_boundary);

        if (boundary == null) {
            throw new XProcException(step.getNode(), "A boundary value must be specified on c:multipart");
        }

        if (boundary.startsWith("--")) {
            throw XProcException.stepError(2);
        }

        String q = "\"";
        if (boundary.contains(q)) {
            q = "'";
        }
        if (boundary.contains(q)) {
            q = "";
        }

        String multipartContentType = contentType + "; boundary=" + q + boundary + q;

        // FIXME: This sucks rocks. I want to write the data to be posted, not provide some way to read it
        MessageBytes byteContent = new MessageBytes();
        byteContent.append("This is a multipart message.\r\n");
        //String postContent = "This is a multipart message.\r\n";
        for (XdmNode body : new RelevantNodes(runtime, multipart, Axis.CHILD)) {
            if (!XProcConstants.c_body.equals(body.getNodeName())) {
                throw new XProcException(step.getNode(), "A c:multipart may only contain c:body elements.");
            }

            String bodyContentType = body.getAttributeValue(_content_type);
            if (bodyContentType == null) {
                throw new XProcException(step.getNode(), "Content-type on c:body is required.");
            }

            String bodyId = body.getAttributeValue(_id);
            String bodyDescription = body.getAttributeValue(_description);
            String bodyDisposition = body.getAttributeValue(_disposition);

            String bodyCharset = HttpUtils.getCharset(bodyContentType);

            if (bodyContentType.contains(";")) {
                int pos = bodyContentType.indexOf(";");
                bodyContentType = bodyContentType.substring(0, pos);
            }

            String bodyEncoding = body.getAttributeValue(_encoding);
            if (bodyEncoding != null && !"base64".equals(bodyEncoding)) {
                throw new UnsupportedOperationException("The '" + bodyEncoding + "' encoding is not supported");
            }

            if (bodyCharset != null) {
                bodyContentType += "; charset=" + bodyCharset;
            } else {
                // Is utf-8 the right default? What about the image/ case? 
                bodyContentType += "; charset=utf-8";
            }

            //postContent += "--" + boundary + "\r\n";
            //postContent += "Content-Type: " + bodyContentType + "\r\n";
            byteContent.append("--" + boundary + "\r\n");
            byteContent.append("Content-Type: " + bodyContentType + "\r\n");

            if (bodyDescription != null) {
                //postContent += "Content-Description: " + bodyDescription + "\r\n";
                byteContent.append("Content-Description: " + bodyDescription + "\r\n");
            }
            if (bodyId != null) {
                //postContent += "Content-ID: " + bodyId + "\r\n";
                byteContent.append("Content-ID: " + bodyId + "\r\n");
            }
            if (bodyDisposition != null) {
                //postContent += "Content-Disposition: " + bodyDisposition + "\r\n";
                byteContent.append("Content-Disposition: " + bodyDisposition + "\r\n");
            }
            if (bodyEncoding != null) {
                //postContent += "Content-Transfer-Encoding: " + bodyEncoding + "\r\n";
                if (encodeBinary) {
                    byteContent.append("Content-Transfer-Encoding: " + bodyEncoding + "\r\n");
                }
            }
            //postContent += "\r\n";
            byteContent.append("\r\n");

            try {
                if (xmlContentType(bodyContentType)) {
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
                    //postContent += writer.toString();
                    byteContent.append(writer.toString());
                } else if (jsonContentType(contentType)) {
                    byteContent.append(XMLtoJSON.convert(body));
                } else if (!encodeBinary && "base64".equals(bodyEncoding)) {
                    byte[] decoded = Base64.decode(body.getStringValue());
                    byteContent.append(decoded, decoded.length);
                } else {
                    StringWriter writer = new StringWriter();
                    XdmSequenceIterator iter = body.axisIterator(Axis.CHILD);
                    while (iter.hasNext()) {
                        XdmNode node = (XdmNode) iter.next();
                        if (node.getNodeKind() != XdmNodeKind.TEXT) {
                            throw XProcException.stepError(28);
                        }
                        writer.write(node.getStringValue());
                    }
                    writer.close();
                    //postContent += writer.toString();
                    byteContent.append(writer.toString());
                }

                //postContent += "\r\n";
                byteContent.append("\r\n");
            } catch (IOException ioe) {
                throw new XProcException(ioe);
            } catch (SaxonApiException sae) {
                throw new XProcException(sae);
            }
        }

        //postContent += "--" + boundary + "--\r\n";
        byteContent.append("--" + boundary + "--\r\n");

        ByteArrayEntity requestEntity = new ByteArrayEntity(byteContent.content(), ContentType.create(multipartContentType));
        //StringRequestEntity requestEntity = new StringRequestEntity(postContent, multipartContentType, null);
        method.setEntity(requestEntity);
    }

    private String getFullContentType(HttpResponse method) {
        Header contentTypeHeader = method.getLastHeader("Content-Type");
        return getFullContentType(contentTypeHeader);
    }

    private String getFullContentType(Header contentTypeHeader) {
        if (contentTypeHeader == null) {
            // This should never happen, but if it does...
            return "application/octet-stream";
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

    private String getContentType(HttpResponse method) {
        Header contentTypeHeader = method.getLastHeader("Content-Type");
        String contentType = getContentType(contentTypeHeader);
        if (contentType == null) {
            // This should never happen either...
            return "application/octet-stream";
        } else {
            return contentType;
        }
    }

    private String getContentType(Header contentTypeHeader) {
        return getHeaderValue(contentTypeHeader);
    }

    private String getContentBoundary(HttpResponse method) {
        Header contentTypeHeader = method.getLastHeader("Content-Type");
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

        NameValuePair boundary = contentTypes[0].getParameterByName("boundary");
        return boundary == null ? null : boundary.getValue();
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
        return HttpUtils.xmlContentType(contentType);
    }

    private boolean jsonContentType(String contentType) {
        return runtime.transparentJSON() && HttpUtils.jsonContentType(contentType);
    }

    private boolean textContentType(String contentType) {
        return HttpUtils.textContentType(contentType);
    }

    private void readBodyContent(TreeWriter tree, InputStream bodyStream, HttpResponse method) throws SaxonApiException, IOException {
        String contentType = getFullContentType(method);
        Charset cs = ContentType.getOrDefault(method.getEntity()).getCharset();
        String charset = cs == null ? Consts.ISO_8859_1.name() : cs.name();
        String boundary = getContentBoundary(method);

        if (overrideContentType != null) {
            contentType = overrideContentType;
        }

        if (contentType.startsWith("multipart/")) {
            tree.addStartElement(XProcConstants.c_multipart);
            tree.addAttribute(_content_type, contentType);
            tree.addAttribute(_boundary, boundary);
            tree.startContent();
            
            readMultipartContent(tree, bodyStream, boundary);

            tree.addEndElement();
        } else {
            if (!detailed && (xmlContentType(contentType) || jsonContentType(contentType))) {
                readBodyContentPart(tree, bodyStream, contentType, charset);
            } else {
                tree.addStartElement(XProcConstants.c_body);
                tree.addAttribute(_content_type, contentType);
                if (!xmlContentType(contentType) && !textContentType(contentType) && !jsonContentType(contentType)) {
                    tree.addAttribute(_encoding, "base64");
                }
                tree.startContent();
                readBodyContentPart(tree, bodyStream, contentType, charset);
                tree.addEndElement();
            }
        }
    }

    private void readMultipartContent(TreeWriter tree, InputStream bodyStream, String boundary) throws IOException, SaxonApiException {
        MIMEReader reader = new MIMEReader(bodyStream, boundary);
        boolean done = false;
        while (reader.readHeaders()) {
            Header pctype = reader.getHeader("Content-Type");
            Header pclen  = reader.getHeader("Content-Length");

            contentType = getHeaderValue(pctype);

            String charset = getContentCharset(pctype);
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
                tree.addSubtree(runtime.parse(new InputSource(preader)));
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
    }

    public void readBodyContentPart(TreeWriter tree, InputStream bodyStream, String contentType, String charset) throws SaxonApiException, IOException {
        if (xmlContentType(contentType)) {
            // Read it as XML
            tree.addSubtree(runtime.parse(new InputSource(bodyStream)));
        } else if (textContentType(contentType)) {
            // Read it as text

            InputStreamReader reader = new InputStreamReader(bodyStream, charset);

            char buf[] = new char[bufSize];
            int len = reader.read(buf, 0, bufSize);
            while (len >= 0) {
                String s = new String(buf,0,len);
                tree.addText(s);
                len = reader.read(buf, 0, bufSize);
            }
        } else if (jsonContentType(contentType)) {
            InputStreamReader reader = new InputStreamReader(bodyStream);
            JSONTokener jt = new JSONTokener(reader);
            XdmNode jsonDoc = JSONtoXML.convert(runtime.getProcessor(), jt, runtime.jsonFlavor());
            tree.addSubtree(jsonDoc);
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
                    String encoded = Base64.encodeBytes(bytes);
                    tree.addText(encoded);
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

    private String extractText(XdmNode doc) {
        String content = "";
        XdmSequenceIterator iter = doc.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            if (child.getNodeKind() == XdmNodeKind.ELEMENT || child.getNodeKind() == XdmNodeKind.TEXT) {
                content += child.getStringValue();
            }
        }
        return content;
    }

    private String decodeBase64(XdmNode doc, String charset) {
        String content = extractText(doc);
        byte[] decoded = Base64.decode(content);
        try {
            return new String(decoded, charset);
        } catch (UnsupportedEncodingException uee) {
            throw XProcException.stepError(10, uee);
        }
    }

    private void doFile() {
        // Find the content type
        String contentType = overrideContentType;

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // FIXME: Is ISO-8859-1 the right default?
        String charset = HttpUtils.getCharset(contentType, "ISO-8859-1");

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

    private class MessageBytes {
        int chunkSize = 8192;
        byte[] byteContent = new byte[chunkSize];
        int pos = 0;

        public MessageBytes() {
        }

        public void append(String string) {
            try {
                byte[] bytes = string.getBytes("US-ASCII");
                append(bytes, bytes.length);
            } catch (UnsupportedEncodingException uee) {
                // This never happens!
                throw new XProcException(uee);
            }
        }

        public void append(byte[] bytes, int size) {
            if (pos + bytes.length > byteContent.length) {
                byte[] newBytes = new byte[byteContent.length + bytes.length + chunkSize];
                System.arraycopy(byteContent, 0, newBytes, 0, byteContent.length);
                byteContent = newBytes;
            }
            System.arraycopy(bytes, 0, byteContent, pos, bytes.length);
            pos += bytes.length;
        }

        public byte[] content() {
            byte[] bytes = new byte[pos];
            System.arraycopy(byteContent, 0, bytes, 0, pos);
            return bytes;
        }
    }
}
