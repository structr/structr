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

import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.util.LinkedHashSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//~--- classes ----------------------------------------------------------------

/**
 * Encapsulates structr context information private to a single request
 *
 * @author Christian Morgner
 */
public class RequestHelper {

	private static final Logger logger = Logger.getLogger(RequestHelper.class.getName());

	private static final String CURRENT_NODE_PATH_KEY = "currentNodePath";
	private static final String JUST_REDIRECTED_KEY = "justRedirected";
	private static final String REDIRECTED_KEY      = "redirected";
	private static final String SESSION_KEYS_KEY    = "sessionKeys";

	// ----- static methods -----
	public static void redirect(HttpServletRequest request, HttpServletResponse response, final AbstractNode currentNode, final AbstractNode destination) {

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

				redirectUrl = getAbsoluteNodePath(request, destination);
			}

			try {

				setRedirected(request, true);

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

	public static List<AbstractNode> retrieveSearchResult(SecurityContext securityContext, HttpServletRequest request) {

		if (request != null) {

			String searchString                = Search.clean(request.getParameter("search"));
			String searchInTitle               = request.getParameter("searchInTitle");
			boolean inTitle                    = (StringUtils.isNotEmpty(searchInTitle)
							      && Boolean.parseBoolean(searchInTitle))
				? true
				: false;
			String searchInContent = request.getParameter("searchInContent");
			boolean inContent      = (StringUtils.isNotEmpty(searchInContent)
						  && Boolean.parseBoolean(searchInContent))
						 ? true
						 : false;

			// if search string is given, put search results into freemarker model
			if ((searchString != null) &&!(searchString.isEmpty())) {

				List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

				searchAttrs.add(Search.orName(searchString));    // search in name

				if (inTitle) {
					searchAttrs.add(Search.orTitle(searchString));    // search in title
				}

				if (inContent) {
					searchAttrs.add(Search.orContent(searchString));    // search in name
				}

				Command search            = Services.command(SearchNodeCommand.class);
				List<AbstractNode> result = (List<AbstractNode>) search.execute(
					securityContext,
					null,     // top node => null means search all
					false,    // don't include deleted
					true,     // retrieve only public nodes
					searchAttrs);

				return result;
			}
		}

		return null;
	}

	public static String getAbsoluteNodePath(HttpServletRequest request, final AbstractNode node) {
		return (request.getContextPath().concat("/view".concat(node.getNodePath().replace("&", "%26"))));
	}

	public static String getCurrentNodePath(HttpServletRequest request) {
		return (String)request.getAttribute(CURRENT_NODE_PATH_KEY);
	}

	public static void setCurrentNodePath(HttpServletRequest request, String currentNodePath) {
		request.setAttribute(CURRENT_NODE_PATH_KEY, currentNodePath);
	}

	public static boolean wasJustRedirected(HttpServletRequest request) {

		HttpSession session = request.getSession();
		if (session != null) {

			Boolean ret = (Boolean) session.getAttribute(JUST_REDIRECTED_KEY);

			if (ret != null) {
				return (ret.booleanValue());
			}
		}

		return (false);
	}

	public static Set<String> getSessionKeys(HttpServletRequest request) {

		HttpSession session = request.getSession();
		Set<String> ret     = null;

		if (session != null) {

			ret = (Set<String>) session.getAttribute(SESSION_KEYS_KEY);

			if (ret == null) {

				ret = new LinkedHashSet<String>();
				session.setAttribute(SESSION_KEYS_KEY, ret);
			}
		}

		return (ret);
	}

	public static boolean isRedirected(HttpServletRequest request) {

		HttpSession session = request.getSession();
		if (session != null) {

			Boolean ret = (Boolean) session.getAttribute(REDIRECTED_KEY);

			if (ret != null) {
				return (ret.booleanValue());
			}
		}

		return (false);
	}

	public static void setRedirected(HttpServletRequest request, boolean redirected) {

		HttpSession session = request.getSession();
		if (session != null) {

			session.setAttribute(JUST_REDIRECTED_KEY, redirected);
			session.setAttribute(REDIRECTED_KEY, redirected);
		}
	}

	public static void setJustRedirected(HttpServletRequest request, boolean justRedirected) {

		HttpSession session = request.getSession();
		if (session != null) {
			session.setAttribute(JUST_REDIRECTED_KEY, justRedirected);
		}
	}
}
