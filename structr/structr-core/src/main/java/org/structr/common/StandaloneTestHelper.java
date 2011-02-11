/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

		context.put(Services.DATABASE_PATH_IDENTIFIER, databasePath);
		context.put(Services.ENTITY_PACKAGES_IDENTIFIER, "org.structr.core.entity");

		// add predicate
		context.put(Services.STRUCTR_PAGE_PREDICATE, new Predicate()
		{
			@Override
			public boolean evaluate(Object obj)
			{
				return(false);
			}
		});

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
