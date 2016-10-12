/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.dynamic.File;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.service.HttpService;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.schema.ConfigurationProvider;
import org.structr.util.Base64;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.Site;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

//~--- classes ----------------------------------------------------------------
/**
 * Main servlet for content rendering.
 *
 *
 *
 */
public class HtmlServlet extends HttpServlet implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(HtmlServlet.class.getName());

	public static final String CONFIRM_REGISTRATION_PAGE = "/confirm_registration";
	public static final String RESET_PASSWORD_PAGE       = "/reset-password";
	public static final String POSSIBLE_ENTRY_POINTS_KEY = "possibleEntryPoints";
	public static final String DOWNLOAD_AS_FILENAME_KEY  = "filename";
	public static final String RANGE_KEY                 = "range";
	public static final String DOWNLOAD_AS_DATA_URL_KEY  = "as-data-url";
	public static final String CONFIRM_KEY_KEY           = "key";
	public static final String TARGET_PAGE_KEY           = "target";
	public static final String ERROR_PAGE_KEY            = "onerror";

	public static final String CUSTOM_RESPONSE_HEADERS      = "HtmlServlet.customResponseHeaders";
	public static final String OBJECT_RESOLUTION_PROPERTIES = "HtmlServlet.resolveProperties";

	private static final String defaultCustomResponseHeaders = "Strict-Transport-Security:max-age=60,"
				+ "X-Content-Type-Options:nosniff,"
				+ "X-Frame-Options:SAMEORIGIN,"
				+ "X-XSS-Protection:1;mode=block";
	private static List<String> customResponseHeaders = Collections.EMPTY_LIST;

	private static final ThreadLocalMatcher threadLocalUUIDMatcher = new ThreadLocalMatcher("[a-fA-F0-9]{32}");
	private static final ExecutorService threadPool = Executors.newCachedThreadPool();

	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();
	private final Set<String> possiblePropertyNamesForEntityResolving   = new LinkedHashSet<>();

	private boolean isAsync = false;


	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	public HtmlServlet() {

		String customResponseHeadersString = Services.getBaseConfiguration().getProperty(CUSTOM_RESPONSE_HEADERS);

		if (StringUtils.isBlank(customResponseHeadersString)) {

			customResponseHeadersString = defaultCustomResponseHeaders;
		}

		if (StringUtils.isNotBlank(customResponseHeadersString)) {
			customResponseHeaders = Arrays.asList(customResponseHeadersString.split("[ ,]+"));
		}

		// resolving properties
		final String resolvePropertiesSource = StructrApp.getConfigurationValue(OBJECT_RESOLUTION_PROPERTIES, "AbstractNode.name");
		for (final String src : resolvePropertiesSource.split("[, ]+")) {

			final String name = src.trim();
			if (StringUtils.isNotBlank(name)) {

				possiblePropertyNamesForEntityResolving.add(name);
			}
		}

		this.isAsync = Services.parseBoolean(Services.getBaseConfiguration().getProperty(HttpService.ASYNC), true);
	}

	@Override
	public void destroy() {
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {

			final Authenticator auth        = getConfig().getAuthenticator();
			List<Page> pages                = null;
			boolean requestUriContainsUuids = false;

			SecurityContext securityContext;
			final App app;

			try {
				final String path = request.getPathInfo();

				// check for registration (has its own tx because of write access
				if (checkRegistration(auth, request, response, path)) {

					return;
				}

				// check for registration (has its own tx because of write access
				if (checkResetPassword(auth, request, response, path)) {

					return;
				}

				// isolate request authentication in a transaction
				try (final Tx tx = StructrApp.getInstance().tx()) {
					securityContext = auth.initializeAndExamineRequest(request, response);
					tx.success();
				}

			app = StructrApp.getInstance(securityContext);

			try (final Tx tx = app.tx()) {

				// Ensure access mode is frontend
				securityContext.setAccessMode(AccessMode.Frontend);

				request.setCharacterEncoding("UTF-8");

				// Important: Set character encoding before calling response.getWriter() !!, see Servlet Spec 5.4
				response.setCharacterEncoding("UTF-8");

				boolean dontCache = false;

				logger.debug("Path info {}", path);

				// don't continue on redirects
				if (response.getStatus() == 302) {

					tx.success();
					return;
				}

				final Principal user = securityContext.getUser(false);
				if (user != null) {

					// Don't cache if a user is logged in
					dontCache = true;

				}

				final RenderContext renderContext = RenderContext.getInstance(securityContext, request, response);

				renderContext.setResourceProvider(config.getResourceProvider());

				final EditMode edit = renderContext.getEditMode(user);

				DOMNode rootElement = null;
				AbstractNode dataNode = null;

				final String[] uriParts = PathHelper.getParts(path);
				if ((uriParts == null) || (uriParts.length == 0)) {

					// find a visible page
					rootElement = findIndexPage(securityContext, pages, edit);

					logger.debug("No path supplied, trying to find index page");

				} else {

					if (rootElement == null) {

						rootElement = findPage(securityContext, pages, path, edit);

					} else {
						dontCache = true;
					}
				}

				if (rootElement == null) { // No page found

					// In case of a file, try to find a file with the query string in the filename
					final String queryString = request.getQueryString();
					
					// Look for a file, first include the query string
					FileBase file = findFile(securityContext, request, path + (queryString != null ? "?" + queryString : ""));
					
					// If no file with query string in the file name found, try without query string
					if (file == null) {
						file = findFile(securityContext, request, path);
					}
					
					if (file != null) {

						streamFile(securityContext, file, request, response, edit);
						tx.success();
						return;

					}

					// store remaining path parts in request
					final Matcher matcher = threadLocalUUIDMatcher.get();

					for (int i = 0; i < uriParts.length; i++) {

						request.setAttribute(uriParts[i], i);
						matcher.reset(uriParts[i]);

						// set to "true" if part matches UUID pattern
						requestUriContainsUuids |= matcher.matches();

					}

					if (!requestUriContainsUuids) {

						// Try to find a data node by name
						dataNode = findFirstNodeByName(securityContext, request, path);

					} else {

						dataNode = findNodeByUuid(securityContext, PathHelper.getName(path));

					}

					//if (dataNode != null && !(dataNode instanceof Linkable)) {
					if (dataNode != null) {

						// Last path part matches a data node
						// Remove last path part and try again searching for a page
						// clear possible entry points
						request.removeAttribute(POSSIBLE_ENTRY_POINTS_KEY);

						rootElement = findPage(securityContext, pages, StringUtils.substringBeforeLast(path, PathHelper.PATH_SEP), edit);

						renderContext.setDetailsDataObject(dataNode);

						// Start rendering on data node
						if (rootElement == null && dataNode instanceof DOMNode) {

							rootElement = ((DOMNode) dataNode);
						}
					}
				}

				// look for pages with HTTP Basic Authentication (must be done as superuser)
				if (rootElement == null) {

					final HttpBasicAuthResult authResult = checkHttpBasicAuth(request, response, path);

					switch (authResult.authState()) {

						// Element with Basic Auth found and authentication succeeded
						case Authenticated:
							final Linkable result = authResult.getRootElement();
							if (result instanceof Page) {

								rootElement = (DOMNode)result;
								securityContext = authResult.getSecurityContext();
								renderContext.pushSecurityContext(securityContext);

							} else if (result instanceof FileBase) {

								streamFile(authResult.getSecurityContext(), (File)result, request, response, EditMode.NONE);
								tx.success();
								return;

							}
							break;

						// Page with Basic Auth found but not yet authenticated
						case MustAuthenticate:
							tx.success();
							return;

						// no Basic Auth for given path, go on
						case NoBasicAuth:
							break;
					}

				}

				// Still nothing found, do error handling
				if (rootElement == null) {
					rootElement = notFound(response, securityContext);
				}

				if (rootElement == null) {
					tx.success();
					return;
				}

				// check dont cache flag on page (if root element is a page)
				// but don't modify true to false
				dontCache |= rootElement.getProperty(Page.dontCache);

				if (EditMode.WIDGET.equals(edit) || dontCache) {

					setNoCacheHeaders(response);

				}

				if (!securityContext.isVisible(rootElement)) {

					rootElement = notFound(response, securityContext);
					if (rootElement == null) {

						tx.success();
						return;
					}

				} else {

					if (!EditMode.WIDGET.equals(edit) && !dontCache && notModifiedSince(request, response, rootElement, dontCache)) {

						ServletOutputStream out = response.getOutputStream();
						out.flush();
						//response.flushBuffer();
						out.close();

					} else {

						// prepare response
						response.setCharacterEncoding("UTF-8");

						String contentType = rootElement.getProperty(Page.contentType);

						if (contentType == null) {

							// Default
							contentType = "text/html;charset=UTF-8";
						}

						if (contentType.equals("text/html")) {
							contentType = contentType.concat(";charset=UTF-8");
						}

						response.setContentType(contentType);

						setCustomResponseHeaders(response);

						final boolean createsRawData = rootElement.getProperty(Page.pageCreatesRawData);

						// async or not?
						if (isAsync && !createsRawData) {

							final AsyncContext async = request.startAsync();
							final ServletOutputStream out = async.getResponse().getOutputStream();
							final AtomicBoolean finished = new AtomicBoolean(false);
							final DOMNode rootNode = rootElement;

							threadPool.submit(new Runnable() {

								@Override
								public void run() {

									try (final Tx tx = app.tx()) {

										//final long start = System.currentTimeMillis();
										// render
										rootNode.render(renderContext, 0);
										finished.set(true);

										//final long end = System.currentTimeMillis();
										//System.out.println("Done in " + (end-start) + " ms");
										tx.success();

									} catch (Throwable t) {
										logger.warn("", t);
										final String errorMsg = t.getMessage();
										try {
											//response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
											response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg);
											finished.set(true);
										} catch (IOException ex) {
											logger.warn("", ex);
										}
									}
								}

							});

							// start output write listener
							out.setWriteListener(new WriteListener() {

								@Override
								public void onWritePossible() throws IOException {

									try {

										final Queue<String> queue = renderContext.getBuffer().getQueue();
										while (out.isReady()) {

											String buffer = null;

											synchronized (queue) {
												buffer = queue.poll();
											}

											if (buffer != null) {

												out.print(buffer);

											} else {

												if (finished.get()) {

													async.complete();

													// don't overwrite 404 code from error page
													if (response.getStatus() != HttpServletResponse.SC_NOT_FOUND) {
														response.setStatus(HttpServletResponse.SC_OK);
													}

													// prevent this block from being called again
													break;
												}

												Thread.sleep(1);
											}
										}

									} catch (Throwable t) {
										logger.warn("", t);
									}
								}

								@Override
								public void onError(Throwable t) {
									logger.warn("", t);
								}
							});

						} else {

							final StringRenderBuffer buffer = new StringRenderBuffer();
							renderContext.setBuffer(buffer);

							// render
							rootElement.render(renderContext, 0);

							try {

								response.getOutputStream().write(buffer.getBuffer().toString().getBytes("utf-8"));
								response.getOutputStream().flush();
								response.getOutputStream().close();

							} catch (IOException ioex) {
								logger.warn("", ioex);
							}
						}
					}
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.error("Exception while processing request", fex);
			}

		} catch (IOException | FrameworkException t) {

			logger.error("Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		doGet(request, response);

	}

	@Override
	protected void doHead(final HttpServletRequest request, final HttpServletResponse response) {

		final Authenticator auth = getConfig().getAuthenticator();
		SecurityContext securityContext;
		List<Page> pages                = null;
		boolean requestUriContainsUuids = false;
		final App app;

		try {
			String path = request.getPathInfo();

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				securityContext = auth.initializeAndExamineRequest(request, response);
				tx.success();
			}

			app = StructrApp.getInstance(securityContext);

			try (final Tx tx = app.tx()) {

				// Ensure access mode is frontend
				securityContext.setAccessMode(AccessMode.Frontend);

				request.setCharacterEncoding("UTF-8");

				// Important: Set character encoding before calling response.getWriter() !!, see Servlet Spec 5.4
				response.setCharacterEncoding("UTF-8");
				response.setContentLength(0);

				boolean dontCache = false;

				logger.debug("Path info {}", path);

				// don't continue on redirects
				if (response.getStatus() == 302) {

					tx.success();
					return;
				}

				final Principal user = securityContext.getUser(false);
				if (user != null) {

					// Don't cache if a user is logged in
					dontCache = true;

				}

				final RenderContext renderContext = RenderContext.getInstance(securityContext, request, response);

				renderContext.setResourceProvider(config.getResourceProvider());

				final EditMode edit = renderContext.getEditMode(user);

				DOMNode rootElement = null;
				AbstractNode dataNode = null;

				String[] uriParts = PathHelper.getParts(path);
				if ((uriParts == null) || (uriParts.length == 0)) {

					// find a visible page
					rootElement = findIndexPage(securityContext, pages, edit);

					logger.debug("No path supplied, trying to find index page");

				} else {

					if (rootElement == null) {

						rootElement = findPage(securityContext, pages, path, edit);

					} else {
						dontCache = true;
					}
				}

				if (rootElement == null) { // No page found

					// Look for a file
					FileBase file = findFile(securityContext, request, path);
					if (file != null) {

						//streamFile(securityContext, file, request, response, edit);
						tx.success();
						return;

					}

					// store remaining path parts in request
					Matcher matcher = threadLocalUUIDMatcher.get();

					for (int i = 0; i < uriParts.length; i++) {

						request.setAttribute(uriParts[i], i);
						matcher.reset(uriParts[i]);

						// set to "true" if part matches UUID pattern
						requestUriContainsUuids |= matcher.matches();

					}

					if (!requestUriContainsUuids) {

						// Try to find a data node by name
						dataNode = findFirstNodeByName(securityContext, request, path);

					} else {

						dataNode = findNodeByUuid(securityContext, PathHelper.getName(path));

					}

					if (dataNode != null && !(dataNode instanceof Linkable)) {

						// Last path part matches a data node
						// Remove last path part and try again searching for a page
						// clear possible entry points
						request.removeAttribute(POSSIBLE_ENTRY_POINTS_KEY);

						rootElement = findPage(securityContext, pages, StringUtils.substringBeforeLast(path, PathHelper.PATH_SEP), edit);

						renderContext.setDetailsDataObject(dataNode);

						// Start rendering on data node
						if (rootElement == null && dataNode instanceof DOMNode) {

							rootElement = ((DOMNode) dataNode);

						}

					}

				}

				// look for pages with HTTP Basic Authentication (must be done as superuser)
				if (rootElement == null) {

					final HttpBasicAuthResult authResult = checkHttpBasicAuth(request, response, path);

					switch (authResult.authState()) {

						// Element with Basic Auth found and authentication succeeded
						case Authenticated:
							final Linkable result = authResult.getRootElement();
							if (result instanceof Page) {

								rootElement = (DOMNode)result;
								renderContext.pushSecurityContext(authResult.getSecurityContext());

							} else if (result instanceof FileBase) {

								//streamFile(authResult.getSecurityContext(), (File)result, request, response, EditMode.NONE);
								tx.success();
								return;

							}
							break;

						// Page with Basic Auth found but not yet authenticated
						case MustAuthenticate:
							return;

						// no Basic Auth for given path, go on
						case NoBasicAuth:
							break;
					}
				}

				// Still nothing found, do error handling
				if (rootElement == null) {

					// Check if security context has set an 401 status
					if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

						try {

							UiAuthenticator.writeUnauthorized(response);

						} catch (IllegalStateException ise) {
						}

					} else {

						rootElement = notFound(response, securityContext);

					}

				}

				if (rootElement == null) {

					// no content
					response.setContentLength(0);
					response.getOutputStream().close();

					tx.success();
					return;
				}

				// check dont cache flag on page (if root element is a page)
				// but don't modify true to false
				dontCache |= rootElement.getProperty(Page.dontCache);

				if (EditMode.WIDGET.equals(edit) || dontCache) {

					setNoCacheHeaders(response);

				}

				if (!securityContext.isVisible(rootElement)) {

					rootElement = notFound(response, securityContext);
					if (rootElement == null) {

						tx.success();
						return;
					}

				}

				if (securityContext.isVisible(rootElement)) {

					if (!EditMode.WIDGET.equals(edit) && !dontCache && notModifiedSince(request, response, rootElement, dontCache)) {

						response.getOutputStream().close();

					} else {

						// prepare response
						response.setCharacterEncoding("UTF-8");

						String contentType = rootElement.getProperty(Page.contentType);

						if (contentType == null) {

							// Default
							contentType = "text/html;charset=UTF-8";
						}

						if (contentType.equals("text/html")) {
							contentType = contentType.concat(";charset=UTF-8");
						}

						response.setContentType(contentType);

						setCustomResponseHeaders(response);

						response.getOutputStream().close();
					}

				} else {

					notFound(response, securityContext);

					response.getOutputStream().close();
				}

				tx.success();

			} catch (Throwable fex) {
				logger.error("Exception while processing request", fex);
			}

		} catch (FrameworkException t) {

			logger.error("Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}

	@Override
	protected void doOptions(final HttpServletRequest request, final HttpServletResponse response) {

		final Authenticator auth = config.getAuthenticator();

		try {

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				auth.initializeAndExamineRequest(request, response);
				tx.success();
			}

			response.setContentLength(0);
			response.setHeader("Allow", "GET,HEAD,OPTIONS");

		} catch (FrameworkException t) {

			logger.error("Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}

	/**
	 * Handle 404 Not Found
	 *
	 * First, search the first page which handles the 404.
	 *
	 * If none found, issue the container's 404 error.
	 *
	 * @param response
	 * @param securityContext
	 * @param renderContext
	 * @throws IOException
	 * @throws FrameworkException
	 */
	private Page notFound(final HttpServletResponse response, final SecurityContext securityContext) throws IOException, FrameworkException {

		final List<Page> errorPages = StructrApp.getInstance(securityContext).nodeQuery(Page.class).and(Page.showOnErrorCodes, "404", false).getAsList();

		for (final Page errorPage : errorPages) {
			
			if (isVisibleForSite(securityContext.getRequest(), errorPage)) {

				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return errorPage;
			}
			
		}
		
		response.sendError(HttpServletResponse.SC_NOT_FOUND);

		return null;
	}

	/**
	 * Find first node whose name matches the last part of the given path
	 *
	 * @param securityContext
	 * @param request
	 * @param path
	 * @return node
	 * @throws FrameworkException
	 */
	private AbstractNode findFirstNodeByName(final SecurityContext securityContext, final HttpServletRequest request, final String path) throws FrameworkException {

		final String name = PathHelper.getName(path);

		if (!name.isEmpty()) {

			logger.debug("Requested name: {}", name);

			final Query query                  = StructrApp.getInstance(securityContext).nodeQuery();
			final ConfigurationProvider config = StructrApp.getConfiguration();

			if (!possiblePropertyNamesForEntityResolving.isEmpty()) {

				query.and();
				resolvePossiblePropertyNamesForObjectResolution(config, query, name);
				query.parent();
			}


			final Result results = query.getResult();

			logger.debug("{} results", results.size());
			request.setAttribute(POSSIBLE_ENTRY_POINTS_KEY, results.getResults());

			return (results.size() > 0 ? (AbstractNode) results.get(0) : null);
		}

		return null;
	}

	/**
	 * Find node by uuid
	 *
	 * @param securityContext
	 * @param request
	 * @param uuid
	 * @return node
	 * @throws FrameworkException
	 */
	private AbstractNode findNodeByUuid(final SecurityContext securityContext, final String uuid) throws FrameworkException {

		if (!uuid.isEmpty()) {

			logger.debug("Requested id: {}", uuid);

			return (AbstractNode) StructrApp.getInstance(securityContext).getNodeById(uuid);
		}

		return null;
	}

	/**
	 * Find a file with its name matching last path part
	 *
	 * @param securityContext
	 * @param request
	 * @param path
	 * @return file
	 * @throws FrameworkException
	 */
	private FileBase findFile(final SecurityContext securityContext, final HttpServletRequest request, final String path) throws FrameworkException {

		List<Linkable> entryPoints = findPossibleEntryPoints(securityContext, request, path);

		// If no results were found, try to replace whitespace by '+' or '%20'
		if (entryPoints.isEmpty()) {
			entryPoints = findPossibleEntryPoints(securityContext, request, PathHelper.replaceWhitespaceByPlus(path));
		}

		if (entryPoints.isEmpty()) {
			entryPoints = findPossibleEntryPoints(securityContext, request, PathHelper.replaceWhitespaceByPercentTwenty(path));
		}

		for (Linkable node : entryPoints) {

			if (node instanceof FileBase && (path.equals(node.getPath()) || node.getUuid().equals(PathHelper.getName(path)))) {
				return (FileBase) node;
			}
		}

		return null;
	}

	/**
	 * Find a page with matching path.
	 *
	 * To be compatible with older versions, fallback to name-only lookup.
	 *
	 * @param securityContext
	 * @param pages
	 * @param path
	 * @param edit
	 * @return page
	 * @throws FrameworkException
	 */
	private Page findPage(final SecurityContext securityContext, List<Page> pages, final String path, final EditMode edit) throws FrameworkException {

		if (pages == null) {
			pages = StructrApp.getInstance(securityContext).nodeQuery(Page.class).getAsList();
			Collections.sort(pages, new GraphObjectComparator(Page.position, GraphObjectComparator.ASCENDING));
		}

		for (final Page page : pages) {

			final String pagePath = page.getPath();
			final String name     = PathHelper.getName(path);

			if (((pagePath != null && pagePath.equals(path)) || name.equals(page.getName()) || name.equals(page.getUuid()) ) && (EditMode.CONTENT.equals(edit) || isVisibleForSite(securityContext.getRequest(), page))) {
				return page;
			}
		}

		return null;
	}

	/**
	 * Find the page with the lowest non-empty position value which is visible in the
	 * current security context and for the given site.
	 *
	 * @param securityContext
	 * @param pages
	 * @param edit
	 * @return page
	 * @throws FrameworkException
	 */
	private Page findIndexPage(final SecurityContext securityContext, List<Page> pages, final EditMode edit) throws FrameworkException {

		if (pages == null) {
			pages = StructrApp.getInstance(securityContext).nodeQuery(Page.class).getAsList();
			Collections.sort(pages, new GraphObjectComparator(Page.position, GraphObjectComparator.ASCENDING));
		}

		for (Page page : pages) {

			if (securityContext.isVisible(page) && page.getProperty(Page.position) != null && ((EditMode.CONTENT.equals(edit) || isVisibleForSite(securityContext.getRequest(), page)) || (page.getProperty(Page.enableBasicAuth) && page.getProperty(Page.visibleToAuthenticatedUsers)))) {

				return page;
			}
		}

		return null;
	}

	/**
	 * This method checks if the current request is a user registration
	 * confirmation, usually triggered by a user clicking on a confirmation
	 * link in an e-mail.
	 *
	 * @param request
	 * @param response
	 * @param path
	 * @return true if the registration was successful
	 * @throws FrameworkException
	 * @throws IOException
	 */
	private boolean checkRegistration(final Authenticator auth, final HttpServletRequest request, final HttpServletResponse response, final String path) throws FrameworkException, IOException {

		logger.debug("Checking registration ...");

		String key = request.getParameter(CONFIRM_KEY_KEY);

		if (StringUtils.isEmpty(key)) {
			return false;
		}

		final String targetPage = request.getParameter(TARGET_PAGE_KEY);
		final String errorPage = request.getParameter(ERROR_PAGE_KEY);

		if (CONFIRM_REGISTRATION_PAGE.equals(path)) {

			final App app = StructrApp.getInstance();

			Result<Principal> results;
			try (final Tx tx = app.tx()) {

				results = app.nodeQuery(Principal.class).and(User.confirmationKey, key).getResult();

				tx.success();
			}

			if (!results.isEmpty()) {

				final Principal user = results.get(0);

				try (final Tx tx = app.tx()) {

					// Clear confirmation key and set session id
					user.setProperty(User.confirmationKey, null);

					if (auth.getUserAutoLogin()) {

						AuthHelper.doLogin(request, user);
					}

					tx.success();
				}

				// Redirect to target page
				if (StringUtils.isNotBlank(targetPage)) {
					response.sendRedirect("/" + targetPage);
				}

				return true;

			} else {
				// Redirect to error page
				if (StringUtils.isNotBlank(errorPage)) {
					response.sendRedirect("/" + errorPage);
				}

				return true;
			}
		}

		return false;
	}

	/**
	 * This method checks if the current request to reset a user password
	 *
	 * @param request
	 * @param response
	 * @param path
	 * @return true if the registration was successful
	 * @throws FrameworkException
	 * @throws IOException
	 */
	private boolean checkResetPassword(final Authenticator auth, final HttpServletRequest request, final HttpServletResponse response, final String path) throws FrameworkException, IOException {

		logger.debug("Checking registration ...");

		String key = request.getParameter(CONFIRM_KEY_KEY);

		if (StringUtils.isEmpty(key)) {
			return false;
		}

		final String targetPage = request.getParameter(TARGET_PAGE_KEY);

		if (RESET_PASSWORD_PAGE.equals(path)) {

			final App app = StructrApp.getInstance();

			Result<Principal> results;
			try (final Tx tx = app.tx()) {

				results = app.nodeQuery(Principal.class).and(User.confirmationKey, key).getResult();

				tx.success();
			}

			if (!results.isEmpty()) {

				final Principal user = results.get(0);

				try (final Tx tx = app.tx()) {

					// Clear confirmation key and set session id
					user.setProperty(User.confirmationKey, null);

					if (auth.getUserAutoLogin()) {

						AuthHelper.doLogin(request, user);
					}

					tx.success();
				}
			}

			// Redirect to target page
			if (StringUtils.isNotBlank(targetPage)) {
				response.sendRedirect(targetPage);
			}

			return true;
		}

		return false;
	}

	private List<Linkable> findPossibleEntryPointsByUuid(final SecurityContext securityContext, final HttpServletRequest request, final String uuid) throws FrameworkException {

		final List<Linkable> possibleEntryPoints = (List<Linkable>) request.getAttribute(POSSIBLE_ENTRY_POINTS_KEY);

		if (CollectionUtils.isNotEmpty(possibleEntryPoints)) {
			return possibleEntryPoints;
		}

		if (uuid.length() > 0) {

			logger.debug("Requested id: {}", uuid);

			final Query query = StructrApp.getInstance(securityContext).nodeQuery();

			query.and(GraphObject.id, uuid);
			query.and().orType(Page.class).orTypes(File.class);

			// Searching for pages needs super user context anyway
			Result results = query.getResult();

			logger.debug("{} results", results.size());
			request.setAttribute(POSSIBLE_ENTRY_POINTS_KEY, results.getResults());

			return (List<Linkable>) results.getResults();
		}

		return Collections.EMPTY_LIST;
	}

	private List<Linkable> findPossibleEntryPointsByPath(final SecurityContext securityContext, final HttpServletRequest request, final String path) throws FrameworkException {

		final List<Linkable> possibleEntryPoints = (List<Linkable>) request.getAttribute(POSSIBLE_ENTRY_POINTS_KEY);

		if (CollectionUtils.isNotEmpty(possibleEntryPoints)) {
			return possibleEntryPoints;
		}

		if (path.length() > 0) {

			logger.debug("Requested path: {}", path);

			final Query pageQuery = StructrApp.getInstance(securityContext).nodeQuery();

			pageQuery.and(Page.path, path).andType(Page.class);
			final Result pages = pageQuery.getResult();

			final Query fileQuery = StructrApp.getInstance(securityContext).nodeQuery();
			fileQuery.and(AbstractFile.path, path).andTypes(File.class);

			final Result files = fileQuery.getResult();
			
			logger.debug("Found {} pages and {} files/folders", new Object[] { pages.size(), files.size() });
			
			final List<Linkable> linkables = (List<Linkable>) pages.getResults();
			linkables.addAll(files.getResults());
			
			request.setAttribute(POSSIBLE_ENTRY_POINTS_KEY, linkables);

			return linkables;
		}

		return Collections.EMPTY_LIST;
	}

	private List<Linkable> findPossibleEntryPoints(final SecurityContext securityContext, final HttpServletRequest request, final String path) throws FrameworkException {

		List<Linkable> possibleEntryPoints = (List<Linkable>) request.getAttribute(POSSIBLE_ENTRY_POINTS_KEY);

		if (CollectionUtils.isNotEmpty(possibleEntryPoints)) {
			return possibleEntryPoints;
		}

		final int numberOfParts = PathHelper.getParts(path).length;

		if (numberOfParts > 0) {

			logger.debug("Requested name {}", path);

			possibleEntryPoints = findPossibleEntryPointsByPath(securityContext, request, path);

			if (possibleEntryPoints.isEmpty() && numberOfParts == 1) {
				possibleEntryPoints = findPossibleEntryPointsByUuid(securityContext, request, PathHelper.getName(path));
			}

			return possibleEntryPoints;
		}

		return Collections.EMPTY_LIST;
	}

	//~--- set methods ----------------------------------------------------
	public static void setNoCacheHeaders(final HttpServletResponse response) {

		response.setHeader("Cache-Control", "private, max-age=0, s-maxage=0, no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache, no-store"); // HTTP 1.0.
		response.setDateHeader("Expires", 0);

	}

	private static void setCustomResponseHeaders(final HttpServletResponse response) {

		for (final String header : customResponseHeaders) {

			final String[] keyValuePair = header.split("[ :]+");
			response.setHeader(keyValuePair[0], keyValuePair[1]);

			logger.debug("Set custom response header: {} {}", new Object[]{keyValuePair[0], keyValuePair[1]});

		}

	}

	private static boolean notModifiedSince(final HttpServletRequest request, HttpServletResponse response, final AbstractNode node, final boolean dontCache) {

		boolean notModified = false;
		final Date lastModified = node.getLastModifiedDate();

		// add some caching directives to header
		// see http://weblogs.java.net/blog/2007/08/08/expires-http-header-magic-number-yslow
		final DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		response.setHeader("Date", httpDateFormat.format(new Date()));

		final Calendar cal = new GregorianCalendar();
		final Integer seconds = node.getProperty(Page.cacheForSeconds);

		if (!dontCache && seconds != null) {

			cal.add(Calendar.SECOND, seconds);
			response.setHeader("Cache-Control", "max-age=" + seconds + ", s-maxage=" + seconds + "");
			response.setHeader("Expires", httpDateFormat.format(cal.getTime()));

		} else {

			if (!dontCache) {
				response.setHeader("Cache-Control", "no-cache, must-revalidate, proxy-revalidate");
			} else {
				response.setHeader("Cache-Control", "private, no-cache, no-store, max-age=0, s-maxage=0, must-revalidate, proxy-revalidate");
			}

		}

		if (lastModified != null) {

			final Date roundedLastModified = DateUtils.round(lastModified, Calendar.SECOND);
			response.setHeader("Last-Modified", httpDateFormat.format(roundedLastModified));

			final String ifModifiedSince = request.getHeader("If-Modified-Since");

			if (StringUtils.isNotBlank(ifModifiedSince)) {

				try {

					Date ifModSince = httpDateFormat.parse(ifModifiedSince);

					// Note that ifModSince has not ms resolution, so the last digits are always 000
					// That requires the lastModified to be rounded to seconds
					if ((ifModSince != null) && (roundedLastModified.equals(ifModSince) || roundedLastModified.before(ifModSince))) {

						notModified = true;

						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						response.setHeader("Vary", "Accept-Encoding");

					}

				} catch (ParseException ex) {
					logger.warn("Could not parse If-Modified-Since header", ex);
				}

			}

		}

		return notModified;
	}

	private void streamFile(SecurityContext securityContext, final FileBase file, HttpServletRequest request, HttpServletResponse response, final EditMode edit) throws IOException {

		if (!securityContext.isVisible(file)) {

			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;

		}

		final ServletOutputStream out         = response.getOutputStream();
		final String downloadAsFilename       = request.getParameter(DOWNLOAD_AS_FILENAME_KEY);
		final Map<String, Object> callbackMap = new LinkedHashMap<>();

		// make edit mode available in callback method
		callbackMap.put("editMode", edit);


		if (downloadAsFilename != null) {
			// Set Content-Disposition header to suggest a default filename and force a "save-as" dialog
			// See:
			// http://en.wikipedia.org/wiki/MIME#Content-Disposition,
			// http://tools.ietf.org/html/rfc2183
			// http://tools.ietf.org/html/rfc1806
			// http://tools.ietf.org/html/rfc2616#section-15.5 and http://tools.ietf.org/html/rfc2616#section-19.5.1
			response.addHeader("Content-Disposition", "attachment; filename=\"" + downloadAsFilename + "\"");

			callbackMap.put("requestedFileName", downloadAsFilename);
		}

		if (!EditMode.WIDGET.equals(edit) && notModifiedSince(request, response, file, false)) {

			out.flush();
			out.close();

			callbackMap.put("statusCode", HttpServletResponse.SC_NOT_MODIFIED);

		} else {

			final String downloadAsDataUrl = request.getParameter(DOWNLOAD_AS_DATA_URL_KEY);
			if (downloadAsDataUrl != null) {

				IOUtils.write(FileHelper.getBase64String(file), out);
				response.setContentType("text/plain");
				response.setStatus(HttpServletResponse.SC_OK);

				out.flush();
				out.close();

				callbackMap.put("statusCode", HttpServletResponse.SC_OK);

			} else {

				// 2b: stream file to response
				final InputStream in = file.getInputStream();
				final String contentType = file.getContentType();

				if (contentType != null) {

					response.setContentType(contentType);

				} else {

					// Default
					response.setContentType("application/octet-stream");
				}

				final String range = request.getHeader("Range");

				try {

					if (StringUtils.isNotEmpty(range)) {

						final long len = file.getSize();
						long start     = 0;
						long end       = len - 1;

						final Matcher matcher = Pattern.compile("bytes=(?<start>\\d*)-(?<end>\\d*)").matcher(range);

						if (matcher.matches()) {
							String startGroup = matcher.group("start");
							start = startGroup.isEmpty() ? start : Long.valueOf(startGroup);
							start = Math.max(0, start);

							String endGroup = matcher.group("end");
							end = endGroup.isEmpty() ? end : Long.valueOf(endGroup);
							end = end > len - 1 ? len - 1 : end;
						}

						long contentLength = end - start + 1;

						// Tell the client that we support byte ranges
						response.setHeader("Accept-Ranges", "bytes");
						response.setHeader("Content-Range", String.format("bytes %s-%s/%s", start, end, len));
						response.setHeader("Content-Length", String.format("%s", contentLength));

						response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
						callbackMap.put("statusCode", HttpServletResponse.SC_PARTIAL_CONTENT);

						IOUtils.copyLarge(in, out, start, contentLength);

					} else {

						response.setStatus(HttpServletResponse.SC_OK);
						callbackMap.put("statusCode", HttpServletResponse.SC_OK);

						IOUtils.copyLarge(in, out);

					}


				} catch (Throwable t) {

				} finally {

					if (out != null) {

						try {
							// 3: output content
							out.flush();
							out.close();

						} catch (Throwable t) {
						}
					}

					if (in != null) {
						in.close();
					}

					response.setStatus(HttpServletResponse.SC_OK);
				}
			}
		}


		// WIDGET mode means "opened in frontend", which we don't want to count as an external download
		if (!EditMode.WIDGET.equals(edit)) {

			// call onDownload callback
			try {

				file.invokeMethod("onDownload", Collections.EMPTY_MAP, false);

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}
		}
	}

	/**
	 * Check if the given page is visible for the requested site defined by
	 * a hostname and a port.
	 *
	 * @param request
	 * @param page
	 * @return
	 */
	private boolean isVisibleForSite(final HttpServletRequest request, final Page page) {

		logger.debug("Page: {} [{}], server name: {}, server port: {}", new Object[]{page.getName(), page.getUuid(), request.getServerName(), request.getServerPort()});

		final Site site = page.getProperty(Page.site);

		if (site == null) {
			logger.debug("Page {} [{}] has no site assigned.", new Object[]{page.getName(), page.getUuid()});
			return true;
		}

		logger.debug("Checking site: {} [{}], hostname: {}, port: {}", new Object[]{site.getName(), site.getUuid(), site.getProperty(Site.hostname), site.getProperty(Site.port)});

		final String serverName = request.getServerName();
		final int serverPort = request.getServerPort();

		if (StringUtils.isNotBlank(serverName) && !serverName.equals(site.getProperty(Site.hostname))) {
			logger.debug("Server name {} does not fit site hostname {}", new Object[]{serverName, site.getProperty(Site.hostname)});
			return false;
		}

		final Integer sitePort = site.getProperty(Site.port);

		if (sitePort != null && serverPort != sitePort) {
			logger.debug("Server port {} does not match site port {}", new Object[]{serverPort, sitePort});
			return false;
		}

		logger.debug("Matching site: {} [{}], hostname: {}, port: {}", new Object[]{site.getName(), site.getUuid(), site.getProperty(Site.hostname), site.getProperty(Site.port)});

		return true;

	}

	private void resolvePossiblePropertyNamesForObjectResolution(final ConfigurationProvider config, final Query query, final String name) {

		for (final String possiblePropertyName : possiblePropertyNamesForEntityResolving) {

			final String[] parts = possiblePropertyName.split("\\.");
			String className     = AbstractNode.class.getSimpleName();
			String keyName       = AbstractNode.name.jsonName();

			switch (parts.length) {

				case 2:
					className = parts[0];
					keyName = parts[1];
					break;

				default:
					logger.warn("Unable to process key for object resolution {}.", possiblePropertyName);
					break;
			}

			if (StringUtils.isNoneBlank(className, keyName)) {

				final Class type = config.getNodeEntityClass(className);
				if (type != null) {

					final PropertyKey key = config.getPropertyKeyForJSONName(type, keyName, false);
					if (key != null) {

						try {

							final PropertyConverter converter = key.inputConverter(SecurityContext.getSuperUserInstance());
							if (converter != null) {

								// try converted value, fail silenty
								query.or(key, converter.convert(name));

							} else {

								// try unconverted value, fail silently if it doesn't work
								query.or(key, name);
							}

						} catch (FrameworkException ignore) { }

					} else {

						logger.warn("Unable to find property key {} of type {} defined in key {} used for object resolution.", new Object[] { keyName, className, possiblePropertyName } );
					}

				} else {

					logger.warn("Unable to find type {} defined in key {} used for object resolution.", new Object[] { className, possiblePropertyName } );
				}
			}
		}
	}

	private HttpBasicAuthResult checkHttpBasicAuth(final HttpServletRequest request, final HttpServletResponse response, final String path) throws IOException, FrameworkException {

		// Look for renderable objects using a SuperUserSecurityContext,
		// but dont actually render the page. We're only interested in
		// the authentication settings.
		Linkable possiblePage = null;

		// try the different methods..
		if (possiblePage == null) {
			possiblePage = StructrApp.getInstance().nodeQuery(Page.class).and(Page.path, path).and(Page.enableBasicAuth, true).sort(Page.position).getFirst();
		}

		if (possiblePage == null) {
			possiblePage = StructrApp.getInstance().nodeQuery(Page.class).and(Page.name, PathHelper.getName(path)).and(Page.enableBasicAuth, true).sort(Page.position).getFirst();
		}

		if (possiblePage == null) {
			possiblePage = StructrApp.getInstance().nodeQuery(File.class).and(File.path, path).and(File.enableBasicAuth, true).getFirst();
		}

		if (possiblePage == null) {
			possiblePage = StructrApp.getInstance().nodeQuery(File.class).and(File.name, PathHelper.getName(path)).and(File.enableBasicAuth, true).getFirst();
		}

		if (possiblePage != null) {

			String realm = possiblePage.getProperty(Page.basicAuthRealm);
			if (realm == null) {

				realm = possiblePage.getName();
			}

			// check Http Basic Authentication headers
			final Principal principal = getPrincipalForAuthorizationHeader(request.getHeader("Authorization"));
			if (principal != null) {

				final SecurityContext securityContext = SecurityContext.getInstance(principal, AccessMode.Frontend);
				if (securityContext != null) {

					// find and instantiate the page again so that the SuperUserSecurityContext
					// can not leak into any of the children of the given page. This is dangerous..
					final Linkable page = StructrApp.getInstance(securityContext).get(Linkable.class, possiblePage.getUuid());
					if (page != null) {

						securityContext.setRequest(request);
						securityContext.setResponse(response);

						return new HttpBasicAuthResult(AuthState.Authenticated, securityContext, page);
					}
				}
			}

			// fallback: the following code will be executed if no Authorization
			// header was sent, OR if the authentication failed
			response.setHeader("WWW-Authenticate", "BASIC realm=\"" + realm + "\"");
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

			// no Authorization header sent by client
			return HttpBasicAuthResult.MUST_AUTHENTICATE;
		}

		// no Http Basic Auth enabled for any page
		return HttpBasicAuthResult.NO_BASIC_AUTH;
	}

	private Principal getPrincipalForAuthorizationHeader(final String authHeader) {

		if (authHeader != null) {

			final String[] authParts = authHeader.split(" ");
			if (authParts.length == 2) {

				final String authType  = authParts[0];
				final String authValue = authParts[1];
				String username        = null;
				String password        = null;

				if ("Basic".equals(authType)) {

					final String value   = new String(Base64.decode(authValue), Charset.forName("utf-8"));
					final String[] parts = value.split(":");

					if (parts.length == 2) {

						username = parts[0];
						password = parts[1];
					}
				}

				if (StringUtils.isNoneBlank(username, password)) {

					try {
						return AuthHelper.getPrincipalForPassword(Principal.name, username, password);

					} catch (Throwable t) {
						// ignore
					}
				}
			}
		}

		return null;
	}

	// ----- nested classes -----
	private enum AuthState {
		NoBasicAuth, MustAuthenticate, Authenticated
	}

	private static class HttpBasicAuthResult {

		// use singletons for the most common cases
		public static final HttpBasicAuthResult MUST_AUTHENTICATE = new HttpBasicAuthResult(AuthState.MustAuthenticate);
		public static final HttpBasicAuthResult NO_BASIC_AUTH     = new HttpBasicAuthResult(AuthState.NoBasicAuth);

		private SecurityContext securityContext = null;
		private Linkable rootElement            = null;
		private AuthState authState             = null;

		public HttpBasicAuthResult(final AuthState authState) {
			this(authState, null, null);
		}

		public HttpBasicAuthResult(final AuthState authState, final SecurityContext securityContext, final Linkable rootElement) {

			this.securityContext = securityContext;
			this.rootElement     = rootElement;
			this.authState       = authState;
		}

		public SecurityContext getSecurityContext() {
			return securityContext;
		}

		public AuthState authState() {
			return authState;
		}

		public Linkable getRootElement() {
			return rootElement;
		}
	}
}
