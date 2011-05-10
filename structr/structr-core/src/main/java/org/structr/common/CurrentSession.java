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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;

/**
 * A helper class that encapsulates access methods to the current session
 * in static methods.
 *
 * @author Christian Morgner
 */
public class CurrentSession
{
	private static final Logger logger = Logger.getLogger(CurrentSession.class.getName());

	private static final String SESSION_KEYS_KEY =			"sessionKeys";
	private static final String GLOBAL_USERNAME_KEY =		"globalUserName";
	private static final String JUST_REDIRECTED_KEY =		"justRedirected";
	private static final String REDIRECTED_KEY =			"redirected";

	public static boolean isRedirected()
	{
		HttpSession session = CurrentRequest.getSession();
		if(session != null)
		{
			Boolean ret = (Boolean)session.getAttribute(REDIRECTED_KEY);
			if(ret != null)
			{
				return(ret.booleanValue());
			}
		}

		return(false);
	}

	public static void setRedirected(boolean redirected)
	{
		HttpSession session = CurrentRequest.getSession();
		if(session != null)
		{
			session.setAttribute(JUST_REDIRECTED_KEY, redirected);
			session.setAttribute(REDIRECTED_KEY, redirected);
		}
	}

	public static boolean wasJustRedirected()
	{
		HttpSession session = CurrentRequest.getSession();
		if(session != null)
		{
			Boolean ret = (Boolean)session.getAttribute(JUST_REDIRECTED_KEY);
			if(ret != null)
			{
				return(ret.booleanValue());
			}
		}

		return(false);
	}

	public static void setJustRedirected(boolean justRedirected)
	{
		HttpSession session = CurrentRequest.getSession();
		if(session != null)
		{
			session.setAttribute(JUST_REDIRECTED_KEY, justRedirected);
		}
	}

	public static void setGlobalUsername(String username)
	{
		HttpSession session = CurrentRequest.getSession();
		if(session != null)
		{
			session.setAttribute(GLOBAL_USERNAME_KEY, username);
		}
	}

	public static String getGlobalUsername()
	{
		HttpSession session = CurrentRequest.getSession();
		if(session != null)
		{
			String ret = (String)session.getAttribute(GLOBAL_USERNAME_KEY);
			if(ret != null)
			{
				return(ret);
			}
		}

		return(null);
	}

	public static void setAttribute(String key, Object value)
	{
		HttpSession session = CurrentRequest.getSession();
		if(session != null)
		{
			session.setAttribute(key, value);
		}
	}

	public static Object getAttribute(String key)
	{
		HttpSession session = CurrentRequest.getSession();
		if(session != null)
		{
			return(session.getAttribute(key));
		}

		return(null);
	}

	public static HttpSession getSession()
	{
		return(CurrentRequest.getSession());
	}

	public static Set<String> getSessionKeys()
	{
		HttpSession session = CurrentRequest.getSession();
		Set<String> ret = null;

		if(session != null)
		{
			ret = (Set<String>)session.getAttribute(SESSION_KEYS_KEY);
			if(ret == null)
			{
				ret = new LinkedHashSet<String>();
				session.setAttribute(SESSION_KEYS_KEY, ret);
			}
		}

		return(ret);
	}
}
