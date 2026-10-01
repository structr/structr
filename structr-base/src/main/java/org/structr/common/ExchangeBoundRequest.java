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
package org.structr.common;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.websocket.DetachedHttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * A view of a Jetty request that reads through to it only while its HTTP exchange is in progress.
 *
 * Jetty recycles a request when its exchange completes and reuses the underlying channel for the next
 * request on the same connection. A request object read after that point throws a NullPointerException
 * from inside Jetty, or, once the next request has been associated with the channel, answers with that
 * request's headers, cookies and parameters. A SecurityContext outlives its exchange whenever code runs
 * after the response has been completed, e.g. a scheduled function or a transaction that commits after
 * the output of an asynchronous render has been written.
 *
 * This view therefore captures the core request of the exchange at construction and compares it with
 * the one the Jetty request currently belongs to on every call. While they are the same, every call is
 * delegated unchanged. Once they differ, every call is answered by a {@link DetachedHttpServletRequest}
 * snapshot taken at construction: request data stays readable, and there is no session, body, dispatch
 * or async support.
 *
 * Every method of {@link HttpServletRequest} is overridden, because a method inherited from
 * {@link HttpServletRequestWrapper} would read the recycled request unconditionally.
 */
public class ExchangeBoundRequest extends HttpServletRequestWrapper {

	private static final Logger logger = LoggerFactory.getLogger(ExchangeBoundRequest.class);

	private final HttpServletRequest live;
	private final ServletApiRequest apiRequest;
	private final Request coreRequest;
	private final DetachedHttpServletRequest snapshot;

	private ExchangeBoundRequest(final HttpServletRequest live, final ServletApiRequest apiRequest, final Request coreRequest, final DetachedHttpServletRequest snapshot) {

		super(live);

		this.live        = live;
		this.apiRequest  = apiRequest;
		this.coreRequest = coreRequest;
		this.snapshot    = snapshot;
	}

	/**
	 * Returns a view of the given request that is safe to keep beyond its exchange, or the request itself
	 * if it is not backed by a live Jetty request (null, already bound, a detached snapshot, a mock).
	 */
	public static HttpServletRequest bind(final HttpServletRequest request) {

		if (request == null || request instanceof ExchangeBoundRequest) {
			return request;
		}

		final ServletApiRequest apiRequest = findApiRequest(request);
		if (apiRequest == null) {
			return request;
		}

		final Request coreRequest = apiRequest.getRequest();
		if (coreRequest == null) {

			// the exchange has already completed, so there is nothing left to take a snapshot of
			return request;
		}

		try {

			return new ExchangeBoundRequest(request, apiRequest, coreRequest, new DetachedHttpServletRequest(request));

		} catch (RuntimeException ex) {

			logger.warn("Unable to take a snapshot of request {}, using it unbound: {}", request.getRequestURI(), ex.getMessage());
			return request;
		}
	}

	/** True while the exchange this request was created for has not completed. */
	public boolean isExchangeActive() {
		return apiRequest.getRequest() == coreRequest;
	}

	private HttpServletRequest current() {
		return isExchangeActive() ? live : snapshot;
	}

	private static ServletApiRequest findApiRequest(final ServletRequest request) {

		ServletRequest current = request;

		while (current != null) {

			if (current instanceof ServletApiRequest apiRequest) {
				return apiRequest;
			}

			if (current instanceof ServletRequestWrapper wrapper) {

				current = wrapper.getRequest();

			} else {

				return null;
			}
		}

		return null;
	}

	// ----- ServletRequest -----
	@Override
	public Object getAttribute(final String name) {
		return current().getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return current().getAttributeNames();
	}

	@Override
	public String getCharacterEncoding() {
		return current().getCharacterEncoding();
	}

	@Override
	public void setCharacterEncoding(final String env) throws UnsupportedEncodingException {
		current().setCharacterEncoding(env);
	}

	@Override
	public int getContentLength() {
		return current().getContentLength();
	}

	@Override
	public long getContentLengthLong() {
		return current().getContentLengthLong();
	}

	@Override
	public String getContentType() {
		return current().getContentType();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return current().getInputStream();
	}

	@Override
	public String getParameter(final String name) {
		return current().getParameter(name);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return current().getParameterNames();
	}

	@Override
	public String[] getParameterValues(final String name) {
		return current().getParameterValues(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return current().getParameterMap();
	}

	@Override
	public String getProtocol() {
		return current().getProtocol();
	}

	@Override
	public String getScheme() {
		return current().getScheme();
	}

	@Override
	public String getServerName() {
		return current().getServerName();
	}

	@Override
	public int getServerPort() {
		return current().getServerPort();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return current().getReader();
	}

	@Override
	public String getRemoteAddr() {
		return current().getRemoteAddr();
	}

	@Override
	public String getRemoteHost() {
		return current().getRemoteHost();
	}

	@Override
	public void setAttribute(final String name, final Object o) {
		current().setAttribute(name, o);
	}

	@Override
	public void removeAttribute(final String name) {
		current().removeAttribute(name);
	}

	@Override
	public Locale getLocale() {
		return current().getLocale();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return current().getLocales();
	}

	@Override
	public boolean isSecure() {
		return current().isSecure();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(final String path) {
		return current().getRequestDispatcher(path);
	}

	@Override
	public int getRemotePort() {
		return current().getRemotePort();
	}

	@Override
	public String getLocalName() {
		return current().getLocalName();
	}

	@Override
	public String getLocalAddr() {
		return current().getLocalAddr();
	}

	@Override
	public int getLocalPort() {
		return current().getLocalPort();
	}

	@Override
	public ServletContext getServletContext() {
		return current().getServletContext();
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return current().startAsync();
	}

	@Override
	public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) throws IllegalStateException {
		return current().startAsync(servletRequest, servletResponse);
	}

	@Override
	public boolean isAsyncStarted() {
		return current().isAsyncStarted();
	}

	@Override
	public boolean isAsyncSupported() {
		return current().isAsyncSupported();
	}

	@Override
	public AsyncContext getAsyncContext() {
		return current().getAsyncContext();
	}

	@Override
	public DispatcherType getDispatcherType() {
		return current().getDispatcherType();
	}

	@Override
	public String getRequestId() {
		return current().getRequestId();
	}

	@Override
	public String getProtocolRequestId() {
		return current().getProtocolRequestId();
	}

	@Override
	public ServletConnection getServletConnection() {
		return current().getServletConnection();
	}

	// ----- HttpServletRequest -----
	@Override
	public String getAuthType() {
		return current().getAuthType();
	}

	@Override
	public Cookie[] getCookies() {
		return current().getCookies();
	}

	@Override
	public long getDateHeader(final String name) {
		return current().getDateHeader(name);
	}

	@Override
	public String getHeader(final String name) {
		return current().getHeader(name);
	}

	@Override
	public Enumeration<String> getHeaders(final String name) {
		return current().getHeaders(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return current().getHeaderNames();
	}

	@Override
	public int getIntHeader(final String name) {
		return current().getIntHeader(name);
	}

	@Override
	public HttpServletMapping getHttpServletMapping() {
		return current().getHttpServletMapping();
	}

	@Override
	public String getMethod() {
		return current().getMethod();
	}

	@Override
	public String getPathInfo() {
		return current().getPathInfo();
	}

	@Override
	public String getPathTranslated() {
		return current().getPathTranslated();
	}

	@Override
	public String getContextPath() {
		return current().getContextPath();
	}

	@Override
	public String getQueryString() {
		return current().getQueryString();
	}

	@Override
	public String getRemoteUser() {
		return current().getRemoteUser();
	}

	@Override
	public boolean isUserInRole(final String role) {
		return current().isUserInRole(role);
	}

	@Override
	public Principal getUserPrincipal() {
		return current().getUserPrincipal();
	}

	@Override
	public String getRequestedSessionId() {
		return current().getRequestedSessionId();
	}

	@Override
	public String getRequestURI() {
		return current().getRequestURI();
	}

	@Override
	public StringBuffer getRequestURL() {
		return current().getRequestURL();
	}

	@Override
	public String getServletPath() {
		return current().getServletPath();
	}

	@Override
	public HttpSession getSession(final boolean create) {
		return current().getSession(create);
	}

	@Override
	public HttpSession getSession() {
		return current().getSession();
	}

	@Override
	public String changeSessionId() {
		return current().changeSessionId();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return current().isRequestedSessionIdValid();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return current().isRequestedSessionIdFromCookie();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return current().isRequestedSessionIdFromURL();
	}

	@Override
	public boolean authenticate(final HttpServletResponse response) throws IOException, ServletException {
		return current().authenticate(response);
	}

	@Override
	public void login(final String username, final String password) throws ServletException {
		current().login(username, password);
	}

	@Override
	public void logout() throws ServletException {
		current().logout();
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return current().getParts();
	}

	@Override
	public Part getPart(final String name) throws IOException, ServletException {
		return current().getPart(name);
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(final Class<T> handlerClass) throws IOException, ServletException {
		return current().upgrade(handlerClass);
	}

	@Override
	@SuppressWarnings("deprecation")
	public jakarta.servlet.http.PushBuilder newPushBuilder() {
		return current().newPushBuilder();
	}

	@Override
	public Map<String, String> getTrailerFields() {
		return current().getTrailerFields();
	}

	@Override
	public boolean isTrailerFieldsReady() {
		return current().isTrailerFieldsReady();
	}
}
