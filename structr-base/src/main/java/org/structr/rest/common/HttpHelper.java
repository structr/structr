/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.common;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for outbound HTTP requests
 */
public class HttpHelper {

	// @code-quality:accept - last reviewed 2026-08-14. long_params counts 34 of the 50 public static
	// methods, but they are overload ladders (six each of get/post/put, four of delete, ...), where
	// the short forms delegate to one full-length method per verb. Replacing the parameters with a
	// request object would be a better API and a breaking change to a helper the modules call
	// directly, so it is a deliberate decision to keep, not an oversight to fix.

	public static final String FIELD_STATUS  = "status";
	public static final String FIELD_BODY    = "body";
	public static final String FIELD_HEADERS = "headers";
	public static final String FIELD_ERROR   = "error";

	private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class.getName());

	/**
	 * Per-request HTTP configuration returned by configure().
	 */
	private record HttpConfig(CloseableHttpClient client, String charset) {}

	private static HttpConfig configure(final HttpRequestBase req, final String requestCharset, final String username, final String password, final String proxyUrlParameter, final String proxyUsernameParameter, final String proxyPasswordParameter, final String cookieParameter, final Map<String, String> headers, final boolean followRedirects, final boolean validateCertificates, final Integer timeout) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

		final String charset        = StringUtils.isBlank(requestCharset)         ? Settings.HttpDefaultCharset.getValue() : requestCharset;
		final String proxyUrl       = StringUtils.isBlank(proxyUrlParameter)      ? Settings.HttpProxyUrl.getValue()       : proxyUrlParameter;
		final String proxyUsername  = StringUtils.isBlank(proxyUsernameParameter) ? Settings.HttpProxyUser.getValue()      : proxyUsernameParameter;
		final String proxyPassword  = StringUtils.isBlank(proxyPasswordParameter) ? Settings.HttpProxyPassword.getValue()  : proxyPasswordParameter;
		final String cookie         = StringUtils.isBlank(cookieParameter)        ? null                                   : cookieParameter;
		HttpHost proxy                          = null;
		final CredentialsProvider credsProvider = new BasicCredentialsProvider();

		if (StringUtils.isNoneBlank(username, password)) {

			credsProvider.setCredentials(new AuthScope(new HttpHost(req.getURI().getHost())), new UsernamePasswordCredentials(username, password));
		}

		if (StringUtils.isNotBlank(proxyUrl)) {

			proxy = HttpHost.create(proxyUrl);

			if (StringUtils.isNoneBlank(proxyUsername, proxyPassword)) {

				credsProvider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(proxyUsername, proxyPassword));
			}
		}

		final HttpClientBuilder clientBuilder = HttpClients.custom()
				.setDefaultConnectionConfig(ConnectionConfig.DEFAULT)
				.setUserAgent(Settings.HttpUserAgent.getValue())
				.setRedirectStrategy(new AddressCheckingRedirectStrategy())
				.setDefaultCredentialsProvider(credsProvider);

		if (!validateCertificates) {

			final boolean lenientHostnameCheck = "LENIENT".equalsIgnoreCase(Settings.HttpHostnameVerification.getValue());

			logger.warn("TLS certificate validation disabled for outbound request to {} (hostname verification: {}). This accepts self-signed and untrusted certificates{}.",
				req.getURI(),
				lenientHostnameCheck ? "LENIENT" : "STRICT",
				lenientHostnameCheck ? " and any hostname mismatch" : "");

			// trust every certificate
			final SSLContext sslContext = SSLContexts.custom().loadTrustMaterial((x509Certificates, s) -> true).build();

			// Keep hostname verification on by default so a certificate
			// still has to be presented for the host we are contacting —
			// even when the chain itself is not being checked. The legacy
			// "verify nothing" behaviour is opt-in via the setting.
			final javax.net.ssl.HostnameVerifier hostnameVerifier = lenientHostnameCheck
				? NoopHostnameVerifier.INSTANCE
				: SSLConnectionSocketFactory.getDefaultHostnameVerifier();

			final SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, null, null, hostnameVerifier);
			final BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", sslConnectionSocketFactory).build());

			clientBuilder.setConnectionManager(connectionManager);
		}

		final CloseableHttpClient client = clientBuilder.build();
		// A caller asking for a timeout means the call, not just the handshake. Bounding the connection
		// alone leaves a server that accepts and then answers slowly able to hold the request open for
		// as long as it likes, which is the hang the option exists to prevent.
		final int connectTimeout = (timeout != null) ? timeout : (Settings.HttpConnectTimeout.getValue() * 1000);
		final int socketTimeout  = (timeout != null) ? timeout : (Settings.HttpSocketTimeout.getValue() * 1000);
		final RequestConfig reqConfig = RequestConfig.custom()
			.setProxy(proxy)
			.setRedirectsEnabled(followRedirects)
			.setCookieSpec(CookieSpecs.STANDARD)
			.setConnectTimeout(connectTimeout)
			.setSocketTimeout(socketTimeout)
			.setConnectionRequestTimeout(Settings.HttpConnectionRequestTimeout.getValue() * 1000)
			.build();

		req.setConfig(reqConfig);

		if (StringUtils.isNotBlank(cookie)) {

			req.addHeader("Cookie", cookie);
			req.getParams().setParameter("http.protocol.single-cookie-header", true);
		}

		req.addHeader("Connection", "close");

		// add request headers from context
		for (final Map.Entry<String, String> header : headers.entrySet()) {

			req.addHeader(header.getKey(), header.getValue());
		}

		return new HttpConfig(client, charset);
	}

	public static String skipBOMIfPresent(final String content) {

		// Skip BOM to work around this Jsoup bug: https://github.com/jhy/jsoup/issues/348
		if (content != null && content.length() > 1 && content.charAt(0) == 65279) {

			return content.substring(1);
		}

		return content;
	}

	public static CloseableHttpClient getClient(final HttpRequestBase req, final String requestCharset, final String username, final String password, final String proxyUrlParameter, final String proxyUsernameParameter, final String proxyPasswordParameter, final String cookieParameter, final Map<String, String> headers, final boolean followRedirects, final boolean validateCertificates) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

		return configure(req, requestCharset, username, password, proxyUrlParameter, proxyUsernameParameter, proxyPasswordParameter, cookieParameter, headers, followRedirects, validateCertificates, null).client();
	}

	public static Map<String, Object> get(final String address) throws FrameworkException {

		return get(address, null, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static Map<String, Object> get(final String address, final String charset) throws FrameworkException {

		return get(address, charset, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static Map<String, Object> get(final String address, final String charset, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return get(address, charset, null, null, headers, validateCertificates);
	}

	public static Map<String, Object> get(final String address, final String charset, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return get(address, charset, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, Object> get(final String address, final String charset, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return get(address, charset, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static Map<String, Object> get(final String address, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return get(address, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates, null);
	}

	public static Map<String, Object> get(final String address, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates, final Map<String, Object> config) throws FrameworkException {

		final Map<String, Object> responseData = new HashMap<>();

		try {

			final URI uri       = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpGet req   = new HttpGet(uri);
			final HttpConfig hc = configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, redirectsFrom(config, false), validateCertificates, timeoutFrom(config));
			final CloseableHttpResponse resp = hc.client().execute(req);
			final String content = bodyOf(resp, charset(resp, hc.charset()));

			responseData.put(HttpHelper.FIELD_BODY, content);
			responseData.put(HttpHelper.FIELD_STATUS, Integer.toString(resp.getStatusLine().getStatusCode()));
			responseData.put(HttpHelper.FIELD_HEADERS, getHeadersAsMap(resp));

		} catch (final FrameworkException fex) {

			// a rejected URL, a blocked address, a whitelist miss: the CALL is wrong, and its status
			// and message must reach the caller rather than being re-wrapped as a failed request
			throw fex;

		} catch (final Throwable t) {

			return noResponse(address, "Unable to fetch content from address", t);
		}

		return responseData;
	}

	public static Map<String, Object> getBinary(final String address, final String charset, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return getBinary(address, charset, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, Object> getBinary(final String address, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		try {

			final Map<String, Object> result = getAsStream(address, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers);
			if (result.get(HttpHelper.FIELD_BODY) instanceof InputStream body) {

				// closing the stream is what closes the response and the client behind it, so it has to happen
				// even when the read fails part way through -- this method read the body and then dropped the
				// stream, which is how every binary GET left its client to be collected rather than closed.
				try (body) {

					result.put(HttpHelper.FIELD_BODY, IOUtils.toByteArray(body));
				}

			} else {

				result.put(HttpHelper.FIELD_BODY, null);
			}

			return result;

		} catch (final FrameworkException fex) {

			// already carries a status and a message naming the address, so re-wrapping it would only bury
			// the specific reason -- a rejected whitelist entry, say -- under a generic one.
			throw fex;

		} catch (final Throwable t) {

			return noResponse(address, "Error while downloading binary data from", t);
		}
	}

	public static Map<String, Object> postBinary(final String address, final Object requestBody, final String charset, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return postBinary(address, requestBody, charset, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, Object> postBinary(final String address, final Object requestBody, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		try {

			Map<String, Object> result = postAsStream(address, requestBody, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers);
			if (result != null && result.get(HttpHelper.FIELD_BODY) != null) {

				InputStream body = (InputStream) result.get(HttpHelper.FIELD_BODY);
				result.put(HttpHelper.FIELD_BODY, IOUtils.toByteArray(body));

			} else if (result != null) {

				result.put(HttpHelper.FIELD_BODY, null);
			}

			return result;

		} catch (final Throwable t) {

			return noResponse(address, "Error while downloading binary data from", t);
		}
	}

	public static Map<String, Object> head(final String address, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return head(address, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, Object> head(final String address, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return head(address, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static Map<String, Object> head(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return head(address, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates, null);
	}

	public static Map<String, Object> head(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates, final Map<String, Object> config) throws FrameworkException {

		final Map<String, Object> responseHeaders = new HashMap<>();

		try {

			final URI uri       = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpHead req  = new HttpHead(uri);
			final HttpConfig hc = configure(req, null, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, redirectsFrom(config, false), validateCertificates, timeoutFrom(config));
			final CloseableHttpResponse response = hc.client().execute(req);

			responseHeaders.put(HttpHelper.FIELD_STATUS, Integer.toString(response.getStatusLine().getStatusCode()));
			responseHeaders.put(HttpHelper.FIELD_HEADERS, Arrays.stream(response.getAllHeaders()).collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue)));

		} catch (final FrameworkException fex) {

			// a rejected URL, a blocked address, a whitelist miss: the CALL is wrong, and its status
			// and message must reach the caller rather than being re-wrapped as a failed request
			throw fex;

		} catch (final Throwable t) {

			return noResponse(address, "Unable to get headers from address", t);
		}

		return responseHeaders;
	}

	public static Map<String, Object> patch(final String address, final Object requestBody, final String username, final String password, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {

		return patch(address, requestBody, username, password, null, null, null, null, headers, charset, validateCertificates);
	}

	public static Map<String, Object> patch(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {

		return patch(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, charset, validateCertificates, null);
	}

	public static Map<String, Object> patch(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates, final String contentType) throws FrameworkException {

		return patch(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, charset, validateCertificates, contentType, null);
	}

	public static Map<String, Object> patch(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates, final String contentType, final Map<String, Object> config) throws FrameworkException {

		final Map<String, Object> responseData = new HashMap<>();

		try {

			final URI url       = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpPut req   = new HttpPatch(url);
			final HttpConfig hc = configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, redirectsFrom(config, true), validateCertificates, timeoutFrom(config));

			req.setEntity(entityFor(requestBody, contentType, hc.charset()));

			final CloseableHttpResponse response = hc.client().execute(req);
			final HttpEntity entity = response.getEntity();
			String content = null;

			if (entity != null) {

				final InputStream responseContent = entity.getContent();
				if (responseContent != null) {

					content = IOUtils.toString(responseContent, charset(response, hc.charset()));
				}
			}

			content = skipBOMIfPresent(content);

			responseData.put(HttpHelper.FIELD_BODY, content);
			responseData.put(HttpHelper.FIELD_STATUS, Integer.toString(response.getStatusLine().getStatusCode()));
			responseData.put(HttpHelper.FIELD_HEADERS, getHeadersAsMap(response));

		} catch (final FrameworkException fex) {

			// a rejected URL, a blocked address, a whitelist miss: the CALL is wrong, and its status
			// and message must reach the caller rather than being re-wrapped as a failed request
			throw fex;

		} catch (final Throwable t) {

			return noResponse(address, "Unable to issue PATCH request to address", t);
		}

		return responseData;
	}

	/**
	 * The request entity, carrying the Content-Type the caller asked for.
	 *
	 * StringEntity(body, charset) alone sends text/plain, which is why a contentType passed to
	 * $.POST() and friends never reached the wire before. The header is derived from the entity by
	 * HttpClient, so it has to be set here and not added to the header map, or the request would carry
	 * the type twice.
	 */
	/** timeout (milliseconds) and redirects, as the outbound HTTP functions pass them. */
	/**
	 * The response body, or null when the server sent none.
	 *
	 * getEntity() is null for a 204, a 304 and for any error response without a body. Reading through it
	 * unchecked turned those into a NullPointerException, which the catch below then reported as a failed
	 * request: the status the server actually sent was lost and the caller saw 422 instead of 404.
	 */
	private static String bodyOf(final CloseableHttpResponse resp, final String charset) throws IOException {

		final HttpEntity entity = resp.getEntity();

		return entity != null ? skipBOMIfPresent(IOUtils.toString(entity.getContent(), charset)) : null;
	}

	/**
	 * The result of a request that never reached the server: DNS, connection refused, TLS, timeout.
	 *
	 * Returned rather than thrown, because there is nothing wrong with the CALL. A status of 0 says "no
	 * response", the way fetch() has no status when it rejects, and lets StructrScript branch on it: that
	 * language has no try/catch, so a throw here would be unrecoverable rather than merely inconvenient.
	 * Only a request that cannot be made at all - a malformed URL, a blocked address - still throws.
	 */
	private static Map<String, Object> noResponse(final String address, final String what, final Throwable t) {

		logger.warn("{} {}: {}", what, address, t.getMessage());

		final Map<String, Object> responseData = new HashMap<>();

		responseData.put(HttpHelper.FIELD_BODY,    null);
		responseData.put(HttpHelper.FIELD_STATUS,  "0");
		responseData.put(HttpHelper.FIELD_HEADERS, Collections.emptyMap());
		responseData.put(HttpHelper.FIELD_ERROR,   t.getMessage() != null ? t.getMessage() : t.toString());

		return responseData;
	}

	private static Integer timeoutFrom(final Map<String, Object> config) {

		// Number, not Integer: StructrScript hands over its numeric literals as Double

		return (config != null && config.get("timeout") instanceof Number number) ? number.intValue() : null;
	}

	private static boolean redirectsFrom(final Map<String, Object> config, final boolean defaultValue) {

		return (config != null && config.get("redirects") instanceof Boolean redirects) ? redirects : defaultValue;
	}

	/**
	 * DELETE with a request body.
	 *
	 * HttpDelete cannot carry an entity, although HTTP permits a body on DELETE and some APIs require
	 * one, so the method is spelled out on an entity-enclosing request instead.
	 */
	private static class HttpDeleteWithBody extends org.apache.http.client.methods.HttpEntityEnclosingRequestBase {

		HttpDeleteWithBody(final URI uri) {

			setURI(uri);
		}

		@Override
		public String getMethod() {

			return "DELETE";
		}
	}

	/** Whether there is no body to send: null, or text that is blank. Binary is never "blank". */
	private static boolean isEmptyBody(final Object requestBody) {

		if (requestBody == null) {

			return true;
		}

		if (requestBody instanceof byte[] bytes) {

			return bytes.length == 0;
		}

		if (requestBody instanceof InputStream) {

			return false;
		}

		return StringUtils.isBlank(requestBody.toString());
	}

	/**
	 * The request entity for a body, which may be text or binary.
	 *
	 * A byte[] or an InputStream is sent as-is, so a caller can push a file's content without inflating it
	 * through base64 or wrapping it in a multipart envelope. An InputStream entity is NOT repeatable: see
	 * the note on preemptive authentication in the verb functions, because a body that cannot be sent
	 * twice cannot survive a 401 challenge or a redirect.
	 */
	private static HttpEntity entityFor(final Object requestBody, final String contentType, final String charset) {

		final ContentType type = StringUtils.isBlank(contentType) ? null : ContentType.create(ContentType.parse(contentType).getMimeType(), charset);

		if (requestBody instanceof byte[] bytes) {

			return type != null ? new ByteArrayEntity(bytes, type) : new ByteArrayEntity(bytes);
		}

		if (requestBody instanceof InputStream stream) {

			// length unknown, so the request is sent chunked

			return type != null ? new InputStreamEntity(stream, -1, type) : new InputStreamEntity(stream, -1);
		}

		final String text = requestBody != null ? requestBody.toString() : "";

		return type != null ? new StringEntity(text, type) : new StringEntity(text, charset);
	}

	public static Map<String, Object> post(final String address, final Object requestBody) throws FrameworkException {

		return post(address, requestBody, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static Map<String, Object> post(final String address, final Object requestBody, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return post(address, requestBody, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, Object> post(final String address, final Object requestBody, final String username, final String password, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {

		return post(address, requestBody, username, password, null, null, null, null, headers, charset, validateCertificates, null);
	}

	public static Map<String, Object> post(final String address, final Object requestBody, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return post(address, requestBody, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static Map<String, Object> post(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return post(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, "UTF-8", validateCertificates, null);
	}

	public static Map<String, Object> post(String address, String requestBody, String proxyUrl, String proxyUsername, Map<String, String> headers, String charset, boolean validateCertificates, Map<String, Object> config) throws FrameworkException {

		return post(address, requestBody, null, null, proxyUrl, proxyUsername, null, null, headers, charset, validateCertificates, config);
	}

	public static Map<String, Object> post(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates, final Map<String, Object> config) throws FrameworkException {

		return post(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, charset, validateCertificates, config, null);
	}

	public static Map<String, Object> post(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates, final Map<String, Object> config, final String contentType) throws FrameworkException {

		final Map<String, Object> responseData = new HashMap<>();

		try {

			final URI uri      = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpPost req = new HttpPost(uri);
			Integer timeout         = null;
			boolean followRedirects = false;

			if (config != null) {

				// Number, not Integer: StructrScript hands over its numeric literals as Double
				if (config.get("timeout") instanceof Number number) {

					timeout = number.intValue();
				}

				if (config.get("redirects") instanceof Boolean redirects) {

					followRedirects = redirects;
				}
			}

			final HttpConfig hc = configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, followRedirects, validateCertificates, timeout);

			req.setEntity(entityFor(requestBody, contentType, hc.charset()));

			final CloseableHttpResponse response = hc.client().execute(req);
			final HttpEntity responseEntity = response.getEntity();
			String content = null;

			if (responseEntity != null) {

				content = IOUtils.toString(responseEntity.getContent(), charset(response, hc.charset()));
			}

			content = skipBOMIfPresent(content);

			responseData.put(HttpHelper.FIELD_BODY, content);
			responseData.put(HttpHelper.FIELD_STATUS, Integer.toString(response.getStatusLine().getStatusCode()));
			responseData.put(HttpHelper.FIELD_HEADERS, getHeadersAsMap(response));

		} catch (final FrameworkException fex) {

			// a rejected URL, a blocked address, a whitelist miss: the CALL is wrong, and its status
			// and message must reach the caller rather than being re-wrapped as a failed request
			throw fex;

		} catch (final Throwable t) {

			return noResponse(address, "Unable to issue POST request to address", t);
		}

		return responseData;
	}

	public static Map<String, Object> put(final String address, final Object requestBody) throws FrameworkException {

		return put(address, requestBody, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static Map<String, Object> put(final String address, final Object requestBody, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return put(address, requestBody, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, Object> put(final String address, final Object requestBody, final String username, final String password, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {

		return put(address, requestBody, username, password, null, null, null, null, headers, charset, validateCertificates);
	}

	public static Map<String, Object> put(final String address, final Object requestBody, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return put(address, requestBody, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static Map<String, Object> put(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return put(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, "UTF-8", validateCertificates);
	}

	public static Map<String, Object> put(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {

		return put(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, charset, validateCertificates, null);
	}

	public static Map<String, Object> put(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates, final String contentType) throws FrameworkException {

		return put(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, charset, validateCertificates, contentType, null);
	}

	public static Map<String, Object> put(final String address, final Object requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates, final String contentType, final Map<String, Object> config) throws FrameworkException {

		final Map<String, Object> responseData = new HashMap<>();

		try {

			final URI uri       = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpPut req   = new HttpPut(uri);
			final HttpConfig hc = configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, redirectsFrom(config, true), validateCertificates, timeoutFrom(config));

			req.setEntity(entityFor(requestBody, contentType, hc.charset()));

			final CloseableHttpResponse response = hc.client().execute(req);
			final HttpEntity responseEntity = response.getEntity();
			String content = null;

			if (responseEntity != null) {

				content = IOUtils.toString(responseEntity.getContent(), charset(response, hc.charset()));
			}

			content = skipBOMIfPresent(content);

			responseData.put(HttpHelper.FIELD_BODY, content);
			responseData.put(HttpHelper.FIELD_STATUS, Integer.toString(response.getStatusLine().getStatusCode()));
			responseData.put(HttpHelper.FIELD_HEADERS, getHeadersAsMap(response));

		} catch (final FrameworkException fex) {

			// a rejected URL, a blocked address, a whitelist miss: the CALL is wrong, and its status
			// and message must reach the caller rather than being re-wrapped as a failed request
			throw fex;

		} catch (final Throwable t) {

			return noResponse(address, "Unable to issue PUT request to address", t);
		}

		return responseData;
	}

	public static Map<String, Object> delete(final String address) throws FrameworkException {

		return delete(address, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static Map<String, Object> delete(final String address, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return delete(address, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, Object> delete(final String address, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return delete(address, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static Map<String, Object> delete(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return delete(address, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates, null);
	}

	public static Map<String, Object> delete(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates, final Map<String, Object> config) throws FrameworkException {

		return delete(address, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates, config, null, null, null);
	}

	public static Map<String, Object> delete(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates, final Map<String, Object> config, final Object requestBody, final String contentType, final String charset) throws FrameworkException {

		final Map<String, Object> responseData = new HashMap<>();

		try {

			final URI uri = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpRequestBase req = isEmptyBody(requestBody) ? new HttpDelete(uri) : new HttpDeleteWithBody(uri);
			final HttpConfig hc = configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, redirectsFrom(config, false), validateCertificates, timeoutFrom(config));

			if (req instanceof HttpDeleteWithBody withBody) {

				withBody.setEntity(entityFor(requestBody, contentType, hc.charset()));
			}

			final CloseableHttpResponse response = hc.client().execute(req);
			final HttpEntity responseEntity = response.getEntity();
			String content = null;

			if (responseEntity != null) {

				content = IOUtils.toString(responseEntity.getContent(), charset(response, hc.charset()));
			}

			content = skipBOMIfPresent(content);

			responseData.put(HttpHelper.FIELD_BODY, content);
			responseData.put(HttpHelper.FIELD_STATUS, Integer.toString(response.getStatusLine().getStatusCode()));
			responseData.put(HttpHelper.FIELD_HEADERS, getHeadersAsMap(response));

		} catch (final FrameworkException fex) {

			// a rejected URL, a blocked address, a whitelist miss: the CALL is wrong, and its status
			// and message must reach the caller rather than being re-wrapped as a failed request
			throw fex;

		} catch (final Throwable t) {

			return noResponse(address, "Unable to issue DELETE command to address", t);
		}

		return responseData;
	}

	public static Map<String, Object> getAsStream(final String address) throws FrameworkException {

		return getAsStream(address, null, null, null, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static Map<String, Object> getAsStream(final String address, final String charset) throws FrameworkException {

		return getAsStream(address, charset, null, null, null, null, null, null, Collections.EMPTY_MAP);
	}

	/**
	 * Issues a GET and hands the response body back as a live {@link InputStream}, so a large download is
	 * never buffered whole -- 86e0ef7f8a removed the 2 GB limit with it, and that is deliberate.
	 *
	 * <p><b>The returned stream owns the response and the client.</b> Closing it closes both, and nothing
	 * else can: {@code configure} builds a fresh {@link CloseableHttpClient} per call and the caller never
	 * sees it. Close the stream -- try-with-resources is enough.</p>
	 *
	 * <p>Be precise about what that is worth, because it is easy to overstate. {@code configure} adds
	 * {@code Connection: close} to every request, so Apache closes the socket itself once the entity
	 * reaches EOF or the entity stream is closed; the socket is not what was being leaked. What was leaked
	 * is the client and its connection manager as <i>objects</i>, released at GC rather than
	 * deterministically -- plus, for a stream that is neither read to the end nor closed, the connection
	 * it is still holding.</p>
	 *
	 * <p>A response carrying no entity at all -- a 204, say -- answers a {@code null} body with the
	 * status and headers still present, and closes its own connection before returning, since there is
	 * nothing for a caller to close.</p>
	 *
	 * @throws FrameworkException if the request could not be issued or the whitelist refused the address.
	 *         Reported rather than answered with {@code null}: a null surfaced as an NPE at the call site
	 *         with the cause only in the log, and it swallowed the whitelist refusal along with it.
	 */
	public static Map<String, Object> getAsStream(final String address, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) throws FrameworkException {

		return getAsStream(address, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, null);
	}

	/**
	 * The streaming variant of get(), with the same transport settings as the buffered one.
	 *
	 * These used to be hardcoded here, so a caller asking for a streamed response silently lost its
	 * certificate validation, its timeout and its redirect setting: the option was accepted and did
	 * nothing, which surfaces much later as a hung or unexpectedly trusted call.
	 */
	public static Map<String, Object> getAsStream(final String address, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates, final Map<String, Object> config) throws FrameworkException {

		CloseableHttpClient client   = null;
		CloseableHttpResponse resp   = null;

		try {

			final URI uri       = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpGet req   = new HttpGet(uri);
			final HttpConfig hc = configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, redirectsFrom(config, true), validateCertificates, timeoutFrom(config));

			client = hc.client();
			resp   = client.execute(req);

			return streamResponse(address, resp, client);

		} catch (final FrameworkException fex) {

			// the whitelist refusal, which names the setting to change: reported as itself rather than
			// re-wrapped, so the actionable message is what the caller sees
			closeQuietly(address, resp, client);

			throw fex;

		} catch (final Throwable t) {

			// the stream was never handed out, so nothing else is in a position to close these
			closeQuietly(address, resp, client);

			throw new FrameworkException(422, "Unable to get content stream from address " + address + ": " + t.getMessage(), t);
		}
	}

	public static Map<String, Object> postAsStream(final String address, final Object requestBody) throws FrameworkException {

		return postAsStream(address, requestBody, null, null, null, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static Map<String, Object> postAsStream(final String address, final Object requestBody, final String charset) throws FrameworkException {

		return postAsStream(address, requestBody, charset, null, null, null, null, null, null, Collections.EMPTY_MAP);
	}

	/**
	 * The POST counterpart of {@link #getAsStream}, with the same ownership rule: closing the returned
	 * stream closes the response and the client.
	 *
	 * <p><b>Nothing calls this today.</b> It is kept in step with {@code getAsStream} rather than left as
	 * a leaking copy beside a fixed one, so that a future caller does not reintroduce the leak; whether it
	 * should exist at all is a separate question.</p>
	 */
	public static Map<String, Object> postAsStream(final String address, final Object requestBody, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) throws FrameworkException {

		return postAsStream(address, requestBody, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, null, true, null);
	}

	/** The streaming variant of post(), with the same transport settings and content type as the buffered one. */
	public static Map<String, Object> postAsStream(final String address, final Object requestBody, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String contentType, final boolean validateCertificates, final Map<String, Object> config) throws FrameworkException {

		CloseableHttpClient client   = null;
		CloseableHttpResponse resp   = null;

		try {

			final URI uri       = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpPost req  = new HttpPost(uri);
			final HttpConfig hc = configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, redirectsFrom(config, true), validateCertificates, timeoutFrom(config));

			req.setEntity(entityFor(requestBody, contentType, hc.charset()));

			client = hc.client();
			resp   = client.execute(req);

			return streamResponse(address, resp, client);

		} catch (final FrameworkException fex) {

			closeQuietly(address, resp, client);

			throw fex;

		} catch (final Throwable t) {

			closeQuietly(address, resp, client);

			throw new FrameworkException(422, "Unable to post and get content stream from address " + address + ": " + t.getMessage(), t);
		}
	}

	/**
	 * Builds the response map for a streaming call. Shared by {@link #getAsStream} and
	 * {@link #postAsStream}, which differ only in the request they issue and so must not differ in how
	 * they hand back the connection.
	 */
	private static Map<String, Object> streamResponse(final String address, final CloseableHttpResponse resp, final CloseableHttpClient client) throws IOException {

		final Map<String, Object> responseData = new HashMap<>();

		responseData.put(HttpHelper.FIELD_STATUS,  Integer.toString(resp.getStatusLine().getStatusCode()));
		responseData.put(HttpHelper.FIELD_HEADERS, getHeadersAsMap(resp));

		final HttpEntity entity = resp.getEntity();
		if (entity == null) {

			// no body means no stream, so a caller has nothing to close and the response and client would be
			// left to GC. Closing here also keeps the old NPE from coming back: resp.getEntity() is null for
			// a 204, and dereferencing it was what made a body-less response indistinguishable from a failure.
			closeQuietly(address, resp, client);
			responseData.put(HttpHelper.FIELD_BODY, null);

			return responseData;
		}

		responseData.put(HttpHelper.FIELD_BODY, closingStream(address, entity.getContent(), resp, client));

		return responseData;
	}

	/**
	 * The response body, wrapped so that closing it also closes the response and then the client.
	 *
	 * <p>A failure to close is logged rather than thrown: the caller is closing a stream it has already
	 * finished with and has nothing useful to do about it, and throwing would hide whatever it was
	 * actually doing.</p>
	 */
	private static InputStream closingStream(final String address, final InputStream content, final CloseableHttpResponse resp, final CloseableHttpClient client) {

		return new FilterInputStream(content) {

			@Override
			public void close() throws IOException {

				try {

					super.close();

				} finally {

					closeQuietly(address, resp, client);
				}
			}
		};
	}

	/**
	 * Closes each resource in turn, logging rather than throwing, and never letting one failure skip the
	 * rest. Null entries are skipped, so this can be called from a catch block that does not know how far
	 * the request got.
	 */
	private static void closeQuietly(final String address, final Closeable... closeables) {

		for (final Closeable closeable : closeables) {

			if (closeable != null) {

				try {

					closeable.close();

				} catch (final Throwable t) {

					logger.warn("Unable to close HTTP resource for address {}: {}", address, t.getMessage());
				}
			}
		}
	}

	/**
	 * Determine the charset from the response Content-Type header,
	 * falling back to the configured default charset.
	 */
	public static String charset(final HttpResponse response) {

		return charset(response, Settings.HttpDefaultCharset.getValue());
	}

	/**
	 * Determine the charset from the response Content-Type header,
	 * falling back to the provided default charset.
	 */
	public static String charset(final HttpResponse response, final String defaultCharset) {

		final ContentType contentType = ContentType.get(response.getEntity());
		if (contentType != null && contentType.getCharset() != null) {

			return contentType.getCharset().toString();
		}

		return defaultCharset;
	}

	public static void streamURLToFile(final String address, final java.io.File fileOnDisk) throws FrameworkException {

		streamURLToFile(address, null, null, null, null, null, Collections.EMPTY_MAP, fileOnDisk);
	}

	public static void streamURLToFile(final String address, final String charset, final Map<String, String> headers, final java.io.File fileOnDisk) throws FrameworkException {

		streamURLToFile(address, charset, null, null, headers, fileOnDisk);
	}

	public static void streamURLToFile(final String address, final String charset, final String username, final String password, final Map<String, String> headers, final java.io.File fileOnDisk) throws FrameworkException {

		streamURLToFile(address, charset, username, password, null, null, null, null, headers, fileOnDisk);
	}

	public static void streamURLToFile(final String address, final String charset, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final java.io.File fileOnDisk) throws FrameworkException {

		streamURLToFile(address, charset, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, fileOnDisk);
	}

	public static void streamURLToFile(final String address, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final java.io.File fileOnDisk) throws FrameworkException {

		HttpHelper.validateUrl(address);

		try {

			final URI url = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpGet req = new HttpGet(url);

			logger.info("Downloading from {}", address);

			final HttpConfig hc = configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, true, null);

			req.addHeader("User-Agent", "curl/7.35.0");

			final CloseableHttpResponse resp = hc.client().execute(req);
			final int statusCode = resp.getStatusLine().getStatusCode();

			if (statusCode == 200) {

				try (final InputStream is = resp.getEntity().getContent()) {

					try (final OutputStream os = new FileOutputStream(fileOnDisk)) {

						IOUtils.copy(is, os);
					}
				}

			} else {

				// consume content, but discard it
				String content = bodyOf(resp, charset(resp, hc.charset()));

				logger.warn("Unable to create file from URI {}: status code was {}, discarding content", address, statusCode);
			}

		} catch (final FrameworkException fex) {

			// a rejected URL, a blocked address, a whitelist miss: the CALL is wrong, and its status
			// and message must reach the caller rather than being re-wrapped as a failed request
			throw fex;

		} catch (final Throwable t) {

			// this one writes to a file rather than returning a response, so there is no status to hand
			// back. A download that never reached the server is still a failure the caller must see.
			throw new FrameworkException(504, "Unable to fetch file content from address " + address + ": " + t.getMessage(), t);
		}
	}

	public static Map<String, String> getHeadersAsMap(final HttpResponse response) {

		final Map<String, String> map = new HashMap<>();

		for (final Header header : response.getAllHeaders()) {

			final String key = header.getName();
			if (map.containsKey(key)) {

				map.put(key, String.join(System.lineSeparator(), map.get(key), header.getValue()));

			} else {

				map.put(header.getName(), header.getValue());
			}
		}

		return map;
	}

	// ----- private methods -----

	/**
	 * Validates a URL against SSRF attacks. Rejects non-HTTP schemes and
	 * URLs that resolve to private/internal IP ranges (loopback, link-local,
	 * site-local, any-local, multicast).
	 *
	 * @param address the URL to validate
	 * @throws FrameworkException if the URL is invalid or resolves to a blocked address
	 */
	public static void validateUrl(final String address) throws FrameworkException {

		if (!Settings.SsrfProtection.getValue()) {

			return;
		}

		final URI uri;

		try {

			uri = URI.create(address);

		} catch (IllegalArgumentException e) {

			throw new FrameworkException(400, "Invalid URL: " + address);
		}

		// Only allow http and https schemes
		final String scheme = uri.getScheme();
		if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {

			throw new FrameworkException(400, "Only http and https URLs are allowed");
		}

		// Resolve hostname and check against private IP ranges
		final String host = uri.getHost();
		if (host == null) {

			throw new FrameworkException(400, "URL has no host component");
		}

		try {

			/* Every address the name answers with, not just the first: a name that resolves to a public
			   and a private address would otherwise pass the check and then be connected to on whichever
			   one the client picks (ticket 1595). */
			for (final InetAddress resolved : InetAddress.getAllByName(host)) {

				if (isBlockedAddress(resolved)) {

					logger.warn("Blocked outbound request to internal address {} (resolved from {})", resolved.getHostAddress(), host);
					throw new FrameworkException(403, "Requests to internal network addresses are not allowed");
				}
			}

		} catch (UnknownHostException e) {

			throw new FrameworkException(400, "Unable to resolve hostname: " + host);
		}
	}

	/**
	 * Whether the given URL points at an address this instance must not reach. Same filter as
	 * {@link #validateUrl}, as a question rather than an exception, for the redirect strategy.
	 */
	public static boolean isBlockedUrl(final URI uri) {

		return uri != null && isBlockedHost(uri.getHost());
	}

	/** Whether any address this host name answers with is one this instance must not reach. */
	public static boolean isBlockedHost(final String host) {

		if (StringUtils.isBlank(host)) {

			return false;
		}

		try {

			for (final InetAddress resolved : InetAddress.getAllByName(host)) {

				if (isBlockedAddress(resolved)) {

					return true;
				}
			}

		} catch (final UnknownHostException uhe) {

			// a name that does not resolve cannot be connected to either, so let the client fail on it

			return false;
		}

		return false;
	}

	/**
	 * Whether a redirect from {@code sourceHost} to {@code target} crosses into the internal network.
	 *
	 * <p>The direction is what matters, not the destination. A request that starts inside and is
	 * redirected inside is an internal service talking to itself, and refusing that would break ordinary
	 * deployments; a request that starts outside and is pulled inside is the SSRF (ticket 1595). It also
	 * keeps the existing split intact: asking for an internal address directly is still the whitelist's
	 * business, not this filter's.</p>
	 */
	public static boolean isBlockedRedirect(final String sourceHost, final URI target) {

		return !isBlockedHost(sourceHost) && isBlockedUrl(target);
	}

	/**
	 * Follows redirects like {@link LaxRedirectStrategy}, but refuses one that points into the internal
	 * network.
	 *
	 * <p>Ticket 1595: the address checks ran on the URL a caller passed in, and the client then followed
	 * 3xx wherever they led - so {@code https://attacker/redir} to {@code http://169.254.169.254/} went
	 * through every check there was. A redirect target is never something the caller chose, which is why
	 * this applies to all outbound requests and not only to the ones that validated their start URL:
	 * asking for an internal address stays possible, being sent to one from outside does not. A request
	 * that already started inside may be redirected inside - see {@link #isBlockedRedirect}.</p>
	 */
	private static class AddressCheckingRedirectStrategy extends LaxRedirectStrategy {

		@Override
		public URI getLocationURI(final HttpRequest request, final HttpResponse response, final HttpContext context) throws ProtocolException {

			final URI uri = super.getLocationURI(request, response, context);

			if (Settings.SsrfProtection.getValue()) {

				final HttpHost currentHost = HttpClientContext.adapt(context).getTargetHost();
				final String sourceHost    = (currentHost != null) ? currentHost.getHostName() : null;

				if (isBlockedRedirect(sourceHost, uri)) {

					logger.warn("Blocked redirect from {} to internal address {}", sourceHost, uri);

					throw new ProtocolException("Redirect to an internal network address is not allowed: " + uri);
				}
			}

			return uri;
		}
	}

	/**
	 * SSRF address filter shared by {@link #validateUrl}, {@link #isBlockedHost} - and with it the
	 * redirect strategy - and {@link #validateProxyUrl}. Blocks loopback,
	 * link-local (incl. 169.254.169.254), site-local (RFC1918), wildcard and multicast addresses, plus the
	 * ranges {@link InetAddress} does not flag: IPv4 carrier-grade NAT 100.64.0.0/10 and IPv6 unique local
	 * addresses fc00::/7 (which covers fd00::/8). #1580
	 */
	private static boolean isBlockedAddress(final InetAddress address) {

		if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress() || address.isMulticastAddress()) {

			return true;
		}

		final byte[] bytes = address.getAddress();

		// IPv4 carrier-grade NAT: 100.64.0.0/10
		if (bytes.length == 4) {

			final int first  = bytes[0] & 0xff;
			final int second = bytes[1] & 0xff;

			if (first == 100 && second >= 64 && second <= 127) {

				return true;
			}
		}

		// IPv6 unique local addresses: fc00::/7
		if (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc) {

			return true;
		}

		return false;
	}

	/**
	 * Validates a proxy address (host:port or scheme://host:port) against the same SSRF filter as
	 * {@link #validateUrl}. The connection to a proxy goes to the proxy's own host:port regardless of the
	 * requested URL, so a caller-supplied proxy must not be allowed to point at an internal service. #1580
	 */
	public static void validateProxyUrl(final String proxyUrl) throws FrameworkException {

		if (!Settings.SsrfProtection.getValue() || StringUtils.isBlank(proxyUrl)) {

			return;
		}

		final String host;

		try {

			// parse exactly as the client will (org.apache.http.HttpHost.create), so the validated host is
			// the host actually connected to
			host = HttpHost.create(proxyUrl).getHostName();

		} catch (final IllegalArgumentException e) {

			throw new FrameworkException(400, "Invalid proxy URL: " + proxyUrl);
		}

		if (StringUtils.isBlank(host)) {

			throw new FrameworkException(400, "Proxy URL has no host component: " + proxyUrl);
		}

		try {

			/* Every address the name answers with, the same rule the target host goes through: a proxy name
			   that resolves to a public and a private address would otherwise pass on whichever one came
			   first and then be connected to on the other. */
			for (final InetAddress resolved : InetAddress.getAllByName(host)) {

				if (isBlockedAddress(resolved)) {

					logger.warn("Blocked outbound request via internal proxy address {} (resolved from {})", resolved.getHostAddress(), host);
					throw new FrameworkException(403, "Proxies on internal network addresses are not allowed");
				}
			}

		} catch (final UnknownHostException e) {

			throw new FrameworkException(400, "Unable to resolve proxy hostname: " + host);
		}
	}

	/**
	 * The whitelist and scheme check every outbound HTTP function goes through, for callers that build
	 * their own request instead of using one of the methods here (ticket 1595).
	 */
	public static URI validateOutgoingAddress(final String address) throws FrameworkException {

		return checkAddressAgainstWhitelist(address);
	}

	private static URI checkAddressAgainstWhitelist(final String address) throws FrameworkException {

		final String whitelist = Settings.OutgoingURLWhitelist.getValue(null);
		final URI uri;

		// An address that cannot be requested at all is a bad CALL, not a failed request, so it throws
		// rather than coming back as a status of 0. Deliberately only what makes the URL unusable: the
		// host and network checks in validateUrl() belong to the servlets, not to these functions.
		try {

			uri = URI.create(address);

		} catch (final IllegalArgumentException iex) {

			throw new FrameworkException(400, "Invalid URL: " + address);
		}

		if (uri.getScheme() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {

			throw new FrameworkException(400, "Only http and https URLs are allowed, got: " + address);
		}

		if (StringUtils.isBlank(uri.getHost())) {

			throw new FrameworkException(400, "URL has no host component: " + address);
		}

		if (!"*".equals(whitelist)) {

			// check address against whitelist
			uri.normalize();

			for (final String part : StringUtils.split(whitelist, ',')) {

				final String cleanedPart = part.strip();
				if (StringUtils.isNotBlank(cleanedPart)) {

					if (address.matches(cleanedPart)) {

						return uri;
					}
				}
			}

			throw new FrameworkException(422, "Outgoing URL \"" + address + "\" does not match any entry in the " + Settings.OutgoingURLWhitelist.getKey() + " setting. Please update the setting in structr.conf to allow this action.");
		}

		return uri;
	}

	// ----- generic fetch -----

	public static Map<String, Object> fetch(final String address, final String method, final Object requestBody, final String username, final String password, final Map<String, String> headers, final String charset, final boolean validateCertificates, final boolean followRedirects, final Integer timeout) throws FrameworkException {

		return fetch(address, method, requestBody, username, password, headers, charset, validateCertificates, followRedirects, timeout, null);
	}

	public static Map<String, Object> fetch(final String address, final String method, final Object requestBody, final String username, final String password, final Map<String, String> headers, final String charset, final boolean validateCertificates, final boolean followRedirects, final Integer timeout, final String contentType) throws FrameworkException {

		// No validateUrl() here. It guards the paths where the address comes from outside - ProxyServlet,
		// DeploymentServlet, DataFeed - and this one is called from a script, where the address is code.
		// Blocking internal addresses here protected nothing, since POST, GET, PUT, PATCH and DELETE
		// reach the same host, and it made an instance unable to call itself or a sidecar.

		final Map<String, Object> responseData = new HashMap<>();

		try {

			final URI uri                 = HttpHelper.checkAddressAgainstWhitelist(address);
			final HttpGenericMethod req    = new HttpGenericMethod(uri, method.toUpperCase());
			final HttpConfig hc            = configure(req, charset, username, password, null, null, null, null, headers, followRedirects, validateCertificates, timeout);

			if (!isEmptyBody(requestBody)) {

				req.setEntity(entityFor(requestBody, contentType, hc.charset()));
			}

			final CloseableHttpResponse response = hc.client().execute(req);
			final HttpEntity responseEntity       = response.getEntity();
			String content = null;

			if (responseEntity != null) {

				content = IOUtils.toString(responseEntity.getContent(), charset(response, hc.charset()));
			}

			content = skipBOMIfPresent(content);

			responseData.put(HttpHelper.FIELD_BODY, content);
			responseData.put(HttpHelper.FIELD_STATUS, Integer.toString(response.getStatusLine().getStatusCode()));
			responseData.put(HttpHelper.FIELD_HEADERS, getHeadersAsMap(response));

		} catch (final FrameworkException fe) {

			throw fe;

		} catch (final Throwable t) {

			return noResponse(address, "Unable to issue " + method + " request to address", t);
		}

		return responseData;
	}

	// ----- nested classes -----

	/**
	 * Generic HTTP method class that supports arbitrary HTTP verbs.
	 * Extends HttpEntityEnclosingRequestBase to allow sending a request body
	 * with any method (e.g. PROPFIND, REPORT, SEARCH).
	 */
	public static class HttpGenericMethod extends HttpEntityEnclosingRequestBase {

		private final String method;

		public HttpGenericMethod(final URI uri, final String method) {

			super();
			setURI(uri);
			this.method = method;
		}

		@Override
		public String getMethod() {

			return method;
		}
	}

	public static class HttpPatch extends HttpPut {

		public HttpPatch(final URI uri) {

			super(uri);
		}

		@Override
		public String getMethod() {

			return "PATCH";
		}
	}
}
