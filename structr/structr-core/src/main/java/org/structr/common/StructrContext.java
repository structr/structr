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
 * Encapsulates structr context information private to a single request.
 *
 * @author Christian Morgner
 */
public class StructrContext
{
	private static final Logger logger = Logger.getLogger(StructrContext.class.getName());
	
	public static final String CURRENT_NODE_PATH =			"current_node_path";

	// ----- private attributes -----
	private static final Map<Thread, StructrContext> contextMap = Collections.synchronizedMap(new WeakHashMap<Thread, StructrContext>());
	private Map<String, Object> attributes = new HashMap<String, Object>();		// no need to be synchronized
	private final List<Callback> callbacks = new LinkedList<Callback>();
	private HttpServletResponse internalResponse = null;
	private HttpServletRequest internalRequest = null;

	private StructrContext()
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
				response.sendRedirect(redirectUrl);


			} catch(IOException ioex)
			{
				logger.log(Level.WARNING, "Exception while trying to redirect to {0}: {1}", new Object[] { redirectUrl, ioex } );
			}
		}
	}

	public static void setRequest(HttpServletRequest request)
	{
		StructrContext context = getContext();
		context.setRequestInternal(request);
	}

	public static HttpServletRequest getRequest()
	{
		return(getContext().getRequestInternal());
	}

	public static void setResponse(HttpServletResponse response)
	{
		StructrContext context = getContext();
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

	public static void registerCallback(Callback callback)
	{
		getContext().registerCallbackInternal(callback);
	}

	public static void callCallbacks()
	{
		getContext().callCallbacksInternal();
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

	private void registerCallbackInternal(Callback callback)
	{
		synchronized(callbacks)
		{
			this.callbacks.add(callback);
		}
	}

	private void callCallbacksInternal()
	{
		synchronized(callbacks)
		{
			for(Iterator<Callback> it = callbacks.iterator(); it.hasNext();)
			{
				Callback callback = it.next();

				try
				{
					callback.callback();

				} catch(Throwable t)
				{
					// do not let any exception prevent
					// the callback list from being cleared
				}

				// remove current element
				it.remove();
			}
		}
	}

	// ----- private static methods -----
	private static StructrContext getContext()
	{
		Thread currentThread = Thread.currentThread();
		StructrContext ret = contextMap.get(currentThread);

		if(ret == null)
		{
			ret = new StructrContext();
			contextMap.put(currentThread, ret);
		}

		return(ret);
	}
}
