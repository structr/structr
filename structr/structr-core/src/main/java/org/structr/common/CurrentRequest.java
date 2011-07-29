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

import org.apache.commons.lang.StringUtils;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//~--- classes ----------------------------------------------------------------

/**
 * Encapsulates structr context information private to a single session
 *
 * @author Christian Morgner
 */
public class CurrentRequest {

	private static final Map<Thread, CurrentRequest> contextMap =
		Collections.synchronizedMap(new WeakHashMap<Thread, CurrentRequest>());
	private static final Logger logger = Logger.getLogger(CurrentRequest.class.getName());

	//~--- fields ---------------------------------------------------------

	private String currentNodePath               = null;
	private User currentUser                     = null;
	private HttpServletRequest internalRequest   = null;
	private HttpServletResponse internalResponse = null;

	// ----- private attributes -----
	private final List<RequestCycleListener> requestCycleListener = new LinkedList<RequestCycleListener>();
	private final Map<String, Object> attributes                  = new HashMap<String, Object>();

	//~--- constructors ---------------------------------------------------

	private CurrentRequest() {}

	//~--- methods --------------------------------------------------------

	// ----- static methods -----
	public static void redirect(final AbstractNode currentNode, final AbstractNode destination) {

		HttpServletResponse response = getResponse();
		HttpServletRequest request   = getRequest();

		if ((request != null) && (response != null)) {

			String redirectUrl = null;
			String referrer    = request.getHeader("referer");
			String requestURI  = request.getRequestURI();

			// TODO: Find a better solution for this.
			// Currently, we check the referrer if it contains the context path.
			// If yes redirect to the absolute node path which contains the context path
			if ((currentNode != null) && (!referrer.contains(request.getContextPath()))) {

				redirectUrl = destination.getNodePath(currentNode);

				// Substract request URI path
				String[] requestUriParts = StringUtils.split(requestURI, "/");

				for (int i = 0; i < requestUriParts.length + 1; i++) {
					redirectUrl = "../".concat(redirectUrl);
				}

			} else {
				redirectUrl = getAbsoluteNodePath(destination);
			}

			try {

				CurrentSession.setRedirected(true);

				// Use encodeURL here to enable UrlRewriteFilter rule
				// response.sendRedirect(response.encodeURL(redirectUrl));
				response.sendRedirect(redirectUrl);

			} catch (IOException ioex) {

				logger.log(Level.WARNING, "Exception while trying to redirect to {0}: {1}",
					   new Object[] { redirectUrl,
							  ioex });
			}
		}
	}

	public static void registerRequestCycleListener(final RequestCycleListener callback) {

		CurrentRequest request = getRequestContext();

		if (request != null) {
			request.registerRequestCycleListenerInternal(callback);
		}
	}

	public static void onRequestStart() {

		CurrentRequest request = getRequestContext();

		if (request != null) {
			request.callOnRequestStart();
		}
	}

	public static void onRequestEnd() {

		CurrentRequest request = getRequestContext();

		if (request != null) {
			request.callOnRequestEnd();
		}
	}

	public static void pushToNextRequest(final String key, final Object value) {

		Set<String> sessionKeys = CurrentSession.getSessionKeys();

		if (sessionKeys != null) {

			synchronized (sessionKeys) {
				sessionKeys.add(key);
			}
		}

		CurrentSession.setAttribute(key, value);
		CurrentSession.setAttribute("_".concat(key), value);
	}

	private void registerRequestCycleListenerInternal(final RequestCycleListener callback) {

		synchronized (requestCycleListener) {
			this.requestCycleListener.add(callback);
		}
	}

	private void callOnRequestStart() {

		if (CurrentSession.wasJustRedirected()) {
			CurrentSession.setJustRedirected(false);
		} else {
			CurrentSession.setRedirected(false);
		}

		// remove session key markers
		Set<String> sessionKeys = CurrentSession.getSessionKeys();

		if (sessionKeys != null) {

			synchronized (sessionKeys) {

				for (String sessionKey : sessionKeys) {

					String sessionKeyMarker = "_".concat(sessionKey);

					// check for session key marker
					if (CurrentSession.getAttribute(sessionKeyMarker) != null) {

						logger.log(Level.INFO, "Removing session key marker for {0}",
							   sessionKey);

						// remove session key marker if present
						CurrentSession.setAttribute(sessionKeyMarker, null);
					}
				}
			}
		}

		synchronized (requestCycleListener) {

			for (Iterator<RequestCycleListener> it = requestCycleListener.iterator(); it.hasNext(); ) {

				RequestCycleListener callback = it.next();

				try {
					callback.onRequestStart();
				} catch (Throwable t) {

					// do not let any exception prevent
					// the callback list from being iterated
				}

				// start of request, DO NOT remove current element
			}
		}
	}

	private void callOnRequestEnd() {

		synchronized (requestCycleListener) {

			for (Iterator<RequestCycleListener> it = requestCycleListener.iterator(); it.hasNext(); ) {

				RequestCycleListener callback = it.next();

				try {
					callback.onRequestEnd();
				} catch (Throwable t) {

					// do not let any exception prevent
					// the callback list from being cleared
				}

				// end of request: remove current element
				it.remove();
			}
		}

		// remove session keys
		Set<String> sessionKeys = CurrentSession.getSessionKeys();

		if (sessionKeys != null) {

			synchronized (sessionKeys) {

				for (Iterator<String> sessionKeyIterator = sessionKeys.iterator();
					sessionKeyIterator.hasNext(); ) {

					// check for session key marker
					String sessionKey = sessionKeyIterator.next();

					if (CurrentSession.getAttribute("_".concat(sessionKey)) == null) {

						logger.log(Level.INFO, "Removing session key marker for {0}",
							   sessionKey);

						// delete session key if marker is already gone
						CurrentSession.setAttribute(sessionKey, null);
						sessionKeyIterator.remove();
					}
				}
			}
		}
	}

	//~--- get methods ----------------------------------------------------

	public static HttpServletRequest getRequest() {
		return (getRequestContext().getRequestInternal());
	}

	public static HttpServletResponse getResponse() {
		return (getRequestContext().getResponseInternal());
	}

	public static HttpSession getSession() {

		HttpServletRequest request = getRequestContext().getRequestInternal();
		HttpSession session        = null;

		if (request != null) {

			try {
				session = request.getSession();
			} catch (Throwable t) {
				session = null;
			}
		}

		return session;
	}

//      public static User getCurrentUser() {
//
//              CurrentRequest request = getRequestContext();
//
//              if (request != null) {
//
//                      User currentUser = request.getCurrentUserInternal();
//
//                      logger.log(Level.FINE, "Current user: {0}", currentUser);
//
//                      return currentUser;
//              }
//
//              return (null);
//      }
	public static String getCurrentNodePath() {

		CurrentRequest request = getRequestContext();

		if (request != null) {
			return request.getCurrentNodePathInternal();
		}

		return null;
	}

	public static String getAbsoluteNodePath(final AbstractNode node) {

		return (CurrentRequest.getRequest().getContextPath().concat(
		    "/view".concat(node.getNodePath().replace("&", "%26"))));
	}

	public static Object getAttribute(final String key) {

		CurrentRequest request = getRequestContext();

		if (request != null) {
			request.getRequestAttributeInternal(key);
		}

		return (null);
	}

	private HttpServletRequest getRequestInternal() {
		return (this.internalRequest);
	}

	private HttpServletResponse getResponseInternal() {
		return (this.internalResponse);
	}

//      private User getCurrentUserInternal() {
//              return (currentUser);
//      }
	private String getCurrentNodePathInternal() {
		return (currentNodePath);
	}

	private Object getRequestAttributeInternal(final String key) {
		return (attributes.get(key));
	}

	// ----- private static methods -----
	private static CurrentRequest getRequestContext() {

		Thread currentThread = Thread.currentThread();

		logger.log(Level.FINE, "Current thread: {0}[{1}]", new Object[] { currentThread.getName(),
			currentThread.getId() });

		CurrentRequest request = contextMap.get(currentThread);

		if (request == null) {

			request = new CurrentRequest();
			contextMap.put(currentThread, request);
		}

		return (request);
	}

	//~--- set methods ----------------------------------------------------

	public static void setRequest(final HttpServletRequest request) {

		CurrentRequest context = getRequestContext();

		context.setRequestInternal(request);
	}

	public static void setResponse(final HttpServletResponse response) {

		CurrentRequest context = getRequestContext();

		context.setResponseInternal(response);
	}

//      public static void setCurrentUser(final User user) {
//
//              CurrentRequest request = getRequestContext();
//
//              if (request != null) {
//                      request.setCurrentUserInternal(user);
//              }
//      }
	public static void setCurrentNodePath(final String currentNodePath) {

		CurrentRequest request = getRequestContext();

		if (request != null) {
			request.setCurrentNodePathInternal(currentNodePath);
		}
	}

	public static void setAttribute(final String key, final Object value) {

		CurrentRequest request = getRequestContext();

		if (request != null) {
			request.setRequestAttributeInternal(key, value);
		}
	}

	// ----- private methods -----
	private void setRequestInternal(final HttpServletRequest request) {
		this.internalRequest = request;
	}

	private void setResponseInternal(final HttpServletResponse response) {
		this.internalResponse = response;
	}

	private void setRequestAttributeInternal(final String key, final Object value) {
		attributes.put(key, value);
	}

//      private void setCurrentUserInternal(final User currentUser) {
//              this.currentUser = currentUser;
//      }
	private void setCurrentNodePathInternal(final String currentNodePath) {
		this.currentNodePath = currentNodePath;
	}
}
