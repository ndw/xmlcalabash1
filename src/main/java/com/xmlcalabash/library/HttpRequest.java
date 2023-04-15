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

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataReader;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.*;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.om.SingletonAttributeMap;
import net.sf.saxon.s9api.*;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONTokener;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;


@XMLCalabash(
        name = "p:http-request",
        type = "{http://www.w3.org/ns/xproc}http-request")

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

    private boolean detailed = false;
    private boolean sendAuthorization = false;
    private URI requestURI = null;
    private Vector<Header> headers = new Vector<Header> ();
    private String overrideContentType = null;
    private String headerContentType = null;
    private boolean encodeBinary = false;
    private HttpClientBuilder builder = null;

    private ReadablePipe source = null;
    private WritablePipe result = null;

    /* Creates a new instance of HttpRequest */
    public HttpRequest(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
        builder = HttpClientBuilder.create();
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
        builder = HttpClientBuilder.create();
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode requestDoc = source.read();
        XdmNode start = S9apiUtils.getDocumentElement(requestDoc);
        assert start != null;

        if (!c_request.equals(start.getNodeName())) {
            throw XProcException.stepError(40);
        }

        // Check for valid attributes
        XdmSequenceIterator<XdmNode> iter = start.axisIterator(Axis.ATTRIBUTE);
        boolean ok = true;
        while (iter.hasNext()) {
            XdmNode attr = iter.next();
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

        boolean statusOnly = "true".equals(start.getAttributeValue(_status_only));
        String method = start.getAttributeValue(_method);
        detailed = "true".equals(start.getAttributeValue(_detailed));
        sendAuthorization = "true".equals(start.getAttributeValue(_send_authorization));
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

        String scheme = requestURI.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            doFile(start.getAttributeValue(_href), start.getBaseURI().toASCIIString());
            return;
        }

        RequestConfig.Builder rqbuilder = RequestConfig.custom();
        rqbuilder.setCookieSpec(CookieSpecs.DEFAULT);

        HttpClientContext localContext = HttpClientContext.create();

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
        builder.setDefaultCookieStore(cookieStore);

        String timeOutStr = step.getExtensionAttribute(cx_timeout);
        if (timeOutStr != null) {
            rqbuilder.setSocketTimeout(Integer.parseInt(timeOutStr));
        }
        builder.setDefaultRequestConfig(rqbuilder.build());

        if (start.getAttributeValue(_username) != null) {
            String user = start.getAttributeValue(_username);
            String pass = start.getAttributeValue(_password);
            String meth = start.getAttributeValue(_auth_method);

            String host = requestURI.getHost();
            int port = requestURI.getPort();
            AuthScope scope = new AuthScope(host,port); // Or this? new AuthScope(null, AuthScope.ANY_PORT)
            BasicCredentialsProvider bCredsProvider = new BasicCredentialsProvider();
            bCredsProvider.setCredentials(scope, new UsernamePasswordCredentials(user, pass));

            List<String> authpref;
            if ("basic".equalsIgnoreCase(meth)) {
                authpref = Collections.singletonList(AuthSchemes.BASIC);

                if (sendAuthorization) {
                    // See https://stackoverflow.com/questions/20914311/httpclientbuilder-basic-auth
                    AuthCache authCache = new BasicAuthCache();
                    BasicScheme basicAuth = new BasicScheme();
                    authCache.put(new HttpHost(host, port), basicAuth);

                    localContext.setCredentialsProvider(bCredsProvider);
                    localContext.setAuthCache(authCache);
                }
            } else if ("digest".equalsIgnoreCase(meth)) {
                authpref = Collections.singletonList(AuthSchemes.DIGEST);
            } else {
                throw XProcException.stepError(3, "Unsupported auth-method: " + meth);
            }

            rqbuilder.setProxyPreferredAuthSchemes(authpref);
            builder.setDefaultCredentialsProvider(bCredsProvider);
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

        String lcMethod = method.toUpperCase();

        // You cannot have a body on HEAD or GET
        if (body != null && ("HEAD".equals(lcMethod) || "GET".equals(lcMethod))) {
            throw XProcException.stepError(5);
        }

        HttpUriRequest httpRequest;
        HttpResponse httpResult = null;
        switch (lcMethod) {
            case "GET":
                httpRequest = doGet();
                break;
            case "POST":
                httpRequest = doPost(body);
                break;
            case "PUT":
                httpRequest = doPut(body);
                break;
            case "PATCH":
                httpRequest = doPatch(body);
                break;
            case "HEAD":
                httpRequest = doHead();
                break;
            case "DELETE":
                httpRequest = doDelete();
                break;
            default:
                if (body != null) {
                    httpRequest = doGenericMethodWithBody(lcMethod, body);
                } else {
                    httpRequest = doGenericMethod(lcMethod);
                }        }

        TreeWriter tree = new TreeWriter(runtime);

        try {
            // Execute the method.
            builder.setRetryHandler(new StandardHttpRequestRetryHandler(3, false));

            for (String pscheme : runtime.getConfiguration().proxies.keySet()) {
                String proxy = runtime.getConfiguration().proxies.get(pscheme);
                int pos = proxy.indexOf(":");
                String host = proxy.substring(0, pos);
                int port = Integer.parseInt(proxy.substring(pos+1));
                HttpHost httpProxy = new HttpHost(host, port, pscheme);
                builder.setProxy(httpProxy);
            }

            HttpClient httpClient = builder.build();
            if (httpClient == null) {
                throw new XProcException("HTTP requests have been disabled");
            }

            httpResult = httpClient.execute(httpRequest, localContext);
            int statusCode = httpResult.getStatusLine().getStatusCode();
            HttpHost host = (HttpHost) localContext.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
            HttpUriRequest req = (HttpUriRequest) localContext.getAttribute(HttpCoreContext.HTTP_REQUEST);
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
                tree.addStartElement(XProcConstants.c_response, SingletonAttributeMap.of(TypeUtils.attributeInfo(_status, ""+statusCode)));

                for (Header header : httpResult.getAllHeaders()) {
                    // I don't understand why/how HeaderElement parsing works. I get very weird results.
                    // So I'm just going to go the long way around...
                    String h = header.toString();
                    int cp = h.indexOf(":");
                    String name = header.getName();
                    String value = h.substring(cp+1).trim();
                    AttributeMap attr = EmptyAttributeMap.getInstance();

                    attr = attr.put(TypeUtils.attributeInfo(_name, name));
                    attr = attr.put(TypeUtils.attributeInfo(_value, value));
                    tree.addStartElement(XProcConstants.c_header, attr);
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

                        // 15 Apr 2023, I'm suddenly getting an exception:
                        //
                        // org.apache.http.ConnectionClosedException: Premature end of chunk coded message body: closing chunk expected
                        //
                        // on some connections. (Some connections to tests.xproc.org, for example). I've no idea how to
                        // fix them, and I see a few places on the web where others are having this problem.
                        // Apparently when http uses a chunked encoding it's supposed to send a final zero-length
                        // chunk and this error arises if the server doesn't. I'm just going to paper over it because
                        // what else can I do?

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            byte[] buf = new byte[4096];
                            int len = bodyStream.read(buf);
                            while (len >= 0) {
                                baos.write(buf, 0, len);
                                len = bodyStream.read(buf);
                            }
                        } catch (ConnectionClosedException ex) {
                            if (!ex.getMessage().contains("closing chunk expected")) {
                                throw ex;
                            }
                        }
                        bodyStream = new ByteArrayInputStream(baos.toByteArray());

                        readBodyContent(tree, bodyStream, httpResult);
                    } else {
                        throw XProcException.dynamicError(6, "Reading HTTP response on " + getStep().getName());
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

    private HttpGenericMethod doGenericMethod(String methodName) {
        HttpGenericMethod method = new HttpGenericMethod(methodName, requestURI);

        for (Header header : headers) {
            method.addHeader(header);
        }

        return method;
    }

    private HttpGenericMethodWithBody doGenericMethodWithBody(String methodName, XdmNode body) {
        HttpGenericMethodWithBody method = new HttpGenericMethodWithBody(methodName, requestURI);
        doPutOrPost(method,body);
        return method;
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
    
    private HttpPatch doPatch(XdmNode body) {
        HttpPatch method = new HttpPatch(requestURI);
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
        String contentType = body.getAttributeValue(_content_type);
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

        String encoding = body.getAttributeValue(_encoding);
        if (encoding != null && !"base64".equals(encoding)) {
            throw XProcException.stepError(52);
        }

        HttpEntity requestEntity = null;

        try {
            if ("base64".equals(encoding)) {
                String charset = body.getAttributeValue(_charset);

                // See also: https://github.com/ndw/xmlcalabash1/pull/241 and
                // https://github.com/ndw/xmlcalabash1/issues/242
                //
                // The PR proposes ignoring the charset and treating the data as binary.
                // That's clearly necessary for the case where the data *is* binary.
                // However, if the base64 encoded element *has* a charset parameter,
                // we must pass that through (because text/html data often winds
                // up base64 encoded with a charset.
                //
                // In other words, the answer to the comment that used to be here,
                // "is utf-8 the right default?", is "no."

                String content = extractText(body);
                byte[] decoded = Base64.decode(content);

                if (charset == null) {
                    // Treat as binary
                    requestEntity = new ByteArrayEntity(decoded, ContentType.create(contentType));
                } else {
                    // Treat as encoded characters
                    requestEntity = new ByteArrayEntity(decoded, ContentType.create(contentType, charset));
                }
            } else {
                if (jsonContentType(contentType)) {
                    requestEntity = new StringEntity(XMLtoJSON.convert(body), ContentType.create(contentType, "UTF-8"));
                } else if (xmlContentType(contentType)) {
                    Serializer serializer = makeSerializer();

                    try {
                        S9apiUtils.assertDocumentContent(body.axisIterator(Axis.CHILD));
                    } catch (XProcException xe) {
                        throw XProcException.stepError(22);
                    }

                    Vector<XdmNode> content = new Vector<XdmNode> ();
                    XdmSequenceIterator<XdmNode> iter = body.axisIterator(Axis.CHILD);
                    while (iter.hasNext()) {
                        XdmNode node = iter.next();
                        content.add(node);
                    }

                    // FIXME: set serializer properties appropriately!
                    StringWriter writer = new StringWriter();
                    serializer.setOutputWriter(writer);
                    S9apiUtils.serialize(runtime, content, serializer);
                    writer.close();
                    requestEntity = new StringEntity(writer.toString(), ContentType.create(contentType, "UTF-8"));
                } else {
                    requestEntity = new StringEntity(extractText(body), ContentType.create(contentType, "UTF-8"));
                }
            }

            method.setEntity(requestEntity);

        } catch (IOException | SaxonApiException ioe) {
            throw new XProcException(ioe);
        }
    }

    private void doPutOrPostMultipart(HttpEntityEnclosingRequest method, XdmNode document) {
        // Check for consistency of content-type
        String contentType = document.getAttributeValue(_content_type);
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

        String boundary = document.getAttributeValue(_boundary);
        if (boundary == null) {
            throw new XProcException(step.getNode(), "A boundary value must be specified on c:multipart");
        }

        if (boundary.startsWith("--")) {
            throw XProcException.stepError(2);
        }

        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setBoundary(boundary);
        entityBuilder.setContentType(ContentType.create(contentType));
        int partCount = 0;

        for (XdmNode body : new AxisNodes(document, Axis.CHILD, AxisNodes.SIGNIFICANT)) {
            if (!XProcConstants.c_body.equals(body.getNodeName())) {
                throw new XProcException(step.getNode(), "A c:multipart may only contain c:body elements.");
            }

            String bodyContentType = body.getAttributeValue(_content_type);
            if (bodyContentType == null) {
                throw new XProcException(step.getNode(), "Content-type on c:body is required.");
            }

            partCount++;
            String bodyId = body.getAttributeValue(_id);
            String bodyDescription = body.getAttributeValue(_description);
            String bodyDisposition = body.getAttributeValue(_disposition);

            if (bodyContentType.contains(";")) {
                int pos = bodyContentType.indexOf(";");
                bodyContentType = bodyContentType.substring(0, pos);
            }

            String bodyEncoding = body.getAttributeValue(_encoding);
            if (bodyEncoding != null && !"base64".equals(bodyEncoding)) {
                throw new UnsupportedOperationException("The '" + bodyEncoding + "' encoding is not supported");
            }

            String bodyCharset = HttpUtils.getCharset(bodyContentType);
            if (bodyCharset == null) {
                bodyCharset = "UTF-8";
            }

            FormBodyPartBuilder part = FormBodyPartBuilder.create();
            ContentType partCT = ContentType.create(bodyContentType, bodyCharset);

            part.setName("part" + partCount);
            if (bodyDescription != null) {
                part = part.addField("Content-Description", bodyDescription);
            }
            if (bodyId != null) {
                part = part.addField("Content-Id", bodyId);
            }
            if (bodyDisposition != null) {
                part = part.addField("Content-Disposition", bodyDisposition);
            }
            if (bodyEncoding != null) {
                if (encodeBinary) {
                    part = part.addField("Content-Tranfer-Encoding", bodyEncoding);
                }
            }

            try {
                if (xmlContentType(bodyContentType)) {
                    Serializer serializer = makeSerializer();

                    Vector<XdmNode> content = new Vector<> ();
                    XdmSequenceIterator<XdmNode> iter = body.axisIterator(Axis.CHILD);
                    while (iter.hasNext()) {
                        XdmNode node = iter.next();
                        content.add(node);
                    }

                    // FIXME: set serializer properties appropriately!
                    StringWriter writer = new StringWriter();
                    serializer.setOutputWriter(writer);
                    S9apiUtils.serialize(runtime, content, serializer);
                    writer.close();

                    part = part.setBody(new StringBody(writer.toString(), partCT));
                    entityBuilder = entityBuilder.addPart(part.build());
                } else if (jsonContentType(contentType)) {
                    part.setBody(new StringBody(XMLtoJSON.convert(body), partCT));
                    entityBuilder = entityBuilder.addPart(part.build());
                } else if (!encodeBinary && "base64".equals(bodyEncoding)) {
                    byte[] decoded = Base64.decode(body.getStringValue());
                    part.setBody(new ByteArrayBody(decoded, partCT, "fred"));
                    entityBuilder = entityBuilder.addPart(part.build());
                } else {
                    part.setBody(new StringBody(extractText(body), partCT));
                    entityBuilder = entityBuilder.addPart(part.build());
                }
            } catch (IOException | SaxonApiException ioe) {
                throw new XProcException(ioe);
            }
        }

        method.setEntity(entityBuilder.build());
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

        StringBuilder ctype = new StringBuilder(contentTypes[0].getName());
        NameValuePair[] params = contentTypes[0].getParameters();
        if (params != null) {
            for (NameValuePair pair : params) {
                ctype.append(";").append(pair.getName()).append("=\"").append(pair.getValue()).append("\"");
            }
        }

        return ctype.toString();
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
            AttributeMap attr = EmptyAttributeMap.getInstance();
            attr = attr.put(TypeUtils.attributeInfo(_content_type, contentType));
            attr = attr.put(TypeUtils.attributeInfo(_boundary, boundary));
            tree.addStartElement(XProcConstants.c_multipart, attr);

            readMultipartContent(tree, bodyStream, boundary);

            tree.addEndElement();
        } else {
            if (!detailed && (xmlContentType(contentType) || jsonContentType(contentType))) {
                readBodyContentPart(tree, bodyStream, contentType, charset);
            } else {
                AttributeMap attr = EmptyAttributeMap.getInstance();
                attr = attr.put(TypeUtils.attributeInfo(_content_type, contentType));
                if (!xmlContentType(contentType) && !textContentType(contentType) && !jsonContentType(contentType)) {
                    attr = attr.put(TypeUtils.attributeInfo(_encoding, "base64"));
                }

                tree.addStartElement(XProcConstants.c_body, attr);
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

            String contentType = getHeaderValue(pctype);

            String charset = getContentCharset(pctype);
            String partType = getHeaderValue(pctype);
            InputStream partStream = null;

            if (pclen != null) {
                int len = Integer.parseInt(getHeaderValue(pclen));
                partStream = reader.readBodyPart(len);
            } else {
                partStream = reader.readBodyPart();
            }

            AttributeMap attr = EmptyAttributeMap.getInstance();
            attr = attr.put(TypeUtils.attributeInfo(_content_type, contentType));
            if (!xmlContentType(contentType) && !textContentType(contentType)) {
                attr = attr.put(TypeUtils.attributeInfo(_encoding, "base64"));
            }

            tree.addStartElement(XProcConstants.c_body, attr);

            if (xmlContentType(partType)) {
                BufferedReader preader = new BufferedReader(new InputStreamReader(partStream, charset));
                // Read it as XML
                tree.addSubtree(runtime.parse(new InputSource(preader)));
            } else if (textContentType(partType)) {
                BufferedReader preader = new BufferedReader(new InputStreamReader(partStream, charset));
                // Read it as text
                char[] buf = new char[bufSize];
                int len = preader.read(buf, 0, bufSize);
                while (len >= 0) {
                    // I'm unsure about this. If I'm reading text and injecting it into XML,
                    // I think I need to change CR/LF pairs (and CR not followed by LF) into
                    // plain LFs.

                    char[] fbuf = new char[bufSize];
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
                byte[] bytes = new byte[bufSize];
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
                    byte[] lastBytes = new byte[pos];
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

            char[] buf = new char[bufSize];
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
            byte[] bytes = new byte[bufSize];
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
                byte[] lastBytes = new byte[pos];
                System.arraycopy(bytes, 0, lastBytes, 0, pos);
                tree.addText(Base64.encodeBytes(lastBytes));
            }

            tree.addText("\n"); // FIXME: should we be doing this?
        }
    }

    private String extractText(XdmNode doc) {
        StringBuilder content = new StringBuilder();

        XdmSequenceIterator<XdmNode> iter = doc.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = iter.next();
            if (child.getNodeKind() != XdmNodeKind.TEXT) {
                throw XProcException.stepError(28);
            }
            content.append(child.getStringValue());
        }

        return content.toString();
    }

    private void doFile(String href, String base) {
        try {
            DataStore store = runtime.getDataStore();
            store.readEntry(href, base, "application/xml, text/xml, */*", overrideContentType, new DataReader() {
                public void load(URI id, String contentType, InputStream bodyStream, long len)
                        throws IOException {
                    // Get the default charset from the file.encoding system property.
                    // Fall back to UTF-8 if that's not set.
                    String defCharset = System.getProperty("file.encoding","UTF-8");
                    String charset = HttpUtils.getCharset(contentType, defCharset);

                    TreeWriter tree = new TreeWriter(runtime);
                    tree.startDocument(id);

                    try {
                        if (xmlContentType(contentType)) {
                            readBodyContentPart(tree, bodyStream, contentType, charset);
                        } else {
                            AttributeMap attr = EmptyAttributeMap.getInstance();
                            attr = attr.put(TypeUtils.attributeInfo(_content_type, contentType));
                            if (!xmlContentType(contentType) && !textContentType(contentType)) {
                                attr = attr.put(TypeUtils.attributeInfo(_encoding, "base64"));
                            }

                            tree.addStartElement(XProcConstants.c_body, attr);
                            readBodyContentPart(tree, bodyStream, contentType, charset);
                            tree.addEndElement();
                        }

                        tree.endDocument();

                        XdmNode doc = tree.getResult();
                        result.write(doc);
                    } catch (SaxonApiException sae) {
                        throw new XProcException(sae);
                    }
                }
            });
        } catch (IOException fnfe) {
            throw new XProcException(fnfe);
        }
    }

    private class HttpGenericMethod extends HttpEntityEnclosingRequestBase {
        private String method;
        public HttpGenericMethod(String method, URI requestURI) {
            super();
            this.method = method;
            setURI(requestURI);
        }
        @Override
        public String getMethod() {
            return method;
        }
    }

    private class HttpGenericMethodWithBody extends HttpEntityEnclosingRequestBase {
        private String method;
        public HttpGenericMethodWithBody(String method, URI requestURI) {
            super();
            this.method = method;
            setURI(requestURI);
        }
        @Override
        public String getMethod() {
            return method;
        }
    }
}
