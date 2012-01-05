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
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
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

		});

		return(context);
	}
}
