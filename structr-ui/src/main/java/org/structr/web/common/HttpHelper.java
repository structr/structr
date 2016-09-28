/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.structr.core.Services;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import static org.structr.web.entity.dom.DOMNode.extractHeaders;

/**
 * Helper class for outbound HTTP requests
 */
public class HttpHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class.getName());
	
	private static String address;
	private static String proxyUrl;
	private static String proxyUsername;
	private static String proxyPassword;
	private static String cookie;
	
	private static CloseableHttpClient client;
	private static RequestConfig reqConfig;
	
	private static void configure(final HttpRequestBase req, final String proxyUrlParameter, final String proxyUsernameParameter, final String proxyPasswordParameter, final String cookieParameter, final Map<String, String> headers, final boolean followRedirects) {

		if (StringUtils.isBlank(proxyUrlParameter)) {
			proxyUrl = Services.getBaseConfiguration().getProperty(Services.APPLICATION_PROXY_HTTP_URL);
		}

		if (StringUtils.isBlank(proxyUsernameParameter)) {
			proxyUsername = Services.getBaseConfiguration().getProperty(Services.APPLICATION_PROXY_HTTP_USERNAME);
		}

		if (StringUtils.isBlank(proxyPasswordParameter)) {
			proxyPassword = Services.getBaseConfiguration().getProperty(Services.APPLICATION_PROXY_HTTP_PASSWORD);
		}

		//final HttpHost target             = HttpHost.create(url.getHost());
		HttpHost proxy                    = null;
		CredentialsProvider credsProvider = null;

		if (StringUtils.isNotBlank(proxyUrl)) {

			proxy  = HttpHost.create(proxyUrl);

			credsProvider = new BasicCredentialsProvider();

			if (StringUtils.isNoneBlank(proxyUsername, proxyPassword)) {

				credsProvider.setCredentials(
					new AuthScope(proxy),
					new UsernamePasswordCredentials(proxyUsername, proxyPassword)
				);
			}

		}

		client = HttpClients.custom()
			.setDefaultConnectionConfig(ConnectionConfig.DEFAULT)
			.setUserAgent("curl/7.35.0")
			.setDefaultCredentialsProvider(credsProvider)
			.build();

		reqConfig = RequestConfig.custom()
			.setProxy(proxy)
			.setRedirectsEnabled(followRedirects)
			.setCookieSpec(CookieSpecs.DEFAULT)
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
	
	public static String get(final String address) {
		return get(address, null, null, null, null, Collections.EMPTY_MAP);
	}
	
	public static String get(final String address, String proxyUrl, String proxyUsername, String proxyPassword, String cookie, Map<String, String> headers) {
				
		String content = "";

		try {
		
			final URI url = URI.create(address);
			final HttpGet req = new HttpGet(url);

			configure(req, proxyUrl, proxyUsername, proxyPassword, cookie, headers, true);

			final CloseableHttpResponse resp = client.execute(req);

			final Header contentType = resp.getFirstHeader("Content-Type");
			String       charset     = StringUtils.substringAfterLast(contentType.getValue(), "; charset=");

			// default charset is UTF-8
			if (StringUtils.isBlank(charset)) {
				charset = "UTF-8";
			}

			content = IOUtils.toString(resp.getEntity().getContent(), charset);

			// Skip BOM to workaround this Jsoup bug: https://github.com/jhy/jsoup/issues/348
			if (content.charAt(0) == 65279) {
				content = content.substring(1);
			}

		} catch (final Throwable t) {
			
			logger.error("Unable to fetch content from address " + address + ": ", t.getMessage());
			return t.getMessage();
			
		}
		
		return content;

	}
	
	public static Map<String, String> head(final String address, String proxyUrl, String proxyUsername, String proxyPassword, String cookie, Map<String, String> headers) {
				
		final Map<String, String> responseHeaders = new HashMap<>();

		try {
			final URI url = URI.create(address);
			final HttpHead req = new HttpHead(url);

			configure(req, proxyUrl, proxyUsername, proxyPassword, cookie, headers, false);

			final CloseableHttpResponse response = client.execute(req);

			responseHeaders.put("status", Integer.toString(response.getStatusLine().getStatusCode()));
			for (final Header header : response.getAllHeaders()) {
				
				responseHeaders.put(header.getName(), header.getValue());
				
			}

		} catch (final Throwable t) {
			
			logger.error("Unable to get headers from address " + address + ": ", t.getMessage());
			
		}

		return responseHeaders;

	}
}
