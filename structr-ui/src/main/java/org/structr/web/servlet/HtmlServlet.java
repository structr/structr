/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;


import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;



import org.structr.common.*;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.*;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.web.auth.HttpAuthenticator;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.File;

//~--- JDK imports ------------------------------------------------------------

import java.text.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.lucene.search.BooleanClause.Occur;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.AuthenticatorCommand;
import org.structr.core.entity.Principal;
import org.structr.core.graph.GetNodeByIdCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.rest.ResourceProvider;
import org.structr.web.common.RenderContext;
import org.structr.web.common.ThreadLocalMatcher;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;

//~--- classes ----------------------------------------------------------------

/**
 * Main servlet for content rendering.
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class HtmlServlet extends HttpServlet {

	private static final Logger logger                                          = Logger.getLogger(HtmlServlet.class.getName());
	private static Date lastModified;
	public static final String REST_RESPONSE = "restResponse";
	public static final String REDIRECT = "redirect";
	public static final String POSSIBLE_ENTRY_POINTS = "possibleEntryPoints";
	public static final String REQUEST_CONTAINS_UUID_IDENTIFIER = "request_contains_uuids";
	
	public static final String CONFIRM_REGISTRATION_PAGE = "confirm_registration";
	public static final String GET_SESSION_ID_PAGE = "get_session_id";
	public static final String CONFIRM_KEY_KEY = "key";
	public static final String TARGET_PAGE_KEY = "target";
	
	private ResourceProvider resourceProvider                   = null;

	private static final ThreadLocalMatcher threadLocalUUIDMatcher              = new ThreadLocalMatcher("[a-zA-Z0-9]{32}");
	
	public static SearchNodeCommand searchNodesAsSuperuser;
	//~--- fields ---------------------------------------------------------


	private DecimalFormat decimalFormat                                         = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private boolean edit;
//	private Gson gson;

	public HtmlServlet() {}
	
	public HtmlServlet(final ResourceProvider resourceProvider) {

		this.resourceProvider    = resourceProvider;

	}

	//~--- methods --------------------------------------------------------
	
	@Override
	public void init() {
		
		 searchNodesAsSuperuser = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class);
	}

	@Override
	public void destroy() {}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {

		double start                    = System.nanoTime();


		SecurityContext securityContext = null;

		try {

			securityContext = getAuthenticator().initializeAndExamineRequest(request, response);
			
			request.setCharacterEncoding("UTF-8");

			// Important: Set character encoding before calling response.getWriter() !!, see Servlet Spec 5.4
			response.setCharacterEncoding("UTF-8");

			boolean dontCache = false;
			
			String path = PathHelper.clean(request.getPathInfo());

			logger.log(Level.FINE, "Path info {0}", path);
			logger.log(Level.FINE, "Request examined by security context in {0} seconds", decimalFormat.format((System.nanoTime() - start) / 1000000000.0));
			
			// don't continue on redirects
			if (response.getStatus() == 302) {
				return;
			}
			
			if (securityContext.getUser(false) != null) {
				
				// Don't cache if a user is logged in
				dontCache = true;
				
			}
			
			RenderContext renderContext = RenderContext.getInstance(request, response, Locale.getDefault());
			
			renderContext.setResourceProvider(resourceProvider);
			
			edit = renderContext.getEdit();
			
			DOMNode rootElement               = null;
			AbstractNode dataNode             = null;
			
			
			String[] uriParts = PathHelper.getParts(path);
			if ((uriParts == null) || (uriParts.length == 0)) {

				// find a visible page with position==0
				rootElement = findIndexPage(securityContext);

				logger.log(Level.FINE, "No path supplied, trying to find index page");

			} else {
				
				// check if request was solely intended to obtain a session id
				if (checkGetSessionId(request, response, path)) {
					return;
				}

				// check for registration
				if (checkRegistration(securityContext, request, response, path)) {
					return;
				}
				
				if (rootElement == null) {
					
					rootElement = findPage(request, path);
					
				} else {
					dontCache = true;
				}
			}

			if (rootElement == null) { // No page found

				// Look for a file
				org.structr.web.entity.File file = findFile(request, path);
				if (file != null) {

					logger.log(Level.FINE, "File found in {0} seconds", decimalFormat.format((System.nanoTime() - start) / 1000000000.0));

					streamFile(securityContext, file, request, response);
					return;

				}

				// store remaining path parts in request
				Matcher matcher                 = threadLocalUUIDMatcher.get();
				boolean requestUriContainsUuids = false;

				for (int i = 0; i < uriParts.length; i++) {

					request.setAttribute(uriParts[i], i);
					matcher.reset(uriParts[i]);

					// set to "true" if part matches UUID pattern
					requestUriContainsUuids |= matcher.matches();

				}


				if (!requestUriContainsUuids) {

					// Try to find a data node by name
					dataNode = findFirstNodeByPath(request, path);

				} else {

					AbstractNode n = (AbstractNode) Services.command(securityContext, GetNodeByIdCommand.class).execute(PathHelper.getName(path));
					if (n != null) {
						dataNode = n;
					}

				}

				if (dataNode != null) {

					// Last path part matches a data node
					// Remove last path part and try again searching for a page

					// clear possible entry points
					request.removeAttribute(POSSIBLE_ENTRY_POINTS);

					rootElement = findPage(request, PathHelper.clean(StringUtils.substringBeforeLast(path, PathHelper.PATH_SEP)));

					renderContext.setDetailsDataObject(dataNode);

				}

			}

			// Still nothing found, do error handling
			if (rootElement == null) {
				
				// Check if security context has set an 401 status
				if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

					try {
						
						HttpAuthenticator.writeUnauthorized(response);
						
					} catch (IllegalStateException ise) {}

				} else {

					HttpAuthenticator.writeNotFound(response);

				}
				
				return;
				
			}
			
			logger.log(Level.FINE, "Page found in {0} seconds", decimalFormat.format((System.nanoTime() - start) / 1000000000.0));
			
			if (edit || dontCache) {

				setNoCacheHeaders(response);
				
			} else {
				
				lastModified = rootElement.getLastModifiedDate();

			}

			if (securityContext.isVisible(rootElement)) {
				
				PrintWriter out            = response.getWriter();
				
				double setup     = System.nanoTime();
				logger.log(Level.FINE, "Setup time: {0} seconds", decimalFormat.format((setup - start) / 1000000000.0));

				if (!edit && !dontCache && notModifiedSince(request, response, rootElement)) {

					out.flush();
					out.close();

				} else {
					
					rootElement.render(securityContext, renderContext, 0);

					String content = renderContext.getBuffer().toString();
					double end     = System.nanoTime();
					logger.log(Level.FINE, "Content for path {0} in {1} seconds", new Object[] { path, decimalFormat.format((end - setup) / 1000000000.0)});

					String contentType = rootElement.getProperty(Page.contentType);

					if (contentType != null && contentType.equals("text/html")) {

						contentType = contentType.concat(";charset=UTF-8");
						response.setContentType(contentType);

					} else {

						// Default
						response.setContentType("text/html;charset=UTF-8");
					}

					// 3: output content
					HttpAuthenticator.writeContent(content, response);

				}

			} else {

				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				
			}

		} catch (Throwable t) {

			t.printStackTrace();
			logger.log(Level.SEVERE, "Exception while processing request", t);
			HttpAuthenticator.writeInternalServerError(response);
		}
	}
	

	/**
	 * Find first node whose name matches the given path
	 * 
	 * @param request
	 * @param path
	 * @return
	 * @throws FrameworkException 
	 */
	private AbstractNode findFirstNodeByPath(HttpServletRequest request, final String path) throws FrameworkException {
		
		// FIXME: Take full path into account
		String name = PathHelper.getName(path);
		
		if (name.length() > 0) {

			logger.log(Level.FINE, "Requested name: {0}", name);

			Result results = searchNodesAsSuperuser.execute(Search.andExactName(name));
			
			logger.log(Level.FINE, "{0} results", results.size());
			request.setAttribute(POSSIBLE_ENTRY_POINTS, results.getResults());
			
			return (results.size() > 0 ? (AbstractNode) results.get(0) : null);
		}

		return null;
	}
	
	/**
	 * Find a file with its name matching last path part
	 * 
	 * @param request
	 * @param path
	 * @return
	 * @throws FrameworkException 
	 */
	private org.structr.web.entity.File findFile(HttpServletRequest request, final String path) throws FrameworkException {
	
		// FIXME: Take full page path into account
		List<AbstractNode> entryPoints = findPossibleEntryPoints(request, PathHelper.getName(path));
		
		// If no results were found, try to replace whitespace by '+' or '%20'
		
		if (entryPoints.isEmpty()) {
			entryPoints = findPossibleEntryPoints(request, PathHelper.getName(PathHelper.replaceWhitespaceByPlus(path)));
		}
		
		if (entryPoints.isEmpty()) {
			entryPoints = findPossibleEntryPoints(request, PathHelper.getName(PathHelper.replaceWhitespaceByPercentTwenty(path)));
		}
		
		for (AbstractNode node : entryPoints) {
			if (node instanceof org.structr.web.entity.File) {
				return (org.structr.web.entity.File) node;
			}
		}
		
		return null;
	}
	
	/**
	 * Find a page with its name matching last path part
	 * 
	 * @param request
	 * @param path
	 * @return
	 * @throws FrameworkException 
	 */
	private Page findPage(HttpServletRequest request, final String path) throws FrameworkException {
		
		List<AbstractNode> entryPoints = findPossibleEntryPoints(request, PathHelper.getName(path));
		
		for (AbstractNode node : entryPoints) {
			if (node instanceof Page) {
				return (Page) node;
			}
		}
		
		return null;
	}

	/**
	 * Find the page with the lowest position value which is visible in the
	 * current securit context
	 * 
	 * @param securityContext
	 * @return
	 * @throws FrameworkException 
	 */
	private Page findIndexPage(final SecurityContext securityContext) throws FrameworkException {

		logger.log(Level.FINE, "Looking for an index page ...");

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();
		searchAttrs.add(Search.orExactType(Page.class));
		
		Result results = (Result) searchNodesAsSuperuser.execute(searchAttrs);

		logger.log(Level.FINE, "{0} results", results.size());

		if (!results.isEmpty()) {

			Collections.sort(results.getResults(), new GraphObjectComparator(Page.position, GraphObjectComparator.ASCENDING));

			// Find first visible page
			
			int i=0;
			Page page = null;
			
			while (page == null || (i<results.size() && !securityContext.isVisible(page))) {
				
				page = (Page) results.get(i++);
				
			}
			
			return page;

		}

		return null;
	}

	/**
	 * Check if the request was solely intended to get a session id
	 * 
	 * @param request
	 * @param response
	 * @param path
	 * @return
	 * @throws FrameworkException
	 * @throws IOException 
	 */
	private boolean checkGetSessionId(final HttpServletRequest request, final HttpServletResponse response, final String path) throws IOException {
		
		logger.log(Level.FINE, "Checking for {0} ...", GET_SESSION_ID_PAGE);
		
		if (GET_SESSION_ID_PAGE.equals(path)) {
		
			request.getSession(true);
			response.setStatus(HttpServletResponse.SC_OK);
			
			response.flushBuffer();
			return true;
			
		}

		return false;
		
	}
	
	/**
	 * This method checks if the current request is a user registration confirmation,
	 * usually triggered by a user clicking on a confirmation link in an e-mail.
	 * 
	 * @param securityContext
	 * @param request
	 * @param response
	 * @param path
	 * @return true if the registration was successful
	 * @throws FrameworkException
	 * @throws IOException 
	 */
	private boolean checkRegistration(final SecurityContext securityContext, final HttpServletRequest request, final HttpServletResponse response, final String path) throws FrameworkException, IOException {

		logger.log(Level.FINE, "Checking registration ...");
		
		String key        = request.getParameter(CONFIRM_KEY_KEY);
		
		if (StringUtils.isEmpty(key)) {
			
			return false;
			
		}
		
		String targetPage = request.getParameter(TARGET_PAGE_KEY);

		if (CONFIRM_REGISTRATION_PAGE.equals(path)) {
		
			List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();
			searchAttrs.add(Search.andExactType(User.class));
			searchAttrs.add(Search.andExactProperty(securityContext, User.confirmationKey, key));

			Result results = (Result) searchNodesAsSuperuser.execute(searchAttrs);
			
			if (!results.isEmpty()) {
				
				final Principal user = (Principal) results.get(0);
				
				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {
						
						// Clear confirmation key and set session id
						user.setProperty(User.confirmationKey, null);
						user.setProperty(Principal.sessionId, request.getSession().getId());
						
						return null;
					}
					
				});
				
				// Redirect to target page
				if (StringUtils.isNotBlank(targetPage)) {
					
					response.sendRedirect("/" + targetPage);
					
					return true;
					
				}
				
			}
			
		}

		return false;
		
	}

	private List<AbstractNode> findPossibleEntryPointsByUuid(HttpServletRequest request, final String uuid) throws FrameworkException {

		List<AbstractNode> possibleEntryPoints = (List<AbstractNode>) request.getAttribute(POSSIBLE_ENTRY_POINTS);

		if (CollectionUtils.isNotEmpty(possibleEntryPoints)) {
			return possibleEntryPoints;
		}

		if (uuid.length() > 0) {

			logger.log(Level.FINE, "Requested id: {0}", uuid);

			List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

			searchAttrs.add(Search.andExactUuid(uuid));

			SearchAttributeGroup group = new SearchAttributeGroup(Occur.MUST);

			group.add(Search.orExactType(Page.class));
			group.add(Search.orExactTypeAndSubtypes(File.class));
//			group.add(Search.orExactTypeAndSubtypes(Image.class.getSimpleName())); // redundant
			searchAttrs.add(group);

			// Searching for pages needs super user context anyway
			Result results = searchNodesAsSuperuser.execute(searchAttrs);
			
			logger.log(Level.FINE, "{0} results", results.size());
			request.setAttribute(POSSIBLE_ENTRY_POINTS, results.getResults());
			
			return (List<AbstractNode>) results.getResults();
		}

		return Collections.EMPTY_LIST;
	}

	private List<AbstractNode> findPossibleEntryPointsByName(HttpServletRequest request, final String name) throws FrameworkException {

		List<AbstractNode> possibleEntryPoints = (List<AbstractNode>) request.getAttribute(POSSIBLE_ENTRY_POINTS);
		
		if (CollectionUtils.isNotEmpty(possibleEntryPoints)) {
			return possibleEntryPoints;
		}

		if (name.length() > 0) {

			logger.log(Level.FINE, "Requested name: {0}", name);

			List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

			searchAttrs.add(Search.andExactName(name));

			SearchAttributeGroup group = new SearchAttributeGroup(Occur.MUST);

			group.add(Search.orExactType(Page.class));
			group.add(Search.orExactTypeAndSubtypes(File.class));
//			group.add(Search.orExactTypeAndSubtypes(Image.class.getSimpleName())); // redundant
			searchAttrs.add(group);

			// Searching for pages needs super user context anyway
			Result results = searchNodesAsSuperuser.execute(searchAttrs);
			
			logger.log(Level.FINE, "{0} results", results.size());
			request.setAttribute(POSSIBLE_ENTRY_POINTS, results.getResults());
			
			return (List<AbstractNode>) results.getResults();
		}

		return Collections.EMPTY_LIST;
	}

	private List<AbstractNode> findPossibleEntryPoints(HttpServletRequest request, final String name) throws FrameworkException {

		List<AbstractNode> possibleEntryPoints = (List<AbstractNode>) request.getAttribute(POSSIBLE_ENTRY_POINTS);
		
		if (CollectionUtils.isNotEmpty(possibleEntryPoints)) {
			return possibleEntryPoints;
		}
		
		if (name.length() > 0) {

			logger.log(Level.FINE, "Requested name {0}", name);

			possibleEntryPoints = findPossibleEntryPointsByName(request, name);
		
			if (possibleEntryPoints.isEmpty()) {
				possibleEntryPoints = findPossibleEntryPointsByUuid(request, name);
			}
			
			return possibleEntryPoints;
		}

		return Collections.EMPTY_LIST;
	}

	//~--- set methods ----------------------------------------------------

	public static void setNoCacheHeaders(final HttpServletResponse response) {
		
		response.setHeader("Cache-Control", "private, max-age=0, no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache, no-store"); // HTTP 1.0.
		response.setDateHeader("Expires", 0);
		
	}
	
	private static boolean notModifiedSince(final HttpServletRequest request, HttpServletResponse response, final AbstractNode node) {

		boolean notModified = false;

		// add some caching directives to header
		// see http://weblogs.java.net/blog/2007/08/08/expires-http-header-magic-number-yslow
		DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		Calendar cal              = new GregorianCalendar();
		Integer seconds           = node.getProperty(Page.cacheForSeconds);

		if (seconds != null) {

			cal.add(Calendar.SECOND, seconds);
			response.addHeader("Cache-Control", "public, max-age=" + seconds + ", s-maxage=" + seconds + ", must-revalidate, proxy-revalidate");
			httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			response.addHeader("Expires", httpDateFormat.format(cal.getTime()));

		} else {

			response.addHeader("Cache-Control", "public, must-revalidate, proxy-revalidate");

		}

		if (lastModified != null) {

			Date roundedLastModified = DateUtils.round(lastModified, Calendar.SECOND);
			response.addHeader("Last-Modified", httpDateFormat.format(roundedLastModified));

			String ifModifiedSince = request.getHeader("If-Modified-Since");

			if (StringUtils.isNotBlank(ifModifiedSince)) {

				try {

					Date ifModSince = httpDateFormat.parse(ifModifiedSince);
					
					// Note that ifModSince has not ms resolution, so the last digits are always 000
					// That requires the lastModified to be rounded to seconds

					if ((ifModSince != null) && (roundedLastModified.equals(ifModSince) || roundedLastModified.before(ifModSince))) {

						notModified = true;

						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);

					}

				} catch (ParseException ex) {
					logger.log(Level.WARNING, "Could not parse If-Modified-Since header", ex);
				}

			}

		}

		return notModified;
	}

	public void setResourceProvider(final ResourceProvider resourceProvider) {
		this.resourceProvider = resourceProvider;
	}
	

	private void streamFile(SecurityContext securityContext, final org.structr.web.entity.File file, HttpServletRequest request, HttpServletResponse response) throws IOException {

		if (!securityContext.isVisible(file)) {
			
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
			
		}

		OutputStream out = response.getOutputStream();

		if (!edit && notModifiedSince(request, response, file)) {

			out.flush();
			out.close();

		} else {

			// 2b: stream file to response
			InputStream in     = file.getInputStream();
			String contentType = file.getContentType();

			if (contentType != null) {

				response.setContentType(contentType);

			} else {

				// Default
				response.setContentType("application/octet-stream");
			}

			try {

				IOUtils.copy(in, out);

			} catch (Throwable t) {

			} finally {

				if (out != null) {

					try {
						// 3: output content
						out.flush();
						out.close();

					} catch(Throwable t) {}
				}

				if (in != null) {
					in.close();
				}

				response.setStatus(HttpServletResponse.SC_OK);
			}
		}
	}

	private Authenticator getAuthenticator() throws FrameworkException {
		
		return (Authenticator) Services.command(null, AuthenticatorCommand.class).execute(getServletConfig());
		
	}
}
