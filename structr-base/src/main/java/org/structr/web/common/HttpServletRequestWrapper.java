/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.common;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

public class HttpServletRequestWrapper implements HttpServletRequest {
	private final HttpServletRequest request;
	private final String url;

	public HttpServletRequestWrapper(final HttpServletRequest request, final String url) {
		this.request = request;
		this.url = url;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return new IteratorEnumeration(getParameterMap().keySet().iterator());
	}

	@Override
	public String[] getParameterValues(String s) {
		return getParameterMap().get(s);
	}

	@Override
	public Object getAttribute(String s) {
		return request.getAttribute(s);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return request.getAttributeNames();
	}

	@Override
	public String getCharacterEncoding() {
		return request.getCharacterEncoding();
	}

	@Override
	public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
		request.setCharacterEncoding(s);
	}

	@Override
	public int getContentLength() {
		return request.getContentLength();
	}

	@Override
	public long getContentLengthLong() {
		return request.getContentLengthLong();
	}

	@Override
	public String getContentType() {
		return request.getContentType();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	@Override
	public String getParameter(String key) {
		String[] p = getParameterMap().get(key);
		return p != null ? p[0] : null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		String[] parts = StringUtils.split(getQueryString(), "&");
		Map<String, String[]> parameterMap = new HashMap();

		for (String p : parts) {
			String[] kv = StringUtils.split(p, "=");
			if (kv.length > 1) {
				parameterMap.put(kv[0], new String[]{kv[1]});
			}
		}

		return parameterMap;
	}

	@Override
	public String getProtocol() {
		return request.getProtocol();
	}

	@Override
	public String getScheme() {
		return request.getScheme();
	}

	@Override
	public String getServerName() {
		return request.getServerName();
	}

	@Override
	public int getServerPort() {
		return request.getServerPort();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return request.getReader();
	}

	@Override
	public String getRemoteAddr() {
		return request.getRemoteAddr();
	}

	@Override
	public String getRemoteHost() {
		return request.getRemoteHost();
	}

	@Override
	public void setAttribute(String s, Object o) {
		request.setAttribute(s, o);
	}

	@Override
	public void removeAttribute(String s) {
		request.removeAttribute(s);
	}

	@Override
	public Locale getLocale() {
		return request.getLocale();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return request.getLocales();
	}

	@Override
	public boolean isSecure() {
		return request.isSecure();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String s) {
		return request.getRequestDispatcher(s);
	}

	@Override
	public String getRealPath(String s) {
		return request.getServletContext().getRealPath(s);
	}

	@Override
	public int getRemotePort() {
		return request.getRemotePort();
	}

	@Override
	public String getLocalName() {
		return request.getLocalName();
	}

	@Override
	public String getLocalAddr() {
		return request.getLocalAddr();
	}

	@Override
	public int getLocalPort() {
		return request.getLocalPort();
	}

	@Override
	public ServletContext getServletContext() {
		return request.getServletContext();
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return request.startAsync();
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
		return request.startAsync(servletRequest, servletResponse);
	}

	@Override
	public boolean isAsyncStarted() {
		return request.isAsyncStarted();
	}

	@Override
	public boolean isAsyncSupported() {
		return request.isAsyncSupported();
	}

	@Override
	public AsyncContext getAsyncContext() {
		return request.getAsyncContext();
	}

	@Override
	public DispatcherType getDispatcherType() {
		return request.getDispatcherType();
	}

	@Override
	public String getQueryString() {
		return StringUtils.substringAfter(url, "?");
	}

	@Override
	public String getRemoteUser() {
		return request.getRemoteUser();
	}

	@Override
	public boolean isUserInRole(String s) {
		return request.isUserInRole(s);
	}

	@Override
	public Principal getUserPrincipal() {
		return request.getUserPrincipal();
	}

	@Override
	public String getRequestedSessionId() {
		return request.getRequestedSessionId();
	}

	@Override
	public String getRequestURI() {
		return request.getRequestURI();
	}

	@Override
	public String getAuthType() {
		return request.getAuthType();
	}

	@Override
	public Cookie[] getCookies() {
		return new Cookie[0];
	}

	@Override
	public long getDateHeader(String s) {
		return request.getDateHeader(s);
	}

	@Override
	public String getHeader(String s) {
		return request.getHeader(s);
	}

	@Override
	public Enumeration<String> getHeaders(String s) {
		return request.getHeaders(s);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return request.getHeaderNames();
	}

	@Override
	public int getIntHeader(String s) {
		return request.getIntHeader(s);
	}

	@Override
	public String getMethod() {
		return request.getMethod();
	}

	@Override
	public String getPathInfo() {
		return StringUtils.substringBefore(url, "?");
	}

	@Override
	public String getPathTranslated() {
		return request.getPathTranslated();
	}

	@Override
	public String getContextPath() {
		return request.getContextPath();
	}

	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(url);
	}

	@Override
	public String getServletPath() {
		return request.getServletPath();
	}

	@Override
	public HttpSession getSession(boolean b) {
		return request.getSession(b);
	}

	@Override
	public HttpSession getSession() {
		return request.getSession();
	}

	@Override
	public String changeSessionId() {
		return request.changeSessionId();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return request.isRequestedSessionIdValid();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return request.isRequestedSessionIdFromCookie();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return request.isRequestedSessionIdFromURL();
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return request.isRequestedSessionIdFromURL();
	}

	@Override
	public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
		return request.authenticate(httpServletResponse);
	}

	@Override
	public void login(String s, String s1) throws ServletException {
		request.login(s, s1);
	}

	@Override
	public void logout() throws ServletException {
		request.logout();
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return request.getParts();
	}

	@Override
	public Part getPart(String s) throws IOException, ServletException {
		return request.getPart(s);
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
		return request.upgrade(aClass);
	}

}