/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.common;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import org.structr.core.Predicate;
import org.structr.core.Services;

/**
 *
 * @author chrisi
 */
public class StandaloneTestHelper
{
	public static void prepareStandaloneTest(String databasePath)
	{
		Services.initialize(prepareStandaloneContext(databasePath));
	}

	public static void finishStandaloneTest()
	{
		Services.shutdown();
	}

	/**
	 * Encapsulates Thread.sleep() in a try-catch-block.
	 * 
	 * @param millis
	 */
	public static void sleep(long millis)
	{
		try
		{
			Thread.sleep(millis);

		} catch(Throwable t)
		{
			// ignore
		}
	}

	// ----- private methods -----
	private static Map<String, Object> prepareStandaloneContext(String databasePath)
	{
		Map<String, Object> context = new Hashtable<String, Object>();

		context.put(Services.DATABASE_PATH, databasePath);

		try
		{
			Class.forName("javax.servlet.ServletContext");

		} catch(Throwable t)
		{
			t.printStackTrace();
		}

		// add synthetic ServletContext
		context.put(Services.SERVLET_CONTEXT, new ServletContext()
		{
			private Vector emptyList = new Vector();
			private Set emptySet = new LinkedHashSet();


			@Override
			public String getContextPath()
			{
				return("/dummy");
			}

			@Override
			public ServletContext getContext(String uripath)
			{
				return(this);
			}

			@Override
			public int getMajorVersion()
			{
				return(0);
			}

			@Override
			public int getMinorVersion()
			{
				return(0);
			}

			@Override
			public String getMimeType(String file)
			{
				return("application/octet-stream");
			}

			@Override
			public Set getResourcePaths(String path)
			{
				return(emptySet);
			}

			@Override
			public URL getResource(String path) throws MalformedURLException
			{
				return(null);
			}

			@Override
			public InputStream getResourceAsStream(String path)
			{
				return(null);
			}

			@Override
			public RequestDispatcher getRequestDispatcher(String path)
			{
				return(null);
			}

			@Override
			public RequestDispatcher getNamedDispatcher(String name)
			{
				return(null);
			}

			@Override
			public Servlet getServlet(String name) throws ServletException
			{
				return(null);
			}

			@Override
			public Enumeration getServlets()
			{
				return(emptyList.elements());
			}

			@Override
			public Enumeration getServletNames()
			{
				return(emptyList.elements());
			}

			@Override
			public void log(String msg)
			{
			}

			@Override
			public void log(Exception exception, String msg)
			{
			}

			@Override
			public void log(String message, Throwable throwable)
			{
			}

			@Override
			public String getRealPath(String path)
			{
				return("/temp/" + path);
			}

			@Override
			public String getServerInfo()
			{
				return("DummyServer");
			}

			@Override
			public String getInitParameter(String name)
			{
				return(null);
			}

			@Override
			public Enumeration getInitParameterNames()
			{
				return(emptyList.elements());
			}

			@Override
			public Object getAttribute(String name)
			{
				return(null);
			}

			@Override
			public Enumeration getAttributeNames()
			{
				return(emptyList.elements());
			}

			@Override
			public void setAttribute(String name, Object object)
			{
			}

			@Override
			public void removeAttribute(String name)
			{
			}

			@Override
			public String getServletContextName()
			{
				return("DummyContext");
			}

			@Override
			public int getEffectiveMajorVersion() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public int getEffectiveMinorVersion() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public boolean setInitParameter(String string, String string1) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public Dynamic addServlet(String string, String string1) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public Dynamic addServlet(String string, Servlet srvlt) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public Dynamic addServlet(String string, Class<? extends Servlet> type) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public <T extends Servlet> T createServlet(Class<T> type) throws ServletException {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public ServletRegistration getServletRegistration(String string) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public Map<String, ? extends ServletRegistration> getServletRegistrations() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public FilterRegistration.Dynamic addFilter(String string, String string1) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public FilterRegistration.Dynamic addFilter(String string, Filter filter) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public FilterRegistration.Dynamic addFilter(String string, Class<? extends Filter> type) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public <T extends Filter> T createFilter(Class<T> type) throws ServletException {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public FilterRegistration getFilterRegistration(String string) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public SessionCookieConfig getSessionCookieConfig() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public void setSessionTrackingModes(Set<SessionTrackingMode> set) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public void addListener(String string) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public <T extends EventListener> void addListener(T t) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public void addListener(Class<? extends EventListener> type) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public <T extends EventListener> T createListener(Class<T> type) throws ServletException {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public JspConfigDescriptor getJspConfigDescriptor() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public ClassLoader getClassLoader() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public void declareRoles(String... strings) {
				throw new UnsupportedOperationException("Not supported yet.");
			}

		});

		return(context);
	}
}
