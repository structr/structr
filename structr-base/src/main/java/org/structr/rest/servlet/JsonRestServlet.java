/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.io.QuietException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.api.util.ResultStream;
import org.structr.common.PropertyView;
import org.structr.common.RequestKeywords;
import org.structr.common.SecurityContext;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.*;
import org.structr.core.IJsonInput;
import org.structr.core.JsonInput;
import org.structr.core.JsonSingleInput;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTEndpoints;
import org.structr.web.entity.User;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.traits.StructrTraits;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTEndpoints;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Implements the structr REST API.
 */
public class JsonRestServlet extends AbstractDataServlet {

	private static final Logger logger                  = LoggerFactory.getLogger(JsonRestServlet.class.getName());
	public static final int DEFAULT_VALUE_PAGE_SIZE     = 20;
	public static final String DEFAULT_VALUE_SORT_ORDER = "asc";

	// ----- protected methods -----
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {

		final String method = request.getMethod();
	        if ("PATCH".equals(method)) {

			doPatch(request, resp);
			return;
		}

		super.service(request, resp);
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "rest";
	}

	// ----- HTTP methods -----
	@Override
	protected void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		SecurityContext securityContext = null;
		Authenticator authenticator     = null;
		RestMethodResult result         = null;
		RESTCallHandler handler         = null;

		setCustomResponseHeaders(response);

		try {

			assertInitialized();

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			// isolate resource authentication
			try (final Tx tx = app.tx()) {

				final Principal currentUser = securityContext.getUser(false);

				handler = RESTEndpoints.resolveRESTCallHandler(request, config.getDefaultPropertyView(), getTypeOrDefault(currentUser, StructrTraits.USER));
				authenticator.checkResourceAccess(securityContext, request, handler.getResourceSignature(), handler.getRequestedView());

				RuntimeEventLog.rest("Delete", handler.getResourceSignature(), currentUser);

				tx.success();
			}

			// isolate doDelete
			boolean retry = true;
			while (retry) {

				try {

					result = handler.doDelete(securityContext);
					retry = false;

				} catch (RetryException ddex) {
					retry = true;
				}
			}

			// isolate write output
			try (final Tx tx = app.tx()) {
				commitResponse(securityContext, request, response, result, handler.getRequestedView(), handler.isCollection());
				tx.success();
			}

		} catch (FrameworkException | AssertException jsonException) {

			writeException(response, jsonException);

		} catch (JsonSyntaxException jsex) {

			logger.warn("JsonSyntaxException in DELETE", jsex);

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonSyntaxException in DELETE: " + jsex.getMessage());

		} catch (JsonParseException jpex) {

			logger.warn("JsonParseException in DELETE", jpex);

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonParseException in DELETE: " + jpex.getMessage());

		} catch (Throwable t) {

			logger.warn("Exception in DELETE", t);

			writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getClass().getSimpleName() + " in DELETE: " + t.getMessage());

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (IOException t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}

		}
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final boolean returnContent = true;

		try {

			assertInitialized();

			doGetOrHead(request, response, returnContent);

		} catch (FrameworkException fex) {
			writeException(response, fex);
		}
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		final boolean returnContent = false;

		try {

			assertInitialized();

			doGetOrHead(request, response, returnContent);

		} catch (FrameworkException fex) {
			writeException(response, fex);
		}
	}

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		setCustomResponseHeaders(response);

		try {

			assertInitialized();

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {

				final Authenticator authenticator = config.getAuthenticator();
				authenticator.initializeAndExamineRequest(request, response);

				tx.success();
			}

			// Note: some resources need the type of the current user to resolve the correct handler, so this might return wrong
			// results, especially in the /me resource where the result might depend on the actual user type that makes the call
			try (final Tx tx = StructrApp.getInstance().tx()) {

				final RESTCallHandler handler = RESTEndpoints.resolveRESTCallHandler(request, config.getDefaultPropertyView(), StructrTraits.USER);
				if (handler != null) {

					RuntimeEventLog.rest("Options", handler.getResourceSignature(), null);

					final Set<String> allowedVerbs = handler.getAllowedHttpMethodsForOptionsCall();
					if (allowedVerbs != null) {

						response.setHeader("Allow", StringUtils.join(allowedVerbs, ","));
					}

				} else {

					RuntimeEventLog.rest("Options", request.getPathInfo(), null);
				}

				tx.success();
			}

			response.setStatus(HttpServletResponse.SC_OK);

		} catch (FrameworkException | AssertException jsonException) {

			writeException(response, jsonException);

		} catch (JsonSyntaxException jsex) {

			logger.warn("OPTIONS: Invalid JSON syntax", jsex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonSyntaxException in OPTIONS: " + jsex.getMessage());

		} catch (JsonParseException jpex) {

			logger.warn("OPTIONS: Unable to parse JSON string", jpex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonParseException in OPTIONS: " + jpex.getMessage());

		} catch (Throwable t) {

			logger.warn("Exception in OPTIONS => Error thrown: ", t);

			writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getClass().getSimpleName() + " in OPTIONS: " + t.getMessage());

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}

		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		final List<RestMethodResult> results = new LinkedList<>();
		final SecurityContext securityContext;
		final Authenticator authenticator;
		final RESTCallHandler handler;

		setCustomResponseHeaders(response);

		try {

			assertInitialized();

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// get reader before initalizing security context
			final String input = IOUtils.toString(request.getReader());

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app              = StructrApp.getInstance(securityContext);
			final IJsonInput jsonInput = cleanAndParseJsonString(app, input);

			if (securityContext != null) {

				// isolate resource authentication
				try (final Tx tx = app.tx()) {

					final Principal currentUser = securityContext.getUser(false);

					handler = RESTEndpoints.resolveRESTCallHandler(request, config.getDefaultPropertyView(), getTypeOrDefault(currentUser, StructrTraits.USER));
					authenticator.checkResourceAccess(securityContext, request, handler.getResourceSignature(), handler.getRequestedView());

					RuntimeEventLog.rest("Post", handler.getResourceSignature(), currentUser);

					tx.success();
				}

				// isolate doPost
				boolean retry = true;
				while (retry) {

					if (handler.createPostTransaction()) {

						try (final Tx tx = app.tx()) {

							tx.prefetchHint("REST POST " + handler.getURL());

							for (JsonInput propertySet : jsonInput.getJsonInputs()) {

								results.add(handler.doPost(securityContext, convertPropertySetToMap(propertySet)));
							}

							tx.success();
							retry = false;

						} catch (RetryException ddex) {
							retry = true;
						}

					} else {

						try {

							for (JsonInput propertySet : jsonInput.getJsonInputs()) {

								results.add(handler.doPost(securityContext, convertPropertySetToMap(propertySet)));
							}

							retry = false;

						} catch (RetryException ddex) {
							retry = true;
						}
					}
				}

				// isolate write output
				try (final Tx tx = app.tx()) {

					if (!results.isEmpty()) {

						tx.prefetchHint("REST POST RESULTS " + handler.getURL());

						final RestMethodResult result = results.get(0);
						final int resultCount         = results.size();

						if (securityContext.returnDetailedCreationResults()) {

							// remove previous results (might be string primitive which shouldn't be mixed with objects)
							result.getContent().clear();

							// return details for all objects that were created in this transaction
							for (final Object obj : securityContext.getCreationDetails()) {

								result.addContent(obj);
							}

						} else {

							if (resultCount > 1) {

								for (final RestMethodResult r : results) {

									final Object objectCreated = r.getContent().get(0);
									if (!result.getContent().contains(objectCreated)) {

										result.addContent(objectCreated);
									}

								}

								// remove Location header if more than one object was
								// written because it may only contain a single URL
								result.addHeader(StructrTraits.LOCATION, null);
							}
						}

						commitResponse(securityContext, request, response, result, handler.getRequestedView(), handler.isCollection());
					}

					tx.success();
				}

			} else {

				// isolate write output
				try (final Tx tx = app.tx()) {

					final RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

					commitResponse(securityContext, request, response, result, PropertyView.Public, false);

					tx.success();
				}

			}

		} catch (FrameworkException | AssertException jsonException) {

			writeException(response, jsonException);

		} catch (JsonSyntaxException jsex) {

			logger.warn("POST: Invalid JSON syntax", jsex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonSyntaxException in POST: " + jsex.getMessage());

		} catch (JsonParseException jpex) {

			logger.warn("Unable to parse JSON string", jpex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonParseException in POST: " + jpex.getMessage());

		} catch (UnsupportedOperationException uoe) {

			logger.warn("Unsupported operation in POST", uoe.getMessage());
			logger.warn(" => Error thrown: ", uoe);

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Unsupported operation in POST: " + uoe.getMessage());

		} catch (Throwable t) {

			logger.warn("Exception in POST", t);

			writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getClass().getSimpleName() + " in POST: " + t.getMessage());

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}
		}
	}

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final SecurityContext securityContext;
		final Authenticator authenticator;
		final RESTCallHandler handler;

		setCustomResponseHeaders(response);

		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);

		try {

			assertInitialized();

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// get reader before initalizing security context
			final String input = IOUtils.toString(request.getReader());

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app              = StructrApp.getInstance(securityContext);
			final IJsonInput jsonInput = cleanAndParseJsonString(app, input);

			if (securityContext != null) {

				// isolate resource authentication
				try (final Tx tx = app.tx()) {

					final Principal currentUser = securityContext.getUser(false);

					// evaluate constraint chain
					handler = RESTEndpoints.resolveRESTCallHandler(request, config.getDefaultPropertyView(), getTypeOrDefault(currentUser, StructrTraits.USER));
					authenticator.checkResourceAccess(securityContext, request, handler.getResourceSignature(), handler.getRequestedView());

					RuntimeEventLog.rest("Put", handler.getResourceSignature(), currentUser);

					tx.success();
				}

				// isolate doPut
				boolean retry = true;
				while (retry) {

					try (final Tx tx = app.tx()) {

						tx.prefetchHint("REST PUT " + handler.getURL());

						result = handler.doPut(securityContext, convertPropertySetToMap(jsonInput.getJsonInputs().get(0)));
						tx.success();
						retry = false;

					} catch (RetryException ddex) {
						retry = true;
					}
				}

				// isolate write output
				try (final Tx tx = app.tx()) {

					tx.prefetchHint("REST PUT RESULTS " + handler.getURL());

					commitResponse(securityContext, request, response, result, handler.getRequestedView(), handler.isCollection());
					tx.success();
				}

			} else {

				// isolate write output
				try (final Tx tx = app.tx()) {

					result = new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);
					commitResponse(securityContext, request, response, result, PropertyView.Public, false);

					tx.success();
				}

			}

		} catch (FrameworkException | AssertException jsonException) {

			writeException(response, jsonException);

		} catch (JsonSyntaxException jsex) {

			logger.warn("PUT: Invalid JSON syntax", jsex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonSyntaxException in PUT: " + jsex.getMessage());

		} catch (JsonParseException jpex) {

			logger.warn("PUT: Unable to parse JSON string", jpex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonParseException in PUT: " + jpex.getMessage());

		} catch (Throwable t) {

			logger.warn("Exception in PUT", t);
			logger.warn(" => Error thrown: ", t);

			writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getClass().getSimpleName() + " in PUT: " + t.getMessage());

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}

		}
	}

	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		setCustomResponseHeaders(response);

		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		writeJsonError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "TRACE method not allowed");
	}

	protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		final SecurityContext securityContext;
		final Authenticator authenticator;
		final RESTCallHandler handler;

		setCustomResponseHeaders(response);

		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);

		try {

			assertInitialized();

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// get reader before initalizing security context
			final String input = IOUtils.toString(request.getReader());

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app              = StructrApp.getInstance(securityContext);
			final IJsonInput jsonInput = cleanAndParseJsonString(app, input);

			if (securityContext != null) {

				// allow setting of nested properties in PATCH documents
				securityContext.setAttribute("setNestedProperties", true);

				// isolate resource authentication
				try (final Tx tx = app.tx()) {

					final Principal currentUser = securityContext.getUser(false);

					handler = RESTEndpoints.resolveRESTCallHandler(request, config.getDefaultPropertyView(), getTypeOrDefault(currentUser, StructrTraits.USER));
					authenticator.checkResourceAccess(securityContext, request, handler.getResourceSignature(), handler.getRequestedView());

					RuntimeEventLog.rest("Patch", handler.getResourceSignature(), currentUser);

					tx.success();
				}

				final List<Map<String, Object>> inputs = new LinkedList<>();

				for (JsonInput propertySet : jsonInput.getJsonInputs()) {

					inputs.add(convertPropertySetToMap(propertySet));
				}

				// isolate doPatch
				boolean retry = true;
				while (retry) {

					try (final Tx tx = app.tx()) {

						tx.prefetchHint("REST PATCH " + handler.getURL());

						result = handler.doPatch(securityContext, inputs);
						tx.success();
						retry = false;

					} catch (RetryException ddex) {
						retry = true;
					}
				}

				// isolate write output
				try (final Tx tx = app.tx()) {

					if (result != null) {

						tx.prefetchHint("REST PATCH RESULTS " + handler.getURL());

						commitResponse(securityContext, request, response, result, handler.getRequestedView(), handler.isCollection());
					}

					tx.success();
				}

			} else {

				// isolate write output
				try (final Tx tx = app.tx()) {

					result = new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

					commitResponse(securityContext, request, response, result, PropertyView.Public, false);

					tx.success();
				}
			}

		} catch (FrameworkException | AssertException jsonException) {

			writeException(response, jsonException);

		} catch (JsonSyntaxException jsex) {

			logger.warn("PATCH: Invalid JSON syntax", jsex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonSyntaxException in PATCH: " + jsex.getMessage());

		} catch (JsonParseException jpex) {

			logger.warn("Unable to parse JSON string", jpex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonParseException in PATCH: " + jpex.getMessage());

		} catch (UnsupportedOperationException uoe) {

			logger.warn("Unsupported operation in PATCH", uoe.getMessage());
			logger.warn(" => Error thrown: ", uoe);

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Unsupported operation in PATCH: " + uoe.getMessage());

		} catch (Throwable t) {

			logger.warn("Exception in PATCH", t);

			writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getClass().getSimpleName() + " in PATCH: " + t.getMessage());

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}

		}
	}

	protected void doGetOrHead(final HttpServletRequest request, final HttpServletResponse response, final boolean returnContent) throws ServletException, IOException {

		final long t0 = System.currentTimeMillis();

		SecurityContext securityContext = null;
		Authenticator authenticator     = null;
		RESTCallHandler handler          = null;

		setCustomResponseHeaders(response);

		try (final Tx tx = StructrApp.getInstance().tx()) {

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			authenticator = config.getAuthenticator();
			securityContext = authenticator.initializeAndExamineRequest(request, response);

			final Principal currentUser = securityContext.getUser(false);

			handler = RESTEndpoints.resolveRESTCallHandler(request, config.getDefaultPropertyView(), getTypeOrDefault(currentUser, StructrTraits.USER));
			authenticator.checkResourceAccess(securityContext, request, handler.getResourceSignature(), handler.getRequestedView());

			RuntimeEventLog.rest(returnContent ? "Get" : "Head", handler.getResourceSignature(), currentUser);

			tx.prefetchHint("REST GET " + handler.getURL());

			// add sorting && pagination
			final String pageSizeParameter          = request.getParameter(RequestKeywords.PageSize.keyword());
			final String pageParameter              = request.getParameter(RequestKeywords.PageNumber.keyword());
			final String outputDepth                = request.getParameter(RequestKeywords.OutputDepth.keyword());
			final int pageSize                      = Services.parseInt(pageSizeParameter, NodeFactory.DEFAULT_PAGE_SIZE);
			final int page                          = Services.parseInt(pageParameter, NodeFactory.DEFAULT_PAGE);
			final int depth                         = Services.parseInt(outputDepth, config.getOutputNestingDepth());
			final String[] sortKeyNames             = request.getParameterValues(RequestKeywords.SortKey.keyword());
			final String[] sortOrders               = request.getParameterValues(RequestKeywords.SortOrder.keyword());
			final String type                       = handler.getEntityClassOrDefault(securityContext);

			// evaluate constraints and measure query time
			final double queryTimeStart = System.nanoTime();

			try (final ResultStream result = handler.doGet(securityContext, new DefaultSortOrder(type, sortKeyNames, sortOrders), pageSize, page)) {

				final double queryTimeEnd = System.nanoTime();

				if (result == null) {

					throw new FrameworkException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Unable to retrieve result, check database connection");
				}

				if (returnContent) {

					final DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
					result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

					processResult(securityContext, request, response, result, handler.getRequestedView(), depth, handler.isCollection());
				}
			}

			tx.success();

			response.setStatus(HttpServletResponse.SC_OK);

		} catch (FrameworkException | AssertException jsonException) {

			writeException(response, jsonException);

		} catch (Throwable t) {

			if (t instanceof QuietException || t.getCause() instanceof QuietException) {
				// ignore exceptions which (by jettys standards) should be handled less verbosely
			} else {
				logger.warn("Exception in GET (URI: {})", securityContext != null ? securityContext.getCompoundRequestURI() : "(null SecurityContext)");
				logger.warn(" => Error thrown: ", t);
			}

			writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getClass().getSimpleName() + " in GET: " + t.getMessage());

		} finally {

			try {
				response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}

		}

		if (handler != null) {
			this.stats.recordStatsValue("json", handler.getResourceSignature(), System.currentTimeMillis() - t0);
		}
	}

	// ----- private methods -----
	private IJsonInput cleanAndParseJsonString(final App app, final String input) throws FrameworkException {

		final Gson gson      = getGson();
		IJsonInput jsonInput = null;

		// isolate input parsing (will include read and write operations)
		try (final Tx tx = app.tx()) {

			jsonInput   = gson.fromJson(input, IJsonInput.class);
			tx.success();

		} catch (JsonSyntaxException jsx) {
			logger.warn("", jsx);
			throw new FrameworkException(400, jsx.getMessage());
		}

		if (jsonInput == null) {

			if (StringUtils.isBlank(input)) {

				try (final Tx tx = app.tx()) {

					jsonInput   = gson.fromJson("{}", IJsonInput.class);
					tx.success();
				}

			} else {

				jsonInput = new JsonSingleInput();
			}
		}

		return jsonInput;

	}

	private Map<String, Object> convertPropertySetToMap(JsonInput propertySet) {

		if (propertySet != null) {
			return propertySet.getAttributes();
		}

		return new LinkedHashMap<>();
	}
}
