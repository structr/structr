/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import javax.servlet.http.HttpSession;

/**
 *
 * @author Christian Morgner
 */
public class CurrentSession
{
	private static final String SESSION_CONTEXT_KEY =		"sessionContext";

	private boolean redirectFlagJustSet = false;
	private boolean redirected = false;

	private String globalUsername = null;

	private CurrentSession()
	{
	}

	public static CurrentSession getSessionContext()
	{
		HttpSession session = CurrentRequest.getSession();
		CurrentSession context = null;
		
		if(session != null)
		{
			context = (CurrentSession)session.getAttribute(SESSION_CONTEXT_KEY);
			if(context == null)
			{
				context = new CurrentSession();
				
				session.setAttribute(SESSION_CONTEXT_KEY, context);
			}
		}
		
		return(context);
	}

	public static boolean isRedirected()
	{
		CurrentSession session = CurrentSession.getSessionContext();
		if(session != null)
		{
			session.isRedirectedInternal();
		}

		return(false);
	}

	public static void setRedirected(boolean redirected)
	{
		CurrentSession session = CurrentSession.getSessionContext();
		if(session != null)
		{
			session.setRedirectedInternal(redirected);
		}
	}

	public static boolean wasJustRedirected()
	{
		CurrentSession session = CurrentSession.getSessionContext();
		if(session != null)
		{
			return(session.wasJustRedirectedInternal());
		}

		return(false);
	}

	public static void setJustRedirected(boolean justRedirected)
	{
		CurrentSession session = CurrentSession.getSessionContext();
		if(session != null)
		{
			session.setJustRedirectedInternal(justRedirected);
		}
	}

	public static void setGlobalUsername(String username)
	{
		CurrentSession session = CurrentSession.getSessionContext();
		if(session != null)
		{
			session.setGlobalUsernameInternal(username);
		}
	}

	public static String getGlobalUsername()
	{
		CurrentSession session = CurrentSession.getSessionContext();
		if(session != null)
		{
			return(session.getGlobalUsernameInternal());
		}

		return(null);
	}

	// ----- private methods -----
	private boolean isRedirectedInternal()
	{
		return redirected;
	}

	private void setRedirectedInternal(boolean redirected)
	{
		if(redirected)
		{
			this.redirectFlagJustSet = true;
		}

		this.redirected = redirected;
	}

	private boolean wasJustRedirectedInternal()
	{
		return(redirectFlagJustSet);
	}

	private void setJustRedirectedInternal(boolean justRedirected)
	{
		this.redirectFlagJustSet = justRedirected;
	}

	private void setGlobalUsernameInternal(final String username)
	{
		this.globalUsername = username;
	}

	private String getGlobalUsernameInternal()
	{
		return (this.globalUsername);
	}
}
