/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;

import javax.net.ssl.SSLContext;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for outbound HTTP requests
 */
public class HttpHelper {

	private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class.getName());

	private static String proxyUrl;
	private static String proxyUsername;
	private static String proxyPassword;
	private static String cookie;
	private static String charset;

	private static CloseableHttpClient client;

	private static void configure(final HttpRequestBase req, final String requestCharset, final String username, final String password, final String proxyUrlParameter, final String proxyUsernameParameter, final String proxyPasswordParameter, final String cookieParameter, final Map<String, String> headers, final boolean followRedirects, final boolean validateCertificates) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

		if (StringUtils.isBlank(requestCharset)) {
			charset = Settings.HttpDefaultCharset.getValue();
		} else {
			charset = requestCharset;
		}

		if (StringUtils.isBlank(proxyUrlParameter)) {
			proxyUrl = Settings.HttpProxyUrl.getValue();
		} else {
			proxyUrl = proxyUrlParameter;
		}

		if (StringUtils.isBlank(proxyUsernameParameter)) {
			proxyUsername = Settings.HttpProxyUser.getValue();
		} else {
			proxyUsername = proxyUsernameParameter;
		}

		if (StringUtils.isBlank(proxyPasswordParameter)) {
			proxyPassword = Settings.HttpProxyPassword.getValue();
		} else {
			proxyPassword = proxyPasswordParameter;
		}

		if (!StringUtils.isBlank(cookieParameter)) {
			cookie = cookieParameter;
		}

		HttpHost proxy                          = null;
		final CredentialsProvider credsProvider = new BasicCredentialsProvider();

		if (StringUtils.isNoneBlank(username, password)) {

			credsProvider.setCredentials(
				new AuthScope(new HttpHost(req.getURI().getHost())),
				new UsernamePasswordCredentials(username, password)
			);
		}

		if (StringUtils.isNotBlank(proxyUrl)) {

			proxy = HttpHost.create(proxyUrl);

			if (StringUtils.isNoneBlank(proxyUsername, proxyPassword)) {

				credsProvider.setCredentials(
					new AuthScope(proxy),
					new UsernamePasswordCredentials(proxyUsername, proxyPassword)
				);
			}
		}

		final HttpClientBuilder clientBuilder = HttpClients.custom()
				.setDefaultConnectionConfig(ConnectionConfig.DEFAULT)
				.setUserAgent(Settings.HttpUserAgent.getValue())
				.setDefaultCredentialsProvider(credsProvider);

		if (Boolean.FALSE.equals(validateCertificates)) {

			// trust every certificate
			final SSLContext sslContext = SSLContexts.custom().loadTrustMaterial((x509Certificates, s) -> true).build();

			final SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new String[]{"SSLv2Hello", "SSLv3", "TLSv1","TLSv1.1", "TLSv1.2" }, null, NoopHostnameVerifier.INSTANCE);

			final BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", sslConnectionSocketFactory).build()
			);

			clientBuilder.setConnectionManager(connectionManager);
		}

		client = clientBuilder.build();

		final RequestConfig reqConfig = RequestConfig.custom()
			.setProxy(proxy)
			.setRedirectsEnabled(followRedirects)
			.setCookieSpec(CookieSpecs.STANDARD)
			.setConnectTimeout(Settings.HttpConnectTimeout.getValue() * 1000)
			.setSocketTimeout(Settings.HttpSocketTimeout.getValue() * 1000)
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
	}

	public static String skipBOMIfPresent (final String content) {

		// Skip BOM to work around this Jsoup bug: https://github.com/jhy/jsoup/issues/348
		if (content != null && content.length() > 1 && content.charAt(0) == 65279) {
			charset = "UTF-8";
			return content.substring(1);
		}

		return content;
	}

	public static CloseableHttpClient getClient(final HttpRequestBase req, final String requestCharset, final String username, final String password, final String proxyUrlParameter, final String proxyUsernameParameter, final String proxyPasswordParameter, final String cookieParameter, final Map<String, String> headers, final boolean followRedirects, final boolean validateCertificates) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

		configure(req, requestCharset, username, password, proxyUrlParameter, proxyUsernameParameter, proxyPasswordParameter, cookieParameter, headers, followRedirects, validateCertificates);
		return client;
	}

	public static String get(final String address) throws FrameworkException {

		return get(address, null, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static String get(final String address, final String charset) throws FrameworkException {

		return get(address, charset, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static String get(final String address, final String charset, final Map<String, String> headers, final boolean validateCertificates)	throws FrameworkException {

		return get(address, charset, null, null, headers, validateCertificates);
	}

	public static String get(final String address, final String charset, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return get(address, charset, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static String get(final String address, final String charset, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return get(address, charset, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static String get(final String address, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		String content = "";

		try {

			final URI     uri = new URL(address).toURI();
			final HttpGet req = new HttpGet(uri);

			configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, validateCertificates);

			final CloseableHttpResponse resp = client.execute(req);

			content = skipBOMIfPresent(IOUtils.toString(resp.getEntity().getContent(), charset(resp)));

		} catch (final Throwable t) {
			throw new FrameworkException(422, "Unable to fetch content from address " + address + ": " + t.getMessage(), t);
		}

		return content;
	}

	public static byte[] getBinary(final String address, final String charset, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return getBinary(address, charset, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static byte[] getBinary(final String address, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		try {

			final URI     uri = new URL(address).toURI();
			final HttpGet req = new HttpGet(uri);

			configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, validateCertificates);

			return IOUtils.toByteArray(getAsStream(address, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers));

		} catch (final Throwable t) {
			logger.error("Error while downloading binary data from " + address, t);
			throw new FrameworkException(422, "Error while downloading binary data from " + address + ": " + t.getMessage(), t);
		}
	}

	public static Map<String, String> head(final String address, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return head(address, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, String> head(final String address, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return head(address, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static Map<String, String> head(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		final Map<String, String> responseHeaders = new HashMap<>();

		try {

			final URI      uri = new URL(address).toURI();
			final HttpHead req = new HttpHead(uri);

			configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, false, validateCertificates);

			final CloseableHttpResponse response = client.execute(req);

			responseHeaders.put("status", Integer.toString(response.getStatusLine().getStatusCode()));
			addHeaders(responseHeaders, response);

		} catch (final Throwable t) {

			logger.error("Unable to get headers from address {}, {}", new Object[] { address, t.getMessage() });
			throw new FrameworkException(422, "Unable to get headers from address " + address + ": " + t.getMessage(), t);
		}

		return responseHeaders;
	}

	public static Map<String, String> patch(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {

		return patch(address, requestBody, username, password, null, null, null, null, headers, charset, validateCertificates);
	}

	public static Map<String, String> patch(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {

		final Map<String, String> responseData = new HashMap<>();

		try {

			final URI url     = URI.create(address);
			final HttpPut req = new HttpPatch(url);

			configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, validateCertificates);

			req.setEntity(new StringEntity(requestBody, charset));

			final CloseableHttpResponse response = client.execute(req);
			final HttpEntity entity = response.getEntity();
			String content = null;

			if (entity != null) {

				final InputStream responseContent = entity.getContent();

				if (responseContent != null) {

					content = IOUtils.toString(responseContent, charset(response));
				}
			}

			content = skipBOMIfPresent(content);

			responseData.put("body", content);
			responseData.put("status", Integer.toString(response.getStatusLine().getStatusCode()));

			addHeaders(responseData, response);

		} catch (final Throwable t) {

			logger.error("Unable to issue PATCH request to address {}, {}", new Object[] { address, t.getMessage() });
			throw new FrameworkException(422, "Unable to issue PATCH request to address " + address + ": " + t.getMessage(), t);
		}

		return responseData;
	}

	public static Map<String, String> post(final String address, final String requestBody)	throws FrameworkException {

		return post(address, requestBody, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static Map<String, String> post(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates)	throws FrameworkException {

		return post(address, requestBody, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, String> post(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers, final String charset, final boolean validateCertificates)	throws FrameworkException {

		return post(address, requestBody, username, password, null, null, null, null, headers, charset, validateCertificates);
	}

	public static Map<String, String> post(final String address, final String requestBody, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates)	throws FrameworkException {

		return post(address, requestBody, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static Map<String, String> post(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates)	throws FrameworkException {

		return post(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, "UTF-8", validateCertificates);
	}

	public static Map<String, String> post(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {

		final Map<String, String> responseData = new HashMap<>();

		try {

			final URI      uri = new URL(address).toURI();
			final HttpPost req = new HttpPost(uri);

			configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, validateCertificates);

			req.setEntity(new StringEntity(requestBody, charset));

			final CloseableHttpResponse response = client.execute(req);
			final HttpEntity responseEntity = response.getEntity();

			String content = null;
			if (responseEntity != null) {
				content = IOUtils.toString(responseEntity.getContent(), charset(response));
			}

			content = skipBOMIfPresent(content);

			responseData.put("body", content);
			responseData.put("status", Integer.toString(response.getStatusLine().getStatusCode()));

			addHeaders(responseData, response);

		} catch (final Throwable t) {

			logger.error("Unable to issue POST request to address {}, {}", new Object[]{address, t.getMessage()});
			throw new FrameworkException(422, "Unable to issue POST request to address " + address + ": " + t.getCause() + " " + (t.getMessage() != null ? t.getMessage() : ""), t);
		}

		return responseData;
	}

	public static Map<String, String> put(final String address, final String requestBody) throws FrameworkException {
		return put(address, requestBody, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static Map<String, String> put(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {
		return put(address, requestBody, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, String> put(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {
		return put(address, requestBody, username, password, null, null, null, null, headers, charset, validateCertificates);
	}

	public static Map<String, String> put(final String address, final String requestBody, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {
		return put(address, requestBody, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static Map<String, String> put(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {
		return put(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, "UTF-8", validateCertificates);
	}

	public static Map<String, String> put(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset, final boolean validateCertificates) throws FrameworkException {

		final Map<String, String> responseData = new HashMap<>();

		try {

			final URI     uri = new URL(address).toURI();
			final HttpPut req = new HttpPut(uri);

			configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, validateCertificates);

			req.setEntity(new StringEntity(requestBody, charset));

			final CloseableHttpResponse response = client.execute(req);
			final HttpEntity responseEntity = response.getEntity();

			String content = null;
			if (responseEntity != null) {
				content = IOUtils.toString(responseEntity.getContent(), charset(response));
			}

			content = skipBOMIfPresent(content);

			responseData.put("body", content);
			responseData.put("status", Integer.toString(response.getStatusLine().getStatusCode()));

			addHeaders(responseData, response);

		} catch (final Throwable t) {

			logger.error("Unable to issue PUT request to address {}, {}", new Object[] { address, t.getMessage() });
			throw new FrameworkException(422, "Unable to issue PUT request to address " + address + ": " + t.getMessage(), t);
		}

		return responseData;
	}

	public static Map<String, String> delete(final String address) throws FrameworkException {

		return delete(address, null, null, null, null, Collections.EMPTY_MAP, true);
	}

	public static Map<String, String> delete(final String address, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return delete(address, username, password, null, null, null, null, headers, validateCertificates);
	}

	public static Map<String, String> delete(final String address, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		return delete(address, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, validateCertificates);
	}

	public static Map<String, String> delete(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final boolean validateCertificates) throws FrameworkException {

		final Map<String, String> responseData = new HashMap<>();

		try {

			final URI        uri = new URL(address).toURI();
			final HttpDelete req = new HttpDelete(uri);

			configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, validateCertificates);

			final CloseableHttpResponse response = client.execute(req);
			final HttpEntity responseEntity = response.getEntity();

			String content = null;
			if (responseEntity != null) {
				content = IOUtils.toString(responseEntity.getContent(), charset(response));
			}

			content = skipBOMIfPresent(content);

			responseData.put("body", content);
			responseData.put("status", Integer.toString(response.getStatusLine().getStatusCode()));

			addHeaders(responseData, response);

		} catch (final Throwable t) {

			logger.error("Unable to issue DELETE command to address {}, {}", new Object[] { address, t.getMessage() });
			throw new FrameworkException(422, "Unable to issue DELETE command to address " + address + ": " + t.getMessage(), t);
		}

		return responseData;
	}

	public static InputStream getAsStream(final String address) {

		return getAsStream(address, null, null, null, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static InputStream getAsStream(final String address, final String charset) {

		return getAsStream(address, charset, null, null, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static InputStream getAsStream(final String address, final String charset, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {

		try {

			final URI     uri = new URL(address).toURI();
			final HttpGet req = new HttpGet(uri);

			configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, true);

			final CloseableHttpResponse resp = client.execute(req);

			return resp.getEntity().getContent();

		} catch (final Throwable t) {

			logger.error("Unable to get content stream from address {}, {}", new Object[] { address, t.getMessage() });
		}

		return null;
	}

	public static String charset(final HttpResponse response) {

		final ContentType contentType = ContentType.get(response.getEntity());
		if (contentType != null && contentType.getCharset() != null) {
			charset = contentType.getCharset().toString();
		}

		return charset;
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

		try {

			final URI     url = URI.create(address);
			final HttpGet req = new HttpGet(url);

			logger.info("Downloading from {}", address);

			configure(req, charset, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true, true);

			req.addHeader("User-Agent", "curl/7.35.0");

			final CloseableHttpResponse resp = client.execute(req);

			final int statusCode = resp.getStatusLine().getStatusCode();

			if (statusCode == 200) {

				try (final InputStream is = resp.getEntity().getContent()) {

					try (final OutputStream os = new FileOutputStream(fileOnDisk)) {

						IOUtils.copy(is, os);
					}
				}

			} else {

				String content = IOUtils.toString(resp.getEntity().getContent(), HttpHelper.charset(resp));

				content = skipBOMIfPresent(content);

				logger.warn("Unable to create file from URI {}: status code was {}", new Object[]{ address, statusCode });
			}

		} catch (final Throwable t) {
			throw new FrameworkException(422, "Unable to fetch file content from address " + address + ": " + t.getMessage());
		}
	}

	private static void addHeaders(final Map<String, String> map, final HttpResponse response) {
		for (final Header header : response.getAllHeaders()) {

			final String key = header.getName();
			if (map.containsKey(key)) {
				map.put(key, String.join(System.lineSeparator(), map.get(key), header.getValue()));
			} else {
				map.put(header.getName(), header.getValue());
			}
		}
	}

	// ----- nested classes -----
	public static class HttpPatch extends HttpPut {

		public HttpPatch() {
			super();
		}

		public HttpPatch(final URI uri) throws MalformedURLException, URISyntaxException {
			super(uri.toURL().toURI());
		}

		@Override
		public String getMethod() {
			return "PATCH";
		}
	}
}
