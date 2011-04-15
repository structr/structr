/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.common;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Encapsulates structr context information private to a single session
 *
 * @author Christian Morgner
 */
public class CurrentRequest
{
	private static final Map<Thread, CurrentRequest> contextMap = Collections.synchronizedMap(new WeakHashMap<Thread, CurrentRequest>());
	private static final Logger logger = Logger.getLogger(CurrentRequest.class.getName());

	// ----- private attributes -----
	private final List<RequestCycleListener> requestCycleListener = new LinkedList<RequestCycleListener>();
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private HttpServletResponse internalResponse = null;
	private HttpServletRequest internalRequest = null;
	private String currentNodePath = null;
        
	private CurrentRequest()
	{
	}

	// ----- static methods -----
	public static void redirect(User user, AbstractNode destination)
	{
		HttpServletResponse response = getResponse();
		HttpServletRequest request = getRequest();

		if(request != null && response != null)
		{
			String redirectUrl = getAbsoluteNodePath(user, destination);

			try
			{
				CurrentSession.setRedirected(true);
				response.sendRedirect(redirectUrl);

			} catch(IOException ioex)
			{
				logger.log(Level.WARNING, "Exception while trying to redirect to {0}: {1}", new Object[] { redirectUrl, ioex } );
			}
		}
	}

	public static void setRequest(HttpServletRequest request)
	{
		CurrentRequest context = getRequestContext();
		context.setRequestInternal(request);
	}

	public static HttpServletRequest getRequest()
	{
		return(getRequestContext().getRequestInternal());
	}

	public static void setResponse(HttpServletResponse response)
	{
		CurrentRequest context = getRequestContext();
		context.setResponseInternal(response);
	}

	public static HttpServletResponse getResponse()
	{
		return(getRequestContext().getResponseInternal());
	}

	public static HttpSession getSession()
	{
		HttpServletRequest request = getRequestContext().getRequestInternal();
		HttpSession ret = null;

		if(request != null)
		{
			ret = request.getSession();
		}

		return(ret);
	}

	public static void setCurrentNodePath(String currentNodePath)
	{
		CurrentRequest request = getRequestContext();
		if(request != null)
		{
			request.setCurrentNodePathInternal(currentNodePath);
		}
	}

	public static String getCurrentNodePath()
	{
		CurrentRequest request = getRequestContext();
		if(request != null)
		{
			return(request.getCurrentNodePathInternal());
		}

		return(null);
	}

	public static String getAbsoluteNodePath(User user, AbstractNode node)
	{
		return(CurrentRequest.getRequest().getContextPath().concat("/view".concat(node.getNodePath(user).replace("&", "%26"))));
	}

	public static void registerRequestCycleListener(RequestCycleListener callback)
	{
		CurrentRequest request = getRequestContext();
		if(request != null)
		{
			request.registerRequestCycleListenerInternal(callback);
		}
	}

	public static void setAttribute(String key, Object value)
	{
		CurrentRequest request = getRequestContext();
		if(request != null)
		{
			request.setRequestAttributeInternal(key, value);
		}
	}

	public static Object getAttribute(String key)
	{
		CurrentRequest request = getRequestContext();
		if(request != null)
		{
			request.getRequestAttributeInternal(key);
		}

		return(null);
	}

	public static void onRequestStart()
	{
		CurrentRequest request = getRequestContext();
		if(request != null)
		{
			request.callOnRequestStart();
		}
	}

	public static void onRequestEnd()
	{
		CurrentRequest request = getRequestContext();
		if(request != null)
		{
			request.callOnRequestEnd();
		}
	}

	// ----- private methods -----
	private void setRequestInternal(HttpServletRequest request)
	{
		this.internalRequest = request;
	}

	private HttpServletRequest getRequestInternal()
	{
		return(this.internalRequest);
	}

	private void setResponseInternal(HttpServletResponse response)
	{
		this.internalResponse = response;
	}

	private HttpServletResponse getResponseInternal()
	{
		return(this.internalResponse);
	}

	private void setRequestAttributeInternal(String key, Object value)
	{
		attributes.put(key, value);
	}

	private void setCurrentNodePathInternal(String currentNodePath)
	{
		this.currentNodePath = currentNodePath;
	}

	private String getCurrentNodePathInternal()
	{
		return(currentNodePath);
	}

	private Object getRequestAttributeInternal(String key)
	{
		return(attributes.get(key));
	}

	private void registerRequestCycleListenerInternal(RequestCycleListener callback)
	{
		synchronized(requestCycleListener)
		{
			this.requestCycleListener.add(callback);
		}
	}

	private void callOnRequestStart()
	{
		if(CurrentSession.wasJustRedirected())
		{
			CurrentSession.setJustRedirected(false);

		} else
		{
			CurrentSession.setRedirected(false);
		}

		synchronized(requestCycleListener)
		{
			for(Iterator<RequestCycleListener> it = requestCycleListener.iterator(); it.hasNext();)
			{
				RequestCycleListener callback = it.next();

				try
				{
					callback.onRequestStart();

				} catch(Throwable t)
				{
					// do not let any exception prevent
					// the callback list from being iterated
				}

				// start of request, DO NOT remove current element
			}
		}
	}

	private void callOnRequestEnd()
	{
		synchronized(requestCycleListener)
		{
			for(Iterator<RequestCycleListener> it = requestCycleListener.iterator(); it.hasNext();)
			{
				RequestCycleListener callback = it.next();

				try
				{
					callback.onRequestEnd();

				} catch(Throwable t)
				{
					// do not let any exception prevent
					// the callback list from being cleared
				}

				// end of request: remove current element
				it.remove();
			}
		}
	}

	// ----- private static methods -----
	private static CurrentRequest getRequestContext()
	{
		Thread currentThread = Thread.currentThread();
		CurrentRequest ret = contextMap.get(currentThread);

		if(ret == null)
		{
			ret = new CurrentRequest();
			contextMap.put(currentThread, ret);
		}

		return(ret);
	}
}
