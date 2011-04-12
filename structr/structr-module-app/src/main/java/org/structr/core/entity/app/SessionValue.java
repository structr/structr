/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.structr.common.StructrContext;

/**
 *
 * @author Christian Morgner
 */
public class SessionValue<T>
{
	protected String key = null;
	protected T defaultValue = null;

	/**
	 * Constructs a new property without a default value.
	 */
	public SessionValue(String key)
	{
		this.key = key;
	}

	/**
	 * Constructs a new property with default value <code>defaultValue</code>.
	 */
	public SessionValue(String key, T defaultValue)
	{
		this(key);

		this.defaultValue = defaultValue;
	}


	@Override
	public int hashCode()
	{
		if(get() != null)
		{
			return(get().hashCode());
		}

		return(key.hashCode());
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof SessionValue)
		{
			return(hashCode() == ((SessionValue)o).hashCode());
		}

		return(false);
	}
	public String getKey()
	{
		return(key);
	}

	public T get()
	{
		HttpServletRequest request = StructrContext.getRequest();

		if(request != null)
		{
			HttpSession session = request.getSession();

			if(session != null)
			{
				T ret = (T)session.getAttribute(key);

				if(ret != null)
				{
					return(ret);
				}
			}
		}

		return(defaultValue);
	}

	public void set(T value)
	{
		HttpServletRequest request = StructrContext.getRequest();

		if(request != null)
		{
			HttpSession session = request.getSession();

			if(session != null)
			{
				session.setAttribute(key, value);
			}
		}
	}

	public void setDefaultValue(T defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	public T getDefaultValue()
	{
		return(defaultValue);
	}
}
