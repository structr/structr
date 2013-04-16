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

import org.structr.core.property.PropertyKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;


import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;


import org.structr.common.*;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.*;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchOperator;
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
import org.structr.core.graph.GetNodeByIdCommand;
import org.structr.core.property.GenericProperty;
import org.structr.rest.ResourceProvider;
import org.structr.web.common.RenderContext;
import org.structr.web.common.ThreadLocalMatcher;
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
	public static final String LAST_GET_URL = "lastGetUrl";
	public static final String POSSIBLE_ENTRY_POINTS = "possibleEntryPoints";
	public static final String REQUEST_CONTAINS_UUID_IDENTIFIER = "request_contains_uuids";
	
	private ResourceProvider resourceProvider                   = null;

	private static final ThreadLocalMatcher threadLocalUUIDMatcher              = new ThreadLocalMatcher("[a-zA-Z0-9]{32}");
	
	public static SearchNodeCommand searchNodesAsSuperuser;
	//~--- fields ---------------------------------------------------------


	private DecimalFormat decimalFormat                                         = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private boolean edit;
	private Gson gson;

	public HtmlServlet() {}
	
	public HtmlServlet(final ResourceProvider resourceProvider) {

		this.resourceProvider    = resourceProvider;

	}

	//~--- methods --------------------------------------------------------
	
	@Override
	public void init() {
		
		 searchNodesAsSuperuser = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class);
	}

	private String postToRestUrl(HttpServletRequest request, final String pagePath, final Map<String, Object> parameters) {

		HttpClient httpClient = new HttpClient();
		ContentExchange contentExchange;
		String restUrl = null;

		gson = new GsonBuilder().create();

		try {

			httpClient.start();

			contentExchange = new ContentExchange();

			httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
			request.setCharacterEncoding("UTF-8");

			restUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getLocalPort() + Services.getRestPath() + "/" + pagePath;

			contentExchange.setURL(restUrl);

			Buffer buf = new ByteArrayBuffer(gson.toJson(parameters), "UTF-8");

			contentExchange.setRequestContent(buf);
			contentExchange.setRequestContentType("application/json;charset=UTF-8");
			contentExchange.setMethod("POST");

			String[] userAndPass = HttpAuthenticator.getUsernameAndPassword(request);

			if ((userAndPass != null) && (userAndPass.length == 2) && (userAndPass[0] != null) && (userAndPass[1] != null)) {

				contentExchange.addRequestHeader("X-User", userAndPass[0]);
				contentExchange.addRequestHeader("X-Password", userAndPass[1]);

			}

			httpClient.send(contentExchange);
			contentExchange.waitForDone();

			return contentExchange.getResponseContent();

		} catch (Exception ex) {

			logger.log(Level.WARNING, "Error while POSTing to REST url " + restUrl, ex);

			return null;
		}
	}

	@Override
	public void destroy() {}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		Map<String, String[]> parameterMap = request.getParameterMap();
		String path                        = PathHelper.clean(request.getPathInfo());

		// For now, we treat the first path item as the page path.
		// FIXME: Allow multi-segment page path (subpages)
		
		//String[] parts  = PathHelper.getParts(path);
		String restPath = StringUtils.substringAfter(path, PathHelper.PATH_SEP);

		String resp = postToRestUrl(request, restPath, convert(parameterMap));
		
		if (resp != null) {
			request.getSession().setAttribute(REST_RESPONSE, resp);
		}

		String redirect = null;

		try {
			// Check for a target URL coming from the form
			redirect = request.getParameter(REDIRECT);
			
			if (redirect == null) {
				redirect = (String) request.getSession().getAttribute(LAST_GET_URL);
				request.getSession().removeAttribute(LAST_GET_URL);
			}

			response.sendRedirect(redirect);

		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not redirect to " + redirect, ex);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {

		double start                    = System.nanoTime();

		try {
			
			request.setCharacterEncoding("UTF-8");

			// Important: Set character encoding before calling response.getWriter() !!, see Servlet Spec 5.4
			response.setCharacterEncoding("UTF-8");
			
			boolean dontCache = false;
			
			String resp = (String) request.getSession().getAttribute(REST_RESPONSE);
			if (resp != null) {
				
				request.setAttribute(REST_RESPONSE, resp);
				
				// empty response content after reading
				request.getSession().removeAttribute(REST_RESPONSE);
				
				// don't allow to show a cached page
				dontCache = true;
				
			}

			String path = PathHelper.clean(request.getPathInfo());

			logger.log(Level.FINE, "Path info {0}", path);
			
			SecurityContext securityContext = SecurityContext.getInstance(this.getServletConfig(), request, response, AccessMode.Frontend);
			RenderContext renderContext = RenderContext.getInstance(request, response, Locale.getDefault());
			
			renderContext.setResourceProvider(resourceProvider);
			
			edit = renderContext.getEdit();
			
			org.structr.web.entity.File file = findFile(request, path);
			DOMNode rootElement               = null;
			AbstractNode dataNode             = null;
			String searchFor                  = null;
			
			if (file == null) {
			
				String[] urlParts = PathHelper.getParts(path);
				

				if ((urlParts == null) || urlParts.length > 1) {

					searchFor = StringUtils.substringBefore(urlParts[1], "?");

				}

				if ((urlParts == null) || (urlParts.length == 0)) {

					// try to find a page with position==0
					rootElement = findIndexPage();

					logger.log(Level.INFO, "No path supplied, trying to find index page");

				} else {

					rootElement = findPage(request, path);
				}

				
				// store remaining path parts in request
				Matcher matcher                 = threadLocalUUIDMatcher.get();
				boolean requestUriContainsUuids = false;

				for (int i = 0; i < urlParts.length; i++) {

					request.setAttribute(urlParts[i], i);
					matcher.reset(urlParts[i]);

					// set to "true" if part matches UUID pattern
					requestUriContainsUuids |= matcher.matches();

				}

				
				if (!requestUriContainsUuids) {

					// Try to find a data ndoe by name
					dataNode = findFirstNodeByPath(request, path);

					if (dataNode != null) {

						request.setAttribute(dataNode.getUuid(), 0);

						// set to "true" if part matches UUID pattern
						requestUriContainsUuids = true;

					}

				} else {
					
					AbstractNode n = (AbstractNode) Services.command(securityContext, GetNodeByIdCommand.class).execute(PathHelper.getName(path));
					if (n != null) {
						dataNode = n;
					}
					
				}

				// store information about UUIDs in path in request for later use in Component
				request.setAttribute(REQUEST_CONTAINS_UUID_IDENTIFIER, requestUriContainsUuids);
			}
			
			
			if (rootElement == null && file == null) {
				
				if (dataNode != null) {
					
					// Last path part matches a data node
					// Remove last path part and try again searching for a page
					
					// clear possible entry points
					request.removeAttribute(POSSIBLE_ENTRY_POINTS);
					
					rootElement = findPage(request, PathHelper.clean(StringUtils.substringBeforeLast(path, PathHelper.PATH_SEP)));
					
					renderContext.setDetailsDataObject(dataNode);
					
				}
				
			}
			
			
			AbstractNode node = file != null ? file : rootElement != null ? rootElement : null;
			
			if (edit || dontCache) {

				response.setHeader("Pragma", "no-cache");

			} else {
				
				if (node != null) {
					lastModified = node.getLastModifiedDate();
				}

			}

			if ((rootElement != null) && securityContext.isVisible(rootElement)) {
				
				// Store last page GET URL in session
				request.getSession().setAttribute(LAST_GET_URL, request.getPathInfo());

				PrintWriter out            = response.getWriter();
				String uuid                = rootElement.getProperty(AbstractNode.uuid);
				
				
				
				List<NodeAttribute> attrs          = new LinkedList<NodeAttribute>();
				Map<String, String[]> parameterMap = request.getParameterMap();

				if ((parameterMap != null) && (parameterMap.size() > 0)) {

					attrs = convertToNodeAttributes(parameterMap);

				}
				
				double setup     = System.nanoTime();
				logger.log(Level.FINE, "Setup time: {0} seconds", decimalFormat.format((setup - start) / 1000000000.0));

				if (!edit && !dontCache && setCachingHeader(request, response, node)) {

					out.flush();
					out.close();

				} else {
					
					rootElement.render(securityContext, renderContext, 0);

					String content = renderContext.getBuffer().toString();
					double end     = System.nanoTime();
					logger.log(Level.FINE, "Content for path {0} in {1} seconds", new Object[] { path, decimalFormat.format((end - setup) / 1000000000.0)});

					// FIXME: where to get content type
					String contentType = rootElement.getProperty(Page.contentType);

					if (contentType != null) {

						if (contentType.equals("text/html")) {

							contentType = contentType.concat(";charset=UTF-8");

						}

						response.setContentType(contentType);

					} else {

						// Default
						response.setContentType("text/html;charset=UTF-8");
					}

					// 3: output content
					HttpAuthenticator.writeContent(content, response);

				}

			} else if ((file != null) && securityContext.isVisible(file)) {

				OutputStream out = response.getOutputStream();

				if (!edit && setCachingHeader(request, response, node)) {

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

			} else {

				// Check if security context has set an 401 status
				if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

					try {
						HttpAuthenticator.writeUnauthorized(response);
					} catch (IllegalStateException ise) { }

				} else {

					HttpAuthenticator.writeNotFound(response);

				}
			}

		} catch (Throwable t) {

			// t.printStackTrace();
			logger.log(Level.WARNING, "Exception while processing request", t);
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
	 * Convert parameter map so that after conversion, all map values
	 * are a single String instead of an one-element String[]
	 *
	 * @param parameterMap
	 * @return
	 */
	private Map<String, Object> convert(final Map<String, String[]> parameterMap) {

		Map parameters = new HashMap<String, Object>();

		for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {

			String[] values = param.getValue();
			Object val;

			if (values.length == 1) {

				val = values[0];

			} else {

				val = values;

			}

			parameters.put(param.getKey(), val);

		}

		return parameters;
	}

	/**
	 * Convert parameter map to list of node attributes
	 *
	 * @param parameterMap
	 * @return
	 */
	private List<NodeAttribute> convertToNodeAttributes(final Map<String, String[]> parameterMap) {

		List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

		for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {

			String[] values = param.getValue();
			Object val;

			if (values.length == 1) {

				val = values[0];

			} else {

				val = values;

			}

			PropertyKey key = new GenericProperty(param.getKey());
			NodeAttribute attr = new NodeAttribute(key, val);

			attrs.add(attr);

		}

		return attrs;
	}

	private Page findIndexPage() throws FrameworkException {

		logger.log(Level.FINE, "Looking for an index page ...");

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();
		searchAttrs.add(Search.orExactType(Page.class.getSimpleName()));
		
		Result results = (Result) searchNodesAsSuperuser.execute(searchAttrs);

		logger.log(Level.FINE, "{0} results", results.size());

		if (!results.isEmpty()) {

			Collections.sort(results.getResults(), new GraphObjectComparator(Page.position, AbstractNodeComparator.ASCENDING));

			return (Page) results.get(0);

		}

		return null;
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

			SearchAttributeGroup group = new SearchAttributeGroup(SearchOperator.AND);

			group.add(Search.orExactType(Page.class.getSimpleName()));
			group.add(Search.orExactTypeAndSubtypes(File.class.getSimpleName()));
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

			SearchAttributeGroup group = new SearchAttributeGroup(SearchOperator.AND);

			group.add(Search.orExactType(Page.class.getSimpleName()));
			group.add(Search.orExactTypeAndSubtypes(File.class.getSimpleName()));
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

			possibleEntryPoints = findPossibleEntryPointsByUuid(request, name);
		
			if (possibleEntryPoints.isEmpty()) {
				possibleEntryPoints = findPossibleEntryPointsByName(request, name);
			}
			
			return possibleEntryPoints;
		}

		return Collections.EMPTY_LIST;
	}

	//~--- set methods ----------------------------------------------------

	private static boolean setCachingHeader(final HttpServletRequest request, HttpServletResponse response, final AbstractNode node) {

		boolean notModified = false;

		// add some caching directives to header
		// see http://weblogs.java.net/blog/2007/08/08/expires-http-header-magic-number-yslow
		DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		Calendar cal              = new GregorianCalendar();
		Integer seconds           = node.getIntProperty(Page.cacheForSeconds);

		if (seconds != null) {

			cal.add(Calendar.SECOND, seconds);
			response.addHeader("Cache-Control", "public, max-age=" + seconds + ", s-maxage=" + seconds + ", must-revalidate, proxy-revalidate");
			httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			response.addHeader("Expires", httpDateFormat.format(cal.getTime()));

		} else {

			response.addHeader("Cache-Control", "public, must-revalidate, proxy-revalidate");

		}

		if (lastModified != null) {

			response.addHeader("Last-Modified", httpDateFormat.format(lastModified));

			String ifModifiedSince = request.getHeader("If-Modified-Since");

			if (StringUtils.isNotBlank(ifModifiedSince)) {

				try {

					Date ifModSince = httpDateFormat.parse(ifModifiedSince);
					
					// Note that ifModSince has not ms resolution, so the last digits are always 000
					// That requires the lastModified to be rounded to seconds
					
					Date rounded = DateUtils.round(lastModified, Calendar.SECOND);

					if ((ifModSince != null) && (rounded.equals(ifModSince) || rounded.before(ifModSince))) {

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
	
}
