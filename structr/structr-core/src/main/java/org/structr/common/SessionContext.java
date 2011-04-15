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
public class SessionContext
{
	private static final Map<Thread, SessionContext> contextMap = Collections.synchronizedMap(new WeakHashMap<Thread, SessionContext>());
	private static final Logger logger = Logger.getLogger(SessionContext.class.getName());
	public static final String CURRENT_NODE_PATH =			"current_node_path";

	// ----- private attributes -----
	private SessionValue<Boolean> redirectedValue = new SessionValue<Boolean>("redirected", false);		// default value is important here (autoboxing)!
	private final List<RequestCycleListener> requestCycleListener = new LinkedList<RequestCycleListener>();
	private Map<String, Object> attributes = new HashMap<String, Object>();					// no need to be synchronized
	private HttpServletResponse internalResponse = null;
	private HttpServletRequest internalRequest = null;
	private boolean redirectFlagJustSet = false;

	private SessionContext()
	{
	}

	// ----- static methods -----
	public static void redirect(User user, AbstractNode destination)
	{
		HttpServletResponse response = getResponse();
		HttpServletRequest request = getRequest();

		if(request != null && response != null)
		{
//			String redirectUrl = destination.getNodeURL(user, RenderMode.LOCAL, request.getContextPath());
			String redirectUrl = getAbsoluteNodePath(user, destination);

			try
			{
				SessionContext.setRedirected(true);
				response.sendRedirect(redirectUrl);

			} catch(IOException ioex)
			{
				logger.log(Level.WARNING, "Exception while trying to redirect to {0}: {1}", new Object[] { redirectUrl, ioex } );
			}
		}
	}

	public static void setRequest(HttpServletRequest request)
	{
		SessionContext context = getContext();
		context.setRequestInternal(request);
	}

	public static HttpServletRequest getRequest()
	{
		return(getContext().getRequestInternal());
	}

	public static void setResponse(HttpServletResponse response)
	{
		SessionContext context = getContext();
		context.setResponseInternal(response);
	}

	public static HttpServletResponse getResponse()
	{
		return(getContext().getResponseInternal());
	}

	public static HttpSession getSession()
	{
		HttpServletRequest request = getContext().getRequestInternal();
		HttpSession ret = null;

		if(request != null)
		{
			ret = request.getSession();
		}

		return(ret);
	}

	public static void setAttribute(String key, Object value)
	{
		getContext().setAttributeInternal(key, value);
	}

	public static Object getAttribute(String key)
	{
		return(getContext().getAttributeInternal(key));
	}

	public static String getAbsoluteNodePath(User user, AbstractNode node)
	{
		return(getRequest().getContextPath().concat("/view".concat(node.getNodePath(user).replace("&", "%26"))));
	}

	public static void setRedirected(boolean redirected)
	{
		getContext().setRedirectedInternal(redirected);
	}

	public static boolean isRedirected()
	{
		return(getContext().getRedirectedInternal());
	}

	public static void registerRequestCycleListener(RequestCycleListener callback)
	{
		getContext().registerRequestCycleListenerInternal(callback);
	}

	public static void onRequestStart()
	{
		getContext().callOnRequestStart();
	}

	public static void onRequestEnd()
	{
		getContext().callOnRequestEnd();
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

	private void setAttributeInternal(String key, Object value)
	{
		this.attributes.put(key, value);
	}

	private Object getAttributeInternal(String key)
	{
		return(this.attributes.get(key));
	}

	private void setRedirectedInternal(boolean redirected)
	{
		redirectFlagJustSet = true;

		this.redirectedValue.set(redirected);
	}

	private boolean getRedirectedInternal()
	{
		return(this.redirectedValue.get());
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
		if(!redirectFlagJustSet)
		{
			this.redirectedValue.set(false);

		} else
		{
			redirectFlagJustSet = false;
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
	private static SessionContext getContext()
	{
		Thread currentThread = Thread.currentThread();
		SessionContext ret = contextMap.get(currentThread);

		if(ret == null)
		{
			ret = new SessionContext();
			contextMap.put(currentThread, ret);
		}

		return(ret);
	}
}
