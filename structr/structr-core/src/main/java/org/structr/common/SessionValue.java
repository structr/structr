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

import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.structr.common.CurrentRequest;

/**
 *
 * @author Christian Morgner
 */
public class SessionValue<T>
{
	protected T defaultValue = null;
	protected String key = null;

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
		HttpServletRequest request = CurrentRequest.getRequest();

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
		HttpServletRequest request = CurrentRequest.getRequest();

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
