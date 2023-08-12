/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.servlet;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.QuietException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.AccessMode;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.rest.servlet.AbstractServletBase;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.util.Base64;
import org.structr.util.FileUtils;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.*;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.web.entity.dom.DOMElement;

/**
 * Main servlet for content rendering.
 */
public class HtmlServlet extends AbstractServletBase implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(HtmlServlet.class.getName());

	public static final String CONFIRM_REGISTRATION_PAGE = "/confirm_registration";
	public static final String RESET_PASSWORD_PAGE       = "/reset-password";
	public static final String POSSIBLE_ENTRY_POINTS_KEY = "possibleEntryPoints";
	public static final String DOWNLOAD_AS_FILENAME_KEY  = "filename";
	public static final String RANGE_KEY                 = "range";
	public static final String DOWNLOAD_AS_DATA_URL_KEY  = "as-data-url";
	public static final String CONFIRMATION_KEY_KEY      = "key";
	public static final String TARGET_PATH_KEY           = "target";
	public static final String ERROR_PAGE_KEY            = "onerror";

	public static final String ENCODED_RENDER_STATE_PARAMETER_NAME    = "structr-encoded-render-state";
	private static final ExecutorService threadPool                   = Executors.newCachedThreadPool();
	private final Pattern FilenameCleanerPattern                      = Pattern.compile("[\n\r]", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private final StructrHttpServiceConfig config                     = new StructrHttpServiceConfig();
	private final Set<String> possiblePropertyNamesForEntityResolving = new LinkedHashSet<>();

	private boolean isAsync = false;

	public HtmlServlet() {

		// resolving properties
		final String resolvePropertiesSource = Settings.HtmlResolveProperties.getValue();
		for (final String src : resolvePropertiesSource.split("[, ]+")) {

			final String name = src.trim();
			if (StringUtils.isNotBlank(name)) {

				possiblePropertyNamesForEntityResolving.add(name);
			}
		}

		this.isAsync = Settings.Async.getValue();
	}

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public String getModuleName() {
		return "ui";
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {

		final long t0                   = System.currentTimeMillis();
		final Authenticator auth        = getConfig().getAuthenticator();
		boolean requestUriContainsUuids = false;

		SecurityContext securityContext;
		final App app;

		try {

			assertInitialized();

			final String path = request.getPathInfo() != null ? request.getPathInfo() : "/";

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

			} catch (AuthenticationException aex) {

				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
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

				RuntimeEventLog.http(path, user);

				final RenderContext renderContext = RenderContext.getInstance(securityContext, request, response);

				renderContext.setResourceProvider(config.getResourceProvider());

				final EditMode edit = renderContext.getEditMode(user);

				DOMNode rootElement = null;
				AbstractNode dataNode = null;

				final String[] uriParts = PathHelper.getParts(path);

				if (uriParts == null) {
					logger.error("URI parts array is null, shouldn't happen.");
					throw new FrameworkException(500, "URI parts array is null, shouldn't happen.");
				}

				if (uriParts.length == 0) {

					logger.debug("No path supplied, trying to find index page");

					// find a visible page
					rootElement = findIndexPage(securityContext, edit);

				} else {

					// special optimization for UUID-addressed partials
					if (uriParts.length == 1 && Settings.isValidUuid(uriParts[0])) {

						final AbstractNode node = findNodeByUuid(securityContext, uriParts[0]);
						if (node != null && node instanceof DOMElement) {

							rootElement = (DOMElement) node;

							renderContext.setIsPartialRendering(true);
						}
					}

					if (rootElement == null) {

						rootElement = findPage(securityContext, path, edit);

						// special case where path is defined as "/custom/path" and request URI is "/custom/path/"
						if (rootElement == null && path.endsWith("/")) {
							rootElement = findPage(securityContext, path.substring(0, path.length() - 1), edit);
						}

					} else {

						dontCache = true;
					}
				}

				File file = null;

				if (rootElement == null) { // No page found

					// In case of a file, try to find a file with the query string in the filename
					final String queryString = request.getQueryString();

					// Look for a file, first include the query string
					if (StringUtils.isNotBlank(queryString)) {
						file = findFile(securityContext, request, path + "?" + queryString);
					}

					// If no file with query string in the file name found, try without query string
					if (file == null) {
						file = findFile(securityContext, request, path);
					}

					if (file == null) {


						for (int i = 0; i < uriParts.length; i++) {

							// store remaining path parts in request
							request.setAttribute(uriParts[i], i);

							// set to "true" if part matches UUID pattern
							requestUriContainsUuids |= Settings.isValidUuid(uriParts[i]);
						}

						if (uriParts.length == 1) {

							if (!requestUriContainsUuids) {

								// Try to find a page by path
								rootElement = findPage(securityContext, path, edit);

								if (rootElement == null) {

									// Try to find a partial by name
									rootElement = findPartialByName(securityContext, PathHelper.getName(path));
								}

							} else {

								final AbstractNode possibleRootNode = findNodeByUuid(securityContext, PathHelper.getName(path));

								if (possibleRootNode instanceof DOMNode) {
									rootElement = (DOMNode) possibleRootNode;
								}
							}

						} else if (uriParts.length >= 2) {

							final String pagePart = StringUtils.substringBeforeLast(path, PathHelper.PATH_SEP);

							rootElement = findPage(securityContext, pagePart, edit);

							if (rootElement == null) {

								final AbstractNode possibleRootNode = findNodeByUuid(securityContext, PathHelper.getName(pagePart));

								// check visibleForSite here as well
								if (possibleRootNode instanceof DOMNode && (!(possibleRootNode instanceof Page) || isVisibleForSite(request, (Page)possibleRootNode))) {

									rootElement = ((DOMNode) possibleRootNode);
								}

								if (rootElement == null) {

									rootElement = findPartialByName(securityContext, PathHelper.getName(pagePart));
								}

								dataNode = findNodeByUuid(securityContext, PathHelper.getName(path));

								if (dataNode == null) {
									dataNode = findFirstNodeByName(securityContext, request, path);
								}

								renderContext.setDetailsDataObject(dataNode);

							} else {

								if (requestUriContainsUuids) {

									dataNode = findNodeByUuid(securityContext, PathHelper.getName(path));

								} else {

									dataNode = findFirstNodeByName(securityContext, request, path);
								}

								renderContext.setDetailsDataObject(dataNode);
							}
						}

					}
				}

				// Disable Basic auth for any EditMode other than NONE
				if (Settings.HttpBasicAuthEnabled.getValue() && EditMode.NONE.equals(edit)) {

					final HttpBasicAuthResult authResult = checkHttpBasicAuth(securityContext, request, response, ((file != null) ? file.getPath() : (dataNode == null) ? path : StringUtils.substringBeforeLast(path, PathHelper.PATH_SEP)));

					switch (authResult.authState()) {

						// Element with Basic Auth found and authentication succeeded
						case Authenticated:

							final Linkable result = authResult.getRootElement();
							if (result instanceof Page) {

								rootElement = (DOMNode)result;
								securityContext = authResult.getSecurityContext();
								renderContext.pushSecurityContext(securityContext);

							} else if (result instanceof File) {

								streamFile(authResult.getSecurityContext(), (File)result, request, response, EditMode.NONE, true);
								tx.success();
								return;

							}
							break;

						// Page with Basic Auth found but not yet authenticated
						case MustAuthenticate:

							final Page errorPage = StructrApp.getInstance().nodeQuery(Page.class).and(StructrApp.key(Page.class, "showOnErrorCodes"), "401", false).getFirst();
							if (errorPage != null && isVisibleForSite(request, errorPage)) {

								// set error page
								rootElement = errorPage;

								// don't cache the error page
								dontCache = true;

							} else {

								// send error
								response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

								tx.success();
								return;
							}

							break;

						// no Basic Auth for given path, go on
						case NoBasicAuth:
							break;
					}
				}

				if (file != null) {

					streamFile(securityContext, file, request, response, edit, true);
					tx.success();
					return;
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
				dontCache |= rootElement.dontCache();

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

						// partial rendering?
						if (request.getParameter(ENCODED_RENDER_STATE_PARAMETER_NAME) != null) {

							final String encodedRenderState = request.getParameter(ENCODED_RENDER_STATE_PARAMETER_NAME);
							renderContext.initializeFromEncodedRenderState(encodedRenderState);
						}

						// prepare response
						response.setCharacterEncoding("UTF-8");

						String contentType = rootElement.getProperty(StructrApp.key(Page.class, "contentType"));

						if (contentType == null) {

							// Default
							contentType = "text/html;charset=UTF-8";
						}

						if (contentType.equals("text/html")) {
							contentType = contentType.concat(";charset=UTF-8");
						}

						response.setContentType(contentType);

						setCustomResponseHeaders(response);

						final boolean createsRawData = rootElement.getProperty(StructrApp.key(Page.class, "pageCreatesRawData"));

						// async or not?
						if (isAsync && !createsRawData) {

							renderAsyncOutput(request, response, app, renderContext, rootElement, t0);

						} else {

							final StringRenderBuffer buffer = new StringRenderBuffer();
							renderContext.setBuffer(buffer);

							// render
							rootElement.render(renderContext, 0);

							try {

								writeOutputStream(response, buffer);

							} catch (IOException ioex) {
								logger.warn("", ioex);
							}

							this.stats.recordStatsValue("html", rootElement.getName(), System.currentTimeMillis() - t0);
						}
					}
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.error("Exception while processing request: {}", fex.getMessage());
			}

		} catch (FrameworkException fex) {

			logger.error("Exception while processing request: {}", fex.getMessage());
			UiAuthenticator.writeFrameworkException(response, fex);

		} catch (EofException ex) {

			// ignore EofException which (by jettys standards) should be handled less verbosely

		} catch (IOException ioex) {

			logger.error("Exception while processing request: {}", ioex.getMessage());
			UiAuthenticator.writeInternalServerError(response);
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		doGet(request, response);
	}

	@Override
	protected void doHead(final HttpServletRequest request, final HttpServletResponse response) {

		final Authenticator auth        = getConfig().getAuthenticator();
		SecurityContext securityContext = null;
		boolean requestUriContainsUuids = false;

		try {

			assertInitialized();

			String path = request.getPathInfo();

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				securityContext = auth.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

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

				DOMNode rootElement   = null;
				AbstractNode dataNode = null;

				String[] uriParts = PathHelper.getParts(path);
				if ((uriParts == null) || (uriParts.length == 0)) {

					// find a visible page
					rootElement = findIndexPage(securityContext, edit);

					logger.debug("No path supplied, trying to find index page");

				} else {

					if (rootElement == null) {

						rootElement = findPage(securityContext, path, edit);

					} else {
						dontCache = true;
					}
				}

				if (rootElement == null) { // No page found

					// Look for a file
					File file = findFile(securityContext, request, path);
					if (file != null) {

						streamFile(securityContext, file, request, response, edit, false);
						tx.success();
						return;

					}

					if (uriParts != null) {

						// store remaining path parts in request
						for (int i = 0; i < uriParts.length; i++) {

							request.setAttribute(uriParts[i], i);

							// set to "true" if part matches UUID pattern
							requestUriContainsUuids |= Settings.isValidUuid(uriParts[i]);

						}
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

						rootElement = findPage(securityContext, StringUtils.substringBeforeLast(path, PathHelper.PATH_SEP), edit);

						renderContext.setDetailsDataObject(dataNode);

						// Start rendering on data node
						if (rootElement == null && dataNode instanceof DOMNode) {

							rootElement = ((DOMNode) dataNode);

						}

					}

				}

				// look for pages with HTTP Basic Authentication (must be done as superuser), only if HTTP Basic Auth is enabled
				if (rootElement == null && Settings.HttpBasicAuthEnabled.getValue()) {

					final HttpBasicAuthResult authResult = checkHttpBasicAuth(securityContext, request, response, path);

					switch (authResult.authState()) {

						// Element with Basic Auth found and authentication succeeded
						case Authenticated:
							final Linkable result = authResult.getRootElement();
							if (result instanceof Page) {

								rootElement = (DOMNode)result;
								renderContext.pushSecurityContext(authResult.getSecurityContext());

							} else if (result instanceof File) {

								//streamFile(authResult.getSecurityContext(), (File)result, request, response, EditMode.NONE);
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

					tx.success();
					return;
				}

				// check dont cache flag on page (if root element is a page)
				// but don't modify true to false
				dontCache |= rootElement.dontCache();

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

						String contentType = rootElement.getProperty(StructrApp.key(Page.class, "contentType"));

						if (contentType == null) {

							// Default
							contentType = "text/html;charset=UTF-8";
						}

						if (contentType.equals("text/html")) {
							contentType = contentType.concat(";charset=UTF-8");
						}

						response.setContentType(contentType);

						setCustomResponseHeaders(response);
					}

				} else {

					notFound(response, securityContext);
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

			assertInitialized();

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

	protected void renderAsyncOutput(HttpServletRequest request, HttpServletResponse response, App app, RenderContext renderContext, DOMNode rootElement, final long requestStartTime) throws IOException {

		final AsyncContext async      = request.startAsync();
		final ServletOutputStream out = async.getResponse().getOutputStream();
		final AtomicBoolean finished  = new AtomicBoolean(false);
		final DOMNode rootNode        = rootElement;

		threadPool.submit(new Runnable() {

			@Override
			public void run() {

				try (final Tx tx = app.tx()) {

					// render
					rootNode.render(renderContext, 0);
					finished.set(true);

					tx.success();

				} catch (Throwable t) {

					logger.warn("Error while rendering page {}: {}", rootNode.getName(), t.getMessage());
					logger.warn(ExceptionUtils.getStackTrace(t));

					try {

						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						finished.set(true);

					} catch (IOException ex) {
						logger.warn(ExceptionUtils.getStackTrace(ex));
					}
				}

				// record async rendering time
				HtmlServlet.super.stats.recordStatsValue("html", rootElement.getName(), System.currentTimeMillis() - requestStartTime);
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

								// prevent this block from being called again
								break;
							}

							Thread.sleep(1);
						}
					}

				} catch (EofException ee) {

					// ignore EofException which (by jettys standards) should be handled less verbosely

				} catch (IOException | InterruptedException t) {
					//logger.warn("Unexpected exception", t);
				}
			}

			@Override
			public void onError(Throwable t) {

				// prevent async from running into default timeout of 30s
				async.complete();

				if (t instanceof QuietException || t.getCause() instanceof QuietException) {
					// ignore exceptions which (by jettys standards) should be handled less verbosely
				} else {

					final SecurityContext sc = renderContext.getSecurityContext();
					final Principal user     = sc.getUser(false);
					final String username    = (user != null) ? user.getName() : "anonymous";

					logger.warn("Could not flush the response body content to the client, probably because the network connection was terminated.");
					logger.warn(" -> From: {} | URI: {} | Query: {} | User: {}", request.getRemoteAddr(), request.getRequestURI(), request.getQueryString(), username);
				}
			}
		});
	}

	protected void writeOutputStream(HttpServletResponse response, StringRenderBuffer buffer) throws IOException {

		response.getOutputStream().write(buffer.getBuffer().toString().getBytes("utf-8"));
		response.getOutputStream().flush();
		response.getOutputStream().close();
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
	 * @throws IOException
	 * @throws FrameworkException
	 */
	private Page notFound(final HttpServletResponse response, final SecurityContext securityContext) throws IOException, FrameworkException {

		final List<Page> errorPages = StructrApp.getInstance(securityContext).nodeQuery(Page.class).and(StructrApp.key(Page.class, "showOnErrorCodes"), "404", false).getAsList();

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


			final List<AbstractNode> results = query.getAsList();

			logger.debug("{} results", results.size());
			request.setAttribute(POSSIBLE_ENTRY_POINTS_KEY, results);

			return (results.size() > 0 ? (AbstractNode) results.get(0) : null);
		}

		return null;
	}

	/**
	 * Find node by uuid
	 *
	 * @param securityContext
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
	private File findFile(final SecurityContext securityContext, final HttpServletRequest request, final String path) throws FrameworkException {

		List<Linkable> entryPoints = findPossibleEntryPoints(securityContext, request, path);

		// If no results were found, try to replace whitespace by '+' or '%20'
		if (entryPoints.isEmpty()) {
			entryPoints = findPossibleEntryPoints(securityContext, request, PathHelper.replaceWhitespaceByPlus(path));
		}

		if (entryPoints.isEmpty()) {
			entryPoints = findPossibleEntryPoints(securityContext, request, PathHelper.replaceWhitespaceByPercentTwenty(path));
		}

		for (Linkable node : entryPoints) {

			if (node instanceof File && (path.equals(node.getPath()) || node.getUuid().equals(PathHelper.getName(path)))) {
				return (File) node;
			}
		}

		return null;
	}

	/**
	 * Find a partial by name (meaning a DOMNode which can not be a page - otherwise a check for Sites has to be applied)
	 *
	 * @param securityContext
	 * @param name
	 * @return
	 * @throws FrameworkException
	 */
	private DOMNode findPartialByName(final SecurityContext securityContext, final String name) throws FrameworkException {

		for (final DOMNode potentialPartial : StructrApp.getInstance(securityContext).nodeQuery(DOMNode.class).andName(name).not().and(DOMNode.type, "Page").getAsList()) {

			if (potentialPartial.getOwnerDocumentAsSuperUser() != null) {
				return potentialPartial;
			}
		}

		return null;
	}

	/**
	 * Find a page by matching path.
	 *
	 * To be compatible with older versions, fallback to name-only lookup.
	 *
	 * @param securityContext
	 * @param path
	 * @param edit
	 * @return page
	 * @throws FrameworkException
	 */
	private Page findPage(final SecurityContext securityContext, final String path, final EditMode edit) throws FrameworkException {

		final PropertyKey<String> pathKey = StructrApp.key(Page.class, "path");
		final PropertyKey<String> nameKey = StructrApp.key(Page.class, "name");

		final PropertyMap attributes = new PropertyMap(pathKey, path);
		final String name = PathHelper.getName(path);
		attributes.put(nameKey, name);

		// Find pages by path or name
		final List<Page> possiblePages = StructrApp.getInstance(securityContext).nodeQuery(Page.class)
			.or()
				.notBlank(pathKey)
				.and(pathKey, path)
				.parent()
			.or()
				.blank(pathKey)
				.and(nameKey, name)
				.parent()
			.sort(pathKey)
			.getAsList();

		for (final Page page : possiblePages) {

			if (EditMode.CONTENT.equals(edit) || isVisibleForSite(securityContext.getRequest(), page)) {

				return page;
			}
		}

		// Check direct access by UUID
		if (name.length() == 32) {

			final NodeInterface possiblePage = StructrApp.getInstance(securityContext).get(NodeInterface.class, name);

			if (possiblePage != null && possiblePage instanceof Page && (EditMode.CONTENT.equals(edit) || isVisibleForSite(securityContext.getRequest(), (Page) possiblePage))) {

				return (Page) possiblePage;
			}
		}

		return null;
	}

	/**
	 * Find the page with the lowest non-empty position value which is visible in the
	 * current security context and for the given site.
	 *
	 * @param securityContext
	 * @param edit
	 * @return page
	 * @throws FrameworkException
	 */
	private Page findIndexPage(final SecurityContext securityContext, final EditMode edit) throws FrameworkException {

		final PropertyKey<Integer> positionKey = StructrApp.key(Page.class, "position");

		List<Page> possiblePages = StructrApp.getInstance(securityContext).nodeQuery(Page.class).notBlank(positionKey).sort(positionKey).getAsList();

		for (Page page : possiblePages) {

			if (securityContext.isVisible(page) && ((EditMode.CONTENT.equals(edit) || isVisibleForSite(securityContext.getRequest(), page)) || (page.getEnableBasicAuth() && page.isVisibleToAuthenticatedUsers()))) {

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
	 * @param auth
	 * @param request
	 * @param response
	 * @param path
	 * @return true if the registration was successful
	 * @throws FrameworkException
	 * @throws IOException
	 */
	private boolean checkRegistration(final Authenticator auth, final HttpServletRequest request, final HttpServletResponse response, final String path) throws FrameworkException, IOException {

		logger.debug("Checking registration ...");

		final String key = request.getParameter(CONFIRMATION_KEY_KEY);

		if (StringUtils.isEmpty(key)) {
			return false;
		}

		final PropertyKey<String> confirmationKeyKey = StructrApp.key(User.class, "confirmationKey");

		if (CONFIRM_REGISTRATION_PAGE.equals(path)) {

			final App app = StructrApp.getInstance();

			List<Principal> results;
			try (final Tx tx = app.tx()) {

				results = app.nodeQuery(Principal.class).and(confirmationKeyKey, key).getAsList();

				tx.success();
			}

			if (!results.isEmpty()) {

				final Principal user = results.get(0);

				try (final Tx tx = app.tx()) {

					// Clear confirmation key and set session id
					user.setProperty(confirmationKeyKey, null);

					if (AuthHelper.isConfirmationKeyValid(key, Settings.ConfirmationKeyRegistrationValidityPeriod.getValue())) {

						if (Settings.RestUserAutologin.getValue()) {

							AuthHelper.doLogin(request, user);

						} else {

							logger.warn("Refusing login because {} is disabled", Settings.RestUserAutologin.getKey());
						}

					} else {

						logger.warn("Confirmation key for user {} is not valid anymore - refusing login.", user.getName());
					}

					tx.success();
				}

				// Redirect to target path
				final String targetPath = filterMaliciousRedirects(request.getParameter(TARGET_PATH_KEY));

				if (StringUtils.isNotBlank(targetPath)) {
					sendRedirectHeader(response, targetPath, false);	// user-provided, should be already prefixed
				}

				return true;

			} else {

				// Redirect to error path
				final String errorPath = filterMaliciousRedirects(request.getParameter(ERROR_PAGE_KEY));

				if (StringUtils.isNotBlank(errorPath)) {
					sendRedirectHeader(response, errorPath, false);	// user-provided, should be already prefixed
				}

				return true;
			}
		}

		return false;
	}

	/**
	 * This method checks if the current request to reset a user password
	 *
	 * @param auth
	 * @param request
	 * @param response
	 * @param path
	 * @return true if the registration was successful
	 * @throws FrameworkException
	 * @throws IOException
	 */
	private boolean checkResetPassword(final Authenticator auth, final HttpServletRequest request, final HttpServletResponse response, final String path) throws FrameworkException, IOException {

		logger.debug("Checking reset password ...");

		final String key = request.getParameter(CONFIRMATION_KEY_KEY);

		if (StringUtils.isEmpty(key)) {
			return false;
		}

		final PropertyKey<String> confirmationKeyKey = StructrApp.key(User.class, "confirmationKey");

		if (RESET_PASSWORD_PAGE.equals(path)) {

			final App app = StructrApp.getInstance();

			List<Principal> results;
			try (final Tx tx = app.tx()) {

				results = app.nodeQuery(Principal.class).and(confirmationKeyKey, key).getAsList();

				tx.success();
			}

			if (!results.isEmpty()) {

				final Principal user = results.get(0);

				try (final Tx tx = app.tx()) {

					// Clear confirmation key and set session id
					user.setProperty(confirmationKeyKey, null);

					if (AuthHelper.isConfirmationKeyValid(key, Settings.ConfirmationKeyPasswordResetValidityPeriod.getValue())) {

						if (Settings.RestUserAutologin.getValue()) {

							if (Settings.PasswordResetFailedCounterOnPWReset.getValue()) {

								AuthHelper.resetFailedLoginAttemptsCounter(user);
							}

							AuthHelper.doLogin(request, user);

						} else {

							logger.warn("Refusing login because {} is disabled", Settings.RestUserAutologin.getKey());
						}

					} else {

						logger.warn("Confirmation key for user {} is not valid anymore - refusing login.", user.getName());
					}

					tx.success();
				}
			}

			// Redirect to target path
			final String targetPath = filterMaliciousRedirects(request.getParameter(TARGET_PATH_KEY));

			if (StringUtils.isNotBlank(targetPath)) {
				sendRedirectHeader(response, targetPath, false);	// user-provided, should be already prefixed
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

			query
				.and()
					.or()
					.andTypes(Page.class)
					.andTypes(File.class)
					.parent()
				.and(GraphObject.id, uuid);

			// Searching for pages needs super user context anyway
			List<Linkable> results = query.getAsList();

			logger.debug("{} results", results.size());
			request.setAttribute(POSSIBLE_ENTRY_POINTS_KEY, results);

			return (List<Linkable>) results;
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

			pageQuery.and(StructrApp.key(Page.class, "path"), path).andType(Page.class);
			final List<Linkable> pages = pageQuery.getAsList();

			final Query fileQuery = StructrApp.getInstance(securityContext).nodeQuery();
			fileQuery.and(StructrApp.key(AbstractFile.class, "path"), path).andTypes(File.class);

			final List<Linkable> files = fileQuery.getAsList();

			logger.debug("Found {} pages and {} files/folders", new Object[] { pages.size(), files.size() });

			final List<Linkable> linkables = (List<Linkable>) pages;
			linkables.addAll(files);

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

	public static void setNoCacheHeaders(final HttpServletResponse response) {

		response.setHeader("Cache-Control", "private, max-age=0, s-maxage=0, no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache, no-store"); // HTTP 1.0.
		response.setDateHeader("Expires", 0);
	}

	private static boolean notModifiedSince(final HttpServletRequest request, HttpServletResponse response, final NodeInterface node, final boolean dontCache) {

		boolean notModified = false;
		final Date lastModified = node.getLastModifiedDate();

		// add some caching directives to header
		// see http://weblogs.java.net/blog/2007/08/08/expires-http-header-magic-number-yslow
		final DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		response.setHeader("Date", httpDateFormat.format(new Date()));

		final Integer seconds = node instanceof Page ? ((Page)node).getCacheForSeconds() : (node instanceof File ? ((File)node).getCacheForSeconds() : null);
		final Calendar cal    = new GregorianCalendar();

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

	private void streamFile(final SecurityContext securityContext, final File file, HttpServletRequest request, HttpServletResponse response, final EditMode edit, final boolean sendContent) throws IOException {

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
			// remove any CR LF characters from the filename to prevent Header Splitting attacks
			final String cleanedFilename = FilenameCleanerPattern.matcher(downloadAsFilename).replaceAll("");

			// Set Content-Disposition header to suggest a default filename and force a "save-as" dialog
			// See:
			// http://en.wikipedia.org/wiki/MIME#Content-Disposition,
			// http://tools.ietf.org/html/rfc2183
			// http://tools.ietf.org/html/rfc1806
			// http://tools.ietf.org/html/rfc2616#section-15.5 and http://tools.ietf.org/html/rfc2616#section-19.5.1
			response.addHeader("Content-Disposition", "attachment; filename=\"" + cleanedFilename + "\"");

			callbackMap.put("requestedFileName", downloadAsFilename);
		}

		boolean dontCache = file.getProperty(StructrApp.key(File.class, "dontCache"));

		if (!EditMode.WIDGET.equals(edit) && notModifiedSince(request, response, file, dontCache)) {

			out.flush();
			out.close();

			callbackMap.put("statusCode", HttpServletResponse.SC_NOT_MODIFIED);

		} else {

			final String downloadAsDataUrl = request.getParameter(DOWNLOAD_AS_DATA_URL_KEY);
			if (downloadAsDataUrl != null) {

				final String encoded = FileHelper.getBase64String(file);

				response.setContentLength(encoded.length());
				response.setContentType("text/plain");

				if (sendContent) {
					IOUtils.write(encoded, out, "utf-8");
				}

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

						final long len = FileUtils.getSize(file.getFileOnDisk());
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
						response.setHeader("Content-Length", Long.toString(contentLength));

						response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
						callbackMap.put("statusCode", HttpServletResponse.SC_PARTIAL_CONTENT);

						if (sendContent) {
							IOUtils.copyLarge(in, out, start, contentLength);
						}

					} else {

						if (!file.isTemplate()) {
							response.addHeader("Content-Length", Long.toString(FileUtils.getSize(file.getFileOnDisk())));
						}

						if (sendContent) {
							IOUtils.copyLarge(in, out);
						}

						final int status = response.getStatus();

						response.setStatus(status);

						callbackMap.put("statusCode", status);
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

					response.setStatus(response.getStatus());
				}
			}
		}


		// WIDGET mode means "opened in frontend", which we don't want to count as an external download
		if (!EditMode.WIDGET.equals(edit)) {

			// call onDownload callback
			try {

				file.invokeMethod(securityContext, "onDownload", callbackMap, false, new EvaluationHints());

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
	public static boolean isVisibleForSite(final HttpServletRequest request, final Page page) {

		final List<Site> sites = Iterables.toList(page.getSites());
		if (sites == null || sites.isEmpty()) {

			return true;
		}

		final String serverName = request.getServerName();
		final int serverPort    = request.getServerPort();

		boolean isVisible = false;

		for (final Site site : sites) {

				if (StringUtils.isBlank(serverName) || serverName.equals(site.getHostname())) {
					isVisible = true;
				}

				final Integer sitePort = site.getPort();

				if (isVisible && (sitePort == null || serverPort == sitePort)) {
					isVisible = true;
				}
		}

		return isVisible;

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

					final PropertyKey key = StructrApp.key(type, keyName);
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

	private HttpBasicAuthResult checkHttpBasicAuth(final SecurityContext outerSecurityContext, final HttpServletRequest request, final HttpServletResponse response, final String path) throws IOException, FrameworkException {

		final PropertyKey<Boolean> basicAuthKey     = StructrApp.key(Linkable.class, "enableBasicAuth");
		final PropertyKey<Integer> positionKey      = StructrApp.key(Page.class, "position");
		final PropertyKey<String> filePathKey       = StructrApp.key(File.class, "path");
		final PropertyKey<String> pagePathKey       = StructrApp.key(Page.class, "path");

		// Look for renderable objects using a SuperUserSecurityContext,
		// but dont actually render the page. We're only interested in
		// the authentication settings.
		Linkable possiblePage = null;

		// try the different methods..
		if (possiblePage == null) {
			possiblePage = StructrApp.getInstance().nodeQuery(Page.class).and(pagePathKey, path).and(basicAuthKey, true).sort(positionKey).getFirst();
		}

		if (possiblePage == null) {
			possiblePage = StructrApp.getInstance().nodeQuery(Page.class).and(Page.name, PathHelper.getName(path)).and(basicAuthKey, true).sort(positionKey).getFirst();
		}

		if (possiblePage == null) {
			possiblePage = StructrApp.getInstance().nodeQuery(File.class).and(filePathKey, path).and(basicAuthKey, true).getFirst();
		}

		if (possiblePage == null) {
			possiblePage = StructrApp.getInstance().nodeQuery(File.class).and(File.name, PathHelper.getName(path)).and(basicAuthKey, true).getFirst();
		}

		if (possiblePage != null) {

			String realm = possiblePage.getBasicAuthRealm();
			if (realm == null) {

				realm = possiblePage.getName();
			} else {
				realm = (String)Scripting.replaceVariables(new ActionContext(outerSecurityContext), possiblePage, realm, false, "realm");
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
			realm = realm.replaceAll("\"", "\\\\\"");
			response.setHeader("WWW-Authenticate", "BASIC realm=\"" + realm + "\"");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

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

	public static String filterMaliciousRedirects(final String source) {

		if (source != null) {

			try {

				final URI uri = URI.create(source).normalize();
				if (uri.isAbsolute()) {

					// relativize URI with itself (removes scheme, host and path)
					final URI rel = uri.relativize(uri);

					// concatenate path and query part
					return URI.create(uri.getPath() + rel.toString()).toString();

				} else {

					return uri.toString();
				}

			} catch (Throwable ex) {
				logger.error(ExceptionUtils.getStackTrace(ex));
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
