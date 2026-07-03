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
package org.structr.websocket;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A detached, self-contained snapshot of an {@link HttpServletRequest}.
 *
 * WebSocket connections outlive the HTTP upgrade request that created them. In Jetty 12 the
 * underlying {@code ServletApiRequest} is recycled once the upgrade completes, so it cannot be
 * kept around for the later authentication / notification code paths. Instead of holding on to
 * Jetty's live request object (which previously required reflectively nulling a private field and
 * an {@code --add-opens} on the Jetty servlet module), we copy everything the WebSocket layer
 * actually reads into this immutable-ish snapshot at upgrade time, while the source request is
 * still valid.
 *
 * Request-scoped I/O (body, async, session mutation) is intentionally not supported — the source
 * request no longer exists once this snapshot is in use.
 */
public class DetachedHttpServletRequest implements HttpServletRequest {

	private final Map<String, List<String>> headers    = new LinkedHashMap<>();
	private final Map<String, String[]> parameters     = new LinkedHashMap<>();
	private final Map<String, Object> attributes       = new HashMap<>();
	private final List<Locale> locales                 = new ArrayList<>();

	private Cookie[] cookies                = null;
	private ServletContext servletContext   = null;

	private String method                   = null;
	private String protocol                 = null;
	private String scheme                   = null;
	private boolean secure                  = false;

	private String serverName               = null;
	private int serverPort                  = 0;

	private String remoteAddr               = null;
	private String remoteHost               = null;
	private int remotePort                  = 0;
	private String localName                = null;
	private String localAddr                = null;
	private int localPort                   = 0;

	private String contextPath              = null;
	private String servletPath              = null;
	private String pathInfo                 = null;
	private String pathTranslated           = null;
	private String requestURI               = null;
	private String requestURL               = null;
	private String queryString              = null;

	private String characterEncoding        = null;
	private String contentType              = null;
	private long contentLength              = -1;

	private String authType                 = null;
	private String remoteUser               = null;
	private java.security.Principal userPrincipal = null;
	private String requestedSessionId       = null;

	public DetachedHttpServletRequest(final HttpServletRequest source) {

		// headers (preserve order and original names; lookups are case-insensitive)
		final Enumeration<String> headerNames = source.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				final String name = headerNames.nextElement();
				headers.put(name, Collections.list(source.getHeaders(name)));
			}
		}

		// parameters (kept mutable: some commands add to the map, e.g. ListLocalizationsCommand)
		final Map<String, String[]> sourceParameters = source.getParameterMap();
		if (sourceParameters != null) {
			for (final Map.Entry<String, String[]> entry : sourceParameters.entrySet()) {
				parameters.put(entry.getKey(), entry.getValue() != null ? entry.getValue().clone() : null);
			}
		}

		// cookies
		final Cookie[] sourceCookies = source.getCookies();
		if (sourceCookies != null) {
			cookies = new Cookie[sourceCookies.length];
			for (int i = 0; i < sourceCookies.length; i++) {
				cookies[i] = (Cookie) sourceCookies[i].clone();
			}
		}

		// locales
		final Enumeration<Locale> sourceLocales = source.getLocales();
		if (sourceLocales != null) {
			locales.addAll(Collections.list(sourceLocales));
		}
		if (locales.isEmpty()) {
			locales.add(Locale.getDefault());
		}

		this.servletContext    = source.getServletContext();

		this.method            = source.getMethod();
		this.protocol          = source.getProtocol();
		this.scheme            = source.getScheme();
		this.secure            = source.isSecure();

		this.serverName        = source.getServerName();
		this.serverPort        = source.getServerPort();

		this.remoteAddr        = source.getRemoteAddr();
		this.remoteHost        = source.getRemoteHost();
		this.remotePort        = source.getRemotePort();
		this.localName         = source.getLocalName();
		this.localAddr         = source.getLocalAddr();
		this.localPort         = source.getLocalPort();

		this.contextPath       = source.getContextPath();
		this.servletPath       = source.getServletPath();
		this.pathInfo          = source.getPathInfo();
		this.pathTranslated    = source.getPathTranslated();
		this.requestURI        = source.getRequestURI();
		final StringBuffer url = source.getRequestURL();
		this.requestURL        = url != null ? url.toString() : null;
		this.queryString       = source.getQueryString();

		this.characterEncoding = source.getCharacterEncoding();
		this.contentType       = source.getContentType();
		this.contentLength     = source.getContentLengthLong();

		this.authType          = source.getAuthType();
		this.remoteUser        = source.getRemoteUser();
		this.userPrincipal     = source.getUserPrincipal();
		this.requestedSessionId = source.getRequestedSessionId();
	}

	private List<String> findHeaders(final String name) {

		for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(name)) {
				return entry.getValue();
			}
		}

		return null;
	}

	// ----- ServletRequest -----
	@Override
	public Object getAttribute(final String s) {
		return attributes.get(s);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(attributes.keySet());
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public void setCharacterEncoding(final String s) {
		this.characterEncoding = s;
	}

	@Override
	public int getContentLength() {
		return (contentLength > Integer.MAX_VALUE) ? -1 : (int) contentLength;
	}

	@Override
	public long getContentLengthLong() {
		return contentLength;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {

		final ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);

		return new ServletInputStream() {

			@Override
			public boolean isFinished() {
				return true;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(final ReadListener readListener) {
			}

			@Override
			public int read() throws IOException {
				return in.read();
			}
		};
	}

	@Override
	public String getParameter(final String key) {
		final String[] values = parameters.get(key);
		return (values != null && values.length > 0) ? values[0] : null;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(parameters.keySet());
	}

	@Override
	public String[] getParameterValues(final String s) {
		return parameters.get(s);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return parameters;
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	@Override
	public String getScheme() {
		return scheme;
	}

	@Override
	public String getServerName() {
		return serverName;
	}

	@Override
	public int getServerPort() {
		return serverPort;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8));
	}

	@Override
	public String getRemoteAddr() {
		return remoteAddr;
	}

	@Override
	public String getRemoteHost() {
		return remoteHost;
	}

	@Override
	public void setAttribute(final String s, final Object o) {
		if (o == null) {
			attributes.remove(s);
		} else {
			attributes.put(s, o);
		}
	}

	@Override
	public void removeAttribute(final String s) {
		attributes.remove(s);
	}

	@Override
	public Locale getLocale() {
		return locales.get(0);
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return Collections.enumeration(locales);
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(final String s) {
		return null;
	}

	@Override
	public int getRemotePort() {
		return remotePort;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	@Override
	public String getLocalAddr() {
		return localAddr;
	}

	@Override
	public int getLocalPort() {
		return localPort;
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new IllegalStateException("Async is not supported on a detached WebSocket request.");
	}

	@Override
	public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) throws IllegalStateException {
		throw new IllegalStateException("Async is not supported on a detached WebSocket request.");
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		throw new IllegalStateException("Async is not supported on a detached WebSocket request.");
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.REQUEST;
	}

	@Override
	public String getRequestId() {
		return "";
	}

	@Override
	public String getProtocolRequestId() {
		return "";
	}

	@Override
	public ServletConnection getServletConnection() {
		return null;
	}

	// ----- HttpServletRequest -----
	@Override
	public String getAuthType() {
		return authType;
	}

	@Override
	public Cookie[] getCookies() {
		return cookies;
	}

	@Override
	public long getDateHeader(final String s) {

		final String value = getHeader(s);
		if (value == null) {
			return -1;
		}

		try {
			return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
		} catch (final DateTimeParseException dpe) {
			throw new IllegalArgumentException("Cannot parse date header: " + value);
		}
	}

	@Override
	public String getHeader(final String s) {
		final List<String> values = findHeaders(s);
		return (values != null && !values.isEmpty()) ? values.get(0) : null;
	}

	@Override
	public Enumeration<String> getHeaders(final String s) {
		final List<String> values = findHeaders(s);
		return Collections.enumeration(values != null ? values : Collections.emptyList());
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(headers.keySet());
	}

	@Override
	public int getIntHeader(final String s) {
		final String value = getHeader(s);
		return (value != null) ? Integer.parseInt(value) : -1;
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public String getPathInfo() {
		return pathInfo;
	}

	@Override
	public String getPathTranslated() {
		return pathTranslated;
	}

	@Override
	public String getContextPath() {
		return contextPath;
	}

	@Override
	public String getQueryString() {
		return queryString;
	}

	@Override
	public String getRemoteUser() {
		return remoteUser;
	}

	@Override
	public boolean isUserInRole(final String s) {
		return false;
	}

	@Override
	public java.security.Principal getUserPrincipal() {
		return userPrincipal;
	}

	@Override
	public String getRequestedSessionId() {
		return requestedSessionId;
	}

	@Override
	public String getRequestURI() {
		return requestURI;
	}

	@Override
	public StringBuffer getRequestURL() {
		return (requestURL != null) ? new StringBuffer(requestURL) : null;
	}

	@Override
	public String getServletPath() {
		return servletPath;
	}

	@Override
	public HttpSession getSession(final boolean b) {
		return null;
	}

	@Override
	public HttpSession getSession() {
		return null;
	}

	@Override
	public String changeSessionId() {
		throw new IllegalStateException("No session associated with a detached WebSocket request.");
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	@Override
	public boolean authenticate(final HttpServletResponse httpServletResponse) {
		return false;
	}

	@Override
	public void login(final String s, final String s1) {
	}

	@Override
	public void logout() {
	}

	@Override
	public Collection<Part> getParts() {
		return Collections.emptyList();
	}

	@Override
	public Part getPart(final String s) {
		return null;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(final Class<T> aClass) {
		throw new UnsupportedOperationException("upgrade() is not supported on a detached WebSocket request.");
	}
}
