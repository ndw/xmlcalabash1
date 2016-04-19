/*
 * HttpClientDataStore.java
 *
 * Copyright 2013 3 Round Stones Inc.
 * Some rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 * Uses {@link HttpClient} to implement the interface. HTTP redirects are
 * followed transparently, unless disabled by {@link #setHttpParams(HttpParams)}.
 * When writing documents, the document is sent to the target URI via HTTP POST.
 * If the server response with a Location in the response header, that is
 * assumed to be the URI of the created document.
 * 
 * @author James Leigh &lt;james@3roundstones.com&gt;
 * 
 */
public class HttpClientDataStore implements DataStore {
	private final DataStore fallback;
	private HttpClient client;
	private HttpParams params;

	public HttpClientDataStore(HttpClient client, DataStore fallback) {
		super();
		this.client = client;
		this.fallback = fallback;
	}

	public synchronized HttpClient getHttpClient() {
		return client;
	}

	public synchronized void setHttpClient(HttpClient client) {
		this.client = client;
	}

	public synchronized HttpParams getHttpParams() {
		if (params == null) {
			params = new BasicHttpParams();
			params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
		}
		return params;
	}

	public synchronized void setHttpParams(HttpParams params) {
		this.params = params;
	}

	public URI writeEntry(String href, String base, final String media,
			final DataWriter handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		String sch = uri.getScheme();
		if ("http".equalsIgnoreCase(sch) || "https".equalsIgnoreCase(sch)) {
			final HttpContext localContext = new BasicHttpContext();
			HttpPost post = new HttpPost(uri);
			post.setHeader("Content-Type", media);
			post.setEntity(new AbstractHttpEntity() {
				public void writeTo(OutputStream outstream) throws IOException {
					handler.store(outstream);
				}

				public boolean isStreaming() {
					return true;
				}

				public boolean isRepeatable() {
					return false;
				}

				public long getContentLength() {
					return -1;
				}

				public InputStream getContent() throws IOException,
						IllegalStateException {
					ByteArrayOutputStream buf = new ByteArrayOutputStream();
					writeTo(buf);
					return new ByteArrayInputStream(buf.toByteArray());
				}
			});
			return execute(post, new ResponseHandler<URI>() {
				public URI handleResponse(HttpResponse response)
						throws IOException {
					URI uri = getContentId(localContext);
					Header location = response
							.getLastHeader("Content-Location");
					if (location == null) {
						return uri;
					} else {
						return uri.resolve(location.getValue());
					}
				}
			}, localContext);
		} else {
			return fallback.writeEntry(href, base, media, handler);
		}
	}

	public void readEntry(String href, String base, String accept,
			String overrideContentType, DataReader handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		String sch = uri.getScheme();
		if ("http".equalsIgnoreCase(sch) || "https".equalsIgnoreCase(sch)) {
			readHttpEntity(uri, accept, overrideContentType, handler);
		} else {
			fallback.readEntry(href, base, accept, overrideContentType, handler);
		}
	}

	public void infoEntry(String href, String base, final String accept,
			final DataInfo handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		final URI uri = baseURI.resolve(href);
		String sch = uri.getScheme();
		if ("http".equalsIgnoreCase(sch) || "https".equalsIgnoreCase(sch)) {
			final HttpContext localContext = new BasicHttpContext();
			HttpHead head = new HttpHead(uri);
			head.setHeader("Accept", accept);
			execute(head, new ResponseHandler<Void>() {
				public Void handleResponse(HttpResponse response)
						throws IOException {
					URI contentId = getContentId(localContext);
					HttpEntity entity = response.getEntity();
					try {
						Header hd = entity.getContentType();
						String type = null;
						if (hd != null) {
							type = hd.getValue();
						}
						long lm = getLastModified(response);
						handler.list(contentId, type, lm);
					} catch (NumberFormatException e) {
						throw new IOException(e);
					} catch (DateParseException e) {
						throw new IOException(e);
					}
					return null;
				}
			}, localContext);
		} else {
			fallback.infoEntry(href, base, accept, handler);
		}
	}

	public void listEachEntry(String href, String base, String accept,
			DataInfo handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		fallback.listEachEntry(href, base, accept, handler);
	}

	public URI createList(String href, String base)
			throws MalformedURLException, FileNotFoundException, IOException {
		return fallback.createList(href, base);
	}

	public void deleteEntry(String href, String base)
			throws MalformedURLException, FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		String sch = uri.getScheme();
		if ("http".equalsIgnoreCase(sch) || "https".equalsIgnoreCase(sch)) {
			final HttpContext localContext = new BasicHttpContext();
			execute(new HttpDelete(uri), new ResponseHandler<Void>() {
				public Void handleResponse(HttpResponse response)
						throws IOException {
					return null;
				}
			}, localContext);
		} else {
			fallback.deleteEntry(href, base);
		}
	}

	private long getLastModified(HttpResponse response)
			throws DateParseException {
		Header last = response.getLastHeader("Last-Modified");
		if (last == null) {
			return -1;
		} else {
			return DateUtils.parseDate(last.getValue()).getTime();
		}
	}

	private void readHttpEntity(final URI uri, final String accept,
			final String overrideContentType, final DataReader handler) throws IOException,
			ClientProtocolException, Error {
		final HttpContext localContext = new BasicHttpContext();
		HttpGet get = new HttpGet(uri);
		get.setHeader("Accept", accept);
		execute(get, new ResponseHandler<Void>() {
			public Void handleResponse(HttpResponse response)
					throws IOException {
				URI contentId = getContentId(localContext);
				HttpEntity entity = response.getEntity();
				Header hd = entity.getContentType();
				String type = null;
				if (hd != null) {
					type = hd.getValue();
				}
                if (overrideContentType != null) {
                    type = overrideContentType;
                }
				handler.load(contentId, type, entity.getContent(),
						entity.getContentLength());
				return null;
			}
		}, localContext);
	}

	private <T> T execute(final HttpUriRequest request,
			final ResponseHandler<? extends T> handler, HttpContext context)
			throws IOException, ClientProtocolException {
		request.setParams(getHttpParams());
		return getHttpClient().execute(request, new ResponseHandler<T>() {
			public T handleResponse(HttpResponse response) throws IOException {
				int respCode = response.getStatusLine().getStatusCode();
				if (respCode == 200 || respCode == 201 || respCode == 203
						|| respCode == 204 || respCode == 205) {
					return handler.handleResponse(response);
				} else {
					throw error(request, respCode);
				}
			}
		}, context);
	}

	IOException error(final HttpUriRequest request, int respCode) {
		String uri = request.getURI().toASCIIString();
		if (respCode == 401 || respCode == 402 || respCode == 403) {
			return new IOException("Authentication failure for URL: " + uri);
		} else if (respCode == 404 || respCode == 405 || respCode == 410) {
			return new FileNotFoundException(uri);
		} else if (respCode == 406) {
			Header accept = request.getLastHeader("Accept");
			return new IOException("Unsupported media type: " + accept
					+ " for URL: " + uri);
		} else if (respCode == 415) {
			Header contentType = request.getLastHeader("Content-Type");
			return new IOException("Unsupported content type: " + contentType
					+ " for URL: " + uri);
		} else {
			return new java.io.IOException("Server returned HTTP"
					+ " response code: " + respCode + " for URL: " + uri);
		}
	}

	private URI getContentId(HttpContext localContext) {
		HttpHost host = (HttpHost) localContext
				.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		HttpUriRequest req = (HttpUriRequest) localContext
				.getAttribute(ExecutionContext.HTTP_REQUEST);
		try {
			URI root = new URI(host.getSchemeName(), null, host.getHostName(),
					host.getPort(), "/", null, null);
			return root.resolve(req.getURI());
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
	}

}
