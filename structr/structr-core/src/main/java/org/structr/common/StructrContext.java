/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Encapsulates structr context information private to a single request.
 *
 * @author Christian Morgner
 */
public class StructrContext
{
	public static final String CURRENT_NODE_PATH =			"current_node_path";

	// ----- private attributes -----
	private static final Map<Thread, StructrContext> contextMap = Collections.synchronizedMap(new WeakHashMap<Thread, StructrContext>());
	private Map<String, Object> attributes = new HashMap<String, Object>();		// no need to be synchronized
	private HttpServletRequest internalRequest = null;

	private StructrContext()
	{
	}

	// ----- static methods -----
	public static void setRequest(HttpServletRequest request)
	{
		StructrContext context = getContext();
		context.setRequestInternal(request);
	}

	public static HttpServletRequest getRequest()
	{
		return(getContext().getRequestInternal());
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

	// ----- private methods -----
	private void setRequestInternal(HttpServletRequest request)
	{
		this.internalRequest = request;
	}

	private HttpServletRequest getRequestInternal()
	{
		return(this.internalRequest);
	}

	private void setAttributeInternal(String key, Object value)
	{
		this.attributes.put(key, value);
	}

	private Object getAttributeInternal(String key)
	{
		return(this.attributes.get(key));
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
