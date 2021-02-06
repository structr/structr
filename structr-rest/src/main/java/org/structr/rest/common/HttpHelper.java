/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;

/**
 * Helper class for outbound HTTP requests
 */
public class HttpHelper {

	private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class.getName());

	private static String proxyUrl;
	private static String proxyUsername;
	private static String proxyPassword;
	private static String cookie;

	private static CloseableHttpClient client;
	private static RequestConfig reqConfig;

	private static void configure(final HttpRequestBase req, final String username, final String password, final String proxyUrlParameter, final String proxyUsernameParameter, final String proxyPasswordParameter, final String cookieParameter, final Map<String, String> headers, final boolean followRedirects) {

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

		//final HttpHost target             = HttpHost.create(url.getHost());
		HttpHost proxy                    = null;
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

		client = HttpClients.custom()
			.setDefaultConnectionConfig(ConnectionConfig.DEFAULT)
			.setUserAgent(Settings.HttpUserAgent.getValue())
			.setDefaultCredentialsProvider(credsProvider)
			.build();

		reqConfig = RequestConfig.custom()
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

	private static String skipBOMIfPresent (final String content) {

		// Skip BOM to workaround this Jsoup bug: https://github.com/jhy/jsoup/issues/348
		if (content != null && content.length() > 1 && content.charAt(0) == 65279) {
			return content.substring(1);
		}

		return content;
	}

	public static String get(final String address)
	throws FrameworkException {
		return get(address, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static String get(final String address, final Map<String, String> headers)
	throws FrameworkException {
		return get(address, null, null, headers);
	}

	public static String get(final String address, final String username, final String password, final Map<String, String> headers)
	throws FrameworkException {
		return get(address, username, password, null, null, null, null, headers);
	}

	public static byte[] getBinary(final String address, final String username, final String password, final Map<String, String> headers)
	throws FrameworkException {
		return getBinary(address, username, password, null, null, null, null, headers);
	}

	public static String get(final String address, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers)
	throws FrameworkException {
		return get(address, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers);
	}

	public static String get(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers)
	throws FrameworkException {

		String content = "";

		try {

			final URI     url = URI.create(address);
			final HttpGet req = new HttpGet(url);

			configure(req, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true);

			final CloseableHttpResponse resp = client.execute(req);

			content = IOUtils.toString(resp.getEntity().getContent(), charset(resp));

			content = skipBOMIfPresent(content);

		} catch (final Throwable t) {
			throw new FrameworkException(422, "Unable to fetch content from address " + address + ": " + t.getMessage(), t);
		}

		return content;
	}

	public static byte[] getBinary(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) throws FrameworkException {

		try {

			final URI     url = URI.create(address);
			final HttpGet req = new HttpGet(url);

			configure(req, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true);

			return IOUtils.toByteArray(getAsStream(address, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers));

		} catch (final Throwable t) {
			logger.error("Error while dowloading binary data from " + address, t);
			throw new FrameworkException(422, "Unable to fetch binary data from address " + address + ": " + t.getMessage(), t);
		}
	}

	public static Map<String, String> head(final String address) {
		return head(address, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static Map<String, String> head(final String address, final String username, final String password, final Map<String, String> headers) {
		return head(address, username, password, null, null, null, null, headers);
	}

	public static Map<String, String> head(final String address, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {
		return head(address, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers);
	}

	public static Map<String, String> head(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {

		final Map<String, String> responseHeaders = new HashMap<>();

		try {

			final URI      url = URI.create(address);
			final HttpHead req = new HttpHead(url);

			configure(req, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, false);

			final CloseableHttpResponse response = client.execute(req);

			responseHeaders.put("status", Integer.toString(response.getStatusLine().getStatusCode()));
			for (final Header header : response.getAllHeaders()) {

				responseHeaders.put(header.getName(), header.getValue());
			}

		} catch (final Throwable t) {

			logger.error("Unable to get headers from address {}, {}", new Object[] { address, t.getMessage() });
		}

		return responseHeaders;
	}

	public static Map<String, String> patch(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers, final String charset) {
		return patch(address, requestBody, username, password, null, null, null, null, headers, charset);
	}

	public static Map<String, String> patch(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset) {

		final Map<String, String> responseData = new HashMap<>();

		try {

			final URI url     = URI.create(address);
			final HttpPut req = new HttpPatch(url);

			configure(req, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true);

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
			for (final Header header : response.getAllHeaders()) {

				responseData.put(header.getName(), header.getValue());
			}

		} catch (final Throwable t) {

			logger.error("Unable to fetch content from address {}, {}", new Object[] { address, t.getMessage() });
		}

		return responseData;
	}
	public static Map<String, String> post(final String address, final String requestBody) {
		return post(address, requestBody, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static Map<String, String> post(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers) {
		return post(address, requestBody, username, password, null, null, null, null, headers);
	}

	public static Map<String, String> post(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers, final String charset) {
		return post(address, requestBody, username, password, null, null, null, null, headers, charset);
	}

	public static Map<String, String> post(final String address, final String requestBody, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {
		return post(address, requestBody, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers);
	}

	public static Map<String, String> post(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {
		return post(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, "UTF-8");
	}

	public static Map<String, String> post(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset) {

		final Map<String, String> responseData = new HashMap<>();

		try {

			final URI      url = URI.create(address);
			final HttpPost req = new HttpPost(url);

			configure(req, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true);

			req.setEntity(new StringEntity(requestBody, charset));

			final CloseableHttpResponse response = client.execute(req);

			String content = IOUtils.toString(response.getEntity().getContent(), charset(response));

			content = skipBOMIfPresent(content);

			responseData.put("body", content);

			responseData.put("status", Integer.toString(response.getStatusLine().getStatusCode()));
			for (final Header header : response.getAllHeaders()) {

				responseData.put(header.getName(), header.getValue());
			}

		} catch (final Throwable t) {

			logger.error("Unable to fetch content from address {}, {}", new Object[] { address, t.getMessage() });
		}

		return responseData;
	}

	public static Map<String, String> put(final String address, final String requestBody) {
		return put(address, requestBody, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static Map<String, String> put(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers) {
		return put(address, requestBody, username, password, null, null, null, null, headers);
	}

	public static Map<String, String> put(final String address, final String requestBody, final String username, final String password, final Map<String, String> headers, final String charset) {
		return put(address, requestBody, username, password, null, null, null, null, headers, charset);
	}

	public static Map<String, String> put(final String address, final String requestBody, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {
		return put(address, requestBody, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers);
	}

	public static Map<String, String> put(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {
		return put(address, requestBody, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, "UTF-8");
	}

	public static Map<String, String> put(final String address, final String requestBody, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final String charset) {

		final Map<String, String> responseData = new HashMap<>();

		try {

			final URI      url = URI.create(address);
			final HttpPut req = new HttpPut(url);

			configure(req, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true);

			req.setEntity(new StringEntity(requestBody, charset));

			final CloseableHttpResponse response = client.execute(req);

			String content = IOUtils.toString(response.getEntity().getContent(), charset(response));

			content = skipBOMIfPresent(content);

			responseData.put("body", content);

			responseData.put("status", Integer.toString(response.getStatusLine().getStatusCode()));
			for (final Header header : response.getAllHeaders()) {

				responseData.put(header.getName(), header.getValue());
			}

		} catch (final Throwable t) {

			logger.error("Unable to fetch content from address {}, {}", new Object[] { address, t.getMessage() });
		}

		return responseData;
	}

	public static Map<String, String> delete(final String address) {
		return delete(address, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static Map<String, String> delete(final String address, final String username, final String password, final Map<String, String> headers) {
		return delete(address, username, password, null, null, null, null, headers);
	}

	public static Map<String, String> delete(final String address, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {
		return delete(address, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers);
	}

	public static Map<String, String> delete(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {

		final Map<String, String> responseData = new HashMap<>();

		try {

			final URI     url = URI.create(address);
			final HttpDelete req = new HttpDelete(url);

			configure(req, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true);

			final CloseableHttpResponse response = client.execute(req);

			String content = IOUtils.toString(response.getEntity().getContent(), charset(response));

			content = skipBOMIfPresent(content);

			responseData.put("body", content);

			responseData.put("status", Integer.toString(response.getStatusLine().getStatusCode()));
			for (final Header header : response.getAllHeaders()) {

				responseData.put(header.getName(), header.getValue());
			}
		} catch (final Throwable t) {

			logger.error("Unable to issue DELETE command to address {}, {}", new Object[] { address, t.getMessage() });
		}

		return responseData;
	}

	public static InputStream getAsStream(final String address) {

		return getAsStream(address, null, null, null, null, null, null, Collections.EMPTY_MAP);
	}

	public static InputStream getAsStream(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers) {

		try {

			final URI     url = URI.create(address);
			final HttpGet req = new HttpGet(url);

			configure(req, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true);

			final CloseableHttpResponse resp = client.execute(req);

			return resp.getEntity().getContent();

		} catch (final Throwable t) {

			logger.error("Unable to get content stream from address {}, {}", new Object[] { address, t.getMessage() });

		}

		return null;
	}

	public static String charset(final HttpResponse response) {

		final ContentType contentType = ContentType.get(response.getEntity());
		String charset = "UTF-8";
		if (contentType != null && contentType.getCharset() != null) {

			charset = contentType.getCharset().toString();
		}

		return charset;
	}

	public static void streamURLToFile(final String address, final java.io.File fileOnDisk)
	throws FrameworkException {
		streamURLToFile(address, null, null, null, null, Collections.EMPTY_MAP, fileOnDisk);
	}

	public static void streamURLToFile(final String address, final Map<String, String> headers, final java.io.File fileOnDisk)
	throws FrameworkException {
		streamURLToFile(address, null, null, headers, fileOnDisk);
	}

	public static void streamURLToFile(final String address, final String username, final String password, final Map<String, String> headers, final java.io.File fileOnDisk)
	throws FrameworkException {
		streamURLToFile(address, username, password, null, null, null, null, headers, fileOnDisk);
	}

	public static void streamURLToFile(final String address, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final java.io.File fileOnDisk)
	throws FrameworkException {
		streamURLToFile(address, null, null, proxyUrl, proxyUsername, proxyPassword, cookie, headers, fileOnDisk);
	}

	public static void streamURLToFile(final String address, final String username, final String password, final String proxyUrl, final String proxyUsername, final String proxyPassword, final String cookie, final Map<String, String> headers, final java.io.File fileOnDisk)
	throws FrameworkException {

		try {

			final URI     url = URI.create(address);
			final HttpGet req = new HttpGet(url);

			logger.info("Downloading from {}", address);

			configure(req, username, password, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true);

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

	// ----- nested classes -----
	public static class HttpPatch extends HttpPut {

		public HttpPatch() {
			super();
		}

		public HttpPatch(final URI uri) {
			super(uri);
		}

		@Override
		public String getMethod() {
			return "PATCH";
		}
	}
}
