/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.neo4j.kernel.DeadlockDetectedException;
import org.structr.common.PagingHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.*;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.rest.JsonInputGSONAdapter;
import org.structr.rest.ResourceProvider;
import org.structr.rest.RestMethodResult;
import org.structr.rest.adapter.FrameworkExceptionGSONAdapter;
import org.structr.rest.adapter.ResultGSONAdapter;
import org.structr.rest.resource.Resource;
import org.structr.rest.serialization.StreamingHtmlWriter;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

//~--- classes ----------------------------------------------------------------

/**
 * Implements the structr REST API.
 *
 *
 */
public class JsonRestServlet extends HttpServlet implements HttpServiceServlet {

	public static final int DEFAULT_VALUE_PAGE_SIZE                     = 20;
	public static final String DEFAULT_VALUE_SORT_ORDER                 = "asc";
	public static final String REQUEST_PARAMETER_LOOSE_SEARCH           = "loose";
	public static final String REQUEST_PARAMETER_PAGE_NUMBER            = "page";
	public static final String REQUEST_PARAMETER_PAGE_SIZE              = "pageSize";
	public static final String REQUEST_PARAMETER_OFFSET_ID              = "pageStartId";
	public static final String REQUEST_PARAMETER_SORT_KEY               = "sort";
	public static final String REQUEST_PARAMETER_SORT_ORDER             = "order";
	public static final Set<String> commonRequestParameters             = new LinkedHashSet<>();
	private static final Logger logger                                  = Logger.getLogger(JsonRestServlet.class.getName());

	static {

		commonRequestParameters.add(REQUEST_PARAMETER_LOOSE_SEARCH);
		commonRequestParameters.add(REQUEST_PARAMETER_PAGE_NUMBER);
		commonRequestParameters.add(REQUEST_PARAMETER_PAGE_SIZE);
		commonRequestParameters.add(REQUEST_PARAMETER_OFFSET_ID);
		commonRequestParameters.add(REQUEST_PARAMETER_SORT_KEY);
		commonRequestParameters.add(REQUEST_PARAMETER_SORT_ORDER);
		commonRequestParameters.add("debugLoggingEnabled");

		// cross reference here, but these need to be added as well..
		commonRequestParameters.add(SearchCommand.DISTANCE_SEARCH_KEYWORD);
		commonRequestParameters.add(SearchCommand.LOCATION_SEARCH_KEYWORD);
		commonRequestParameters.add(SearchCommand.STREET_SEARCH_KEYWORD);
		commonRequestParameters.add(SearchCommand.HOUSE_SEARCH_KEYWORD);
		commonRequestParameters.add(SearchCommand.POSTAL_CODE_SEARCH_KEYWORD);
		commonRequestParameters.add(SearchCommand.CITY_SEARCH_KEYWORD);
		commonRequestParameters.add(SearchCommand.STATE_SEARCH_KEYWORD);
		commonRequestParameters.add(SearchCommand.COUNTRY_SEARCH_KEYWORD);
	}

	// final fields
	private final Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<>();
	private final StructrHttpServiceConfig config                     = new StructrHttpServiceConfig();

	// non-final fields
	private Value<String> propertyView       = null;
	private ThreadLocalGson gson             = null;
	private boolean indentJson               = true;

	//~--- methods --------------------------------------------------------

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public void init() {

		try {
			indentJson = Boolean.parseBoolean(StructrApp.getConfigurationValue(Services.JSON_INDENTATION, "true"));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Unable to parse value for {0}: {1}", new Object[] { Services.JSON_INDENTATION, t.getMessage() } );
		}


		// inject resources
		final ResourceProvider provider = config.getResourceProvider();
		if (provider != null) {

			resourceMap.putAll(provider.getResources());

		} else {

			logger.log(Level.SEVERE, "Unable to initialize JsonRestServlet, no resource provider found. Please check structr.conf for a valid resource provider class.");
		}

		// initialize variables
		this.propertyView           = new ThreadLocalPropertyView();
		this.gson                   = new ThreadLocalGson(config.getOutputNestingDepth());
	}

	// <editor-fold defaultstate="collapsed" desc="DELETE">
	@Override
	protected void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		SecurityContext securityContext = null;
		Authenticator authenticator     = null;
		RestMethodResult result         = null;
		Resource resource               = null;

		try {

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8;");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			// isolate resource authentication
			try (final Tx tx = app.tx()) {

				resource = ResourceHelper.optimizeNestedResourceChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView));
				authenticator.checkResourceAccess(securityContext, request, resource.getResourceSignature(), propertyView.get(securityContext));

				tx.success();
			}

			// isolate doDelete
			boolean retry = true;
			while (retry) {

				try (final Tx tx = app.tx()) {
					result = resource.doDelete();
					tx.success();
					retry = false;

				} catch (DeadlockDetectedException ddex) {
					retry = true;
				}
			}

			// isolate write output
			try (final Tx tx = app.tx()) {
				result.commitResponse(gson.get(), response);
				tx.success();
			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in DELETE", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in DELETE: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in DELETE", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in DELETE: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in DELETE", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in DELETE: " + t.getMessage()));

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (IOException t) {

				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}

		}
	}

	// </editor-fold>

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final boolean returnContent = true;

		doGetOrHead(request, response, returnContent);
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		final boolean returnContent = false;

		doGetOrHead(request, response, returnContent);
	}

	// <editor-fold defaultstate="collapsed" desc="OPTIONS">
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		final SecurityContext securityContext;
		final Authenticator authenticator;
		final Resource resource;

		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);

		try {

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8;");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			// isolate resource authentication
			try (final Tx tx = app.tx()) {

				resource = ResourceHelper.applyViewTransformation(request, securityContext, ResourceHelper.optimizeNestedResourceChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView)), propertyView);
				authenticator.checkResourceAccess(securityContext, request, resource.getResourceSignature(), propertyView.get(securityContext));
				tx.success();
			}

			// isolate doOptions
			boolean retry = true;
			while (retry) {

				try (final Tx tx = app.tx()) {

					result = resource.doOptions();
					tx.success();
					retry = false;

				} catch (DeadlockDetectedException ddex) {
					retry = true;
				}
			}

			// isolate write output
			try (final Tx tx = app.tx()) {
				result.commitResponse(gson.get(), response);
				tx.success();
			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in OPTIONS", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in OPTIONS: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in OPTIONS", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in OPTIONS: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in OPTIONS", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in OPTIONS: " + t.getMessage()));

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}

		}
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="POST">
	@Override
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		final List<RestMethodResult> results = new LinkedList<>();
		final SecurityContext securityContext;
		final Authenticator authenticator;
		final Resource resource;

		try {

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8;");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app              = StructrApp.getInstance(securityContext);
			final String input         = IOUtils.toString(request.getReader());
			final IJsonInput jsonInput = cleanAndParseJsonString(app, input);

			if (securityContext != null) {

				// isolate resource authentication
				try (final Tx tx = app.tx()) {

					resource = ResourceHelper.applyViewTransformation(request, securityContext, ResourceHelper.optimizeNestedResourceChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView)), propertyView);
					authenticator.checkResourceAccess(securityContext, request, resource.getResourceSignature(), propertyView.get(securityContext));
					tx.success();
				}

				// isolate doPost
				boolean retry = true;
				while (retry) {

					if (resource.createPostTransaction()) {

						try (final Tx tx = app.tx()) {

							for (JsonInput propertySet : jsonInput.getJsonInputs()) {

								results.add(resource.doPost(convertPropertySetToMap(propertySet)));
							}

							tx.success();
							retry = false;

						} catch (DeadlockDetectedException ddex) {
							retry = true;
						}

					} else {

						try {

							for (JsonInput propertySet : jsonInput.getJsonInputs()) {

								results.add(resource.doPost(convertPropertySetToMap(propertySet)));
							}

							retry = false;

						} catch (DeadlockDetectedException ddex) {
							retry = true;
						}
					}
				}

				// set default value for property view
				propertyView.set(securityContext, config.getDefaultPropertyView());

				// isolate write output
				try (final Tx tx = app.tx()) {

					if (!results.isEmpty()) {

						final RestMethodResult result = results.get(0);
						final int resultCount         = results.size();

						if (result != null) {

							if (resultCount > 1) {

								for (final RestMethodResult r : results) {

									final GraphObject objectCreated = r.getContent().get(0);
									if (!result.getContent().contains(objectCreated)) {

										result.addContent(objectCreated);
									}

								}

								// remove Location header if more than one object was
								// written because it may only contain a single URL
								result.addHeader("Location", null);
							}

							result.commitResponse(gson.get(), response);
						}

					}

					tx.success();
				}

			} else {

				// isolate write output
				try (final Tx tx = app.tx()) {

					new RestMethodResult(HttpServletResponse.SC_FORBIDDEN).commitResponse(gson.get(), response);
					tx.success();
				}

			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "POST: Invalid JSON syntax", jsex.getMessage());

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in POST: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "Unable to parse JSON string", jpex.getMessage());

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonParseException in POST: " + jpex.getMessage()));

		} catch (UnsupportedOperationException uoe) {

			logger.log(Level.WARNING, "POST not supported");

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "POST not supported: " + uoe.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in POST", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in POST: " + t.getMessage()));

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}

		}
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="PUT">
	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final SecurityContext securityContext;
		final Authenticator authenticator;
		final Resource resource;

		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);

		try {

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8;");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app              = StructrApp.getInstance(securityContext);
			final String input         = IOUtils.toString(request.getReader());
			final IJsonInput jsonInput = cleanAndParseJsonString(app, input);

			if (securityContext != null) {

				// isolate resource authentication
				try (final Tx tx = app.tx()) {

					// evaluate constraint chain
					resource = ResourceHelper.applyViewTransformation(request, securityContext, ResourceHelper.optimizeNestedResourceChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView)), propertyView);
					authenticator.checkResourceAccess(securityContext, request, resource.getResourceSignature(), propertyView.get(securityContext));
					tx.success();
				}

				// isolate doPut
				boolean retry = true;
				while (retry) {

					try (final Tx tx = app.tx()) {
						result = resource.doPut(convertPropertySetToMap(jsonInput.getJsonInputs().get(0)));
						tx.success();
						retry = false;

					} catch (DeadlockDetectedException ddex) {
						retry = true;
					}
				}

				// isolate write output
				try (final Tx tx = app.tx()) {
					result.commitResponse(gson.get(), response);
					tx.success();
				}

			} else {

				// isolate write output
				try (final Tx tx = app.tx()) {
					result = new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);
					result.commitResponse(gson.get(), response);
					tx.success();
				}

			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "PUT: Invalid JSON syntax", jsex.getMessage());

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in PUT: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "PUT: Unable to parse JSON string", jpex.getMessage());

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in PUT: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in PUT", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in PUT: " + t.getMessage()));

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}

		}
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="TRACE">
	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

//		logRequest("TRACE", request);
		response.setContentType("application/json; charset=UTF-8;");
		response.setCharacterEncoding("UTF-8");

		int code = HttpServletResponse.SC_METHOD_NOT_ALLOWED;

		response.setStatus(code);
		response.getWriter().append(RestMethodResult.jsonError(code, "TRACE method not allowed"));
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="private methods">

	private IJsonInput cleanAndParseJsonString(final App app, final String input) throws FrameworkException {

		IJsonInput jsonInput;

		// isolate input parsing (will include read and write operations)
		try (final Tx tx = app.tx()) {
			jsonInput   = gson.get().fromJson(input, IJsonInput.class);
			tx.success();
		}

		if (jsonInput == null) {

			if (StringUtils.isBlank(input)) {

				try (final Tx tx = app.tx()) {
					jsonInput   = gson.get().fromJson("{}", IJsonInput.class);
					tx.success();
				}

			} else {
				//throw new JsonParseException("Invalid or empty JSON string, must at least contain {} to be valid!");
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

	private void doGetOrHead(final HttpServletRequest request, final HttpServletResponse response, final boolean returnContent) throws ServletException, IOException {

		SecurityContext securityContext = null;
		Authenticator authenticator     = null;
		Result result                   = null;
		Resource resource               = null;

		try {

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8;");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			// set default value for property view
			propertyView.set(securityContext, config.getDefaultPropertyView());

			// evaluate constraints and measure query time
			double queryTimeStart    = System.nanoTime();

			// isolate resource authentication
			try (final Tx tx = app.tx()) {

				resource = ResourceHelper.applyViewTransformation(request, securityContext, ResourceHelper.optimizeNestedResourceChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView)), propertyView);
				authenticator.checkResourceAccess(securityContext, request, resource.getResourceSignature(), propertyView.get(securityContext));
				tx.success();
			}

			// add sorting & paging
			String pageSizeParameter = request.getParameter(REQUEST_PARAMETER_PAGE_SIZE);
			String pageParameter     = request.getParameter(REQUEST_PARAMETER_PAGE_NUMBER);
			String offsetId          = request.getParameter(REQUEST_PARAMETER_OFFSET_ID);
			String sortOrder         = request.getParameter(REQUEST_PARAMETER_SORT_ORDER);
			String sortKeyName       = request.getParameter(REQUEST_PARAMETER_SORT_KEY);
			boolean sortDescending   = (sortOrder != null && "desc".equals(sortOrder.toLowerCase()));
			int pageSize		 = Services.parseInt(pageSizeParameter, NodeFactory.DEFAULT_PAGE_SIZE);
			int page                 = Services.parseInt(pageParameter, NodeFactory.DEFAULT_PAGE);
			String baseUrl           = request.getRequestURI();
			PropertyKey sortKey      = null;

			// set sort key
			if (sortKeyName != null) {

				Class<? extends GraphObject> type = resource.getEntityClass();
				if (type == null) {

					// fallback to default implementation
					// if no type can be determined
					type = AbstractNode.class;
				}

				sortKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(type, sortKeyName, false);
			}

			// isolate doGet
			boolean retry = true;
			while (retry) {

				try (final Tx tx = app.tx()) {
					result = resource.doGet(sortKey, sortDescending, pageSize, page, offsetId);
					tx.success();
					retry = false;

				} catch (DeadlockDetectedException ddex) {
					retry = true;
				}
			}

			if (returnContent) {

				result.setIsCollection(resource.isCollectionResource());
				result.setIsPrimitiveArray(resource.isPrimitiveArray());

				PagingHelper.addPagingParameter(result, pageSize, page);

				// timing..
				double queryTimeEnd = System.nanoTime();

				// store property view that will be used to render the results
				result.setPropertyView(propertyView.get(securityContext));

				// allow resource to modify result set
				resource.postProcessResultSet(result);

				DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
				result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

				String accept = request.getHeader("Accept");

				if (accept != null && accept.contains("text/html")) {

					final StreamingHtmlWriter htmlStreamer = new StreamingHtmlWriter(this.propertyView, indentJson, config.getOutputNestingDepth());

					// isolate write output
					try (final Tx tx = app.tx()) {

						response.setContentType("text/html; charset=utf-8;");

						try (final Writer writer = response.getWriter()) {

							htmlStreamer.stream(securityContext, writer, result, baseUrl);
							writer.append("\n");    // useful newline
						}

						tx.success();
					}

				} else {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(this.propertyView, indentJson, config.getOutputNestingDepth());

					// isolate write output
					try (final Tx tx = app.tx()) {

						response.setContentType("application/json; charset=utf-8;");
						try (final Writer writer = response.getWriter()) {

							jsonStreamer.stream(securityContext, writer, result, baseUrl);
							writer.append("\n");    // useful newline
						}

						tx.success();
					}

				}
			}

			response.setStatus(HttpServletResponse.SC_OK);

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in GET", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "Json syntax exception in GET: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in GET", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "Parser exception in GET: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in GET", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "Exception in GET: " + t.getMessage()));

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}

		}
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="nested classes">
	private class ThreadLocalPropertyView extends ThreadLocal<String> implements Value<String> {

		@Override
		protected String initialValue() {
			return config.getDefaultPropertyView();
		}

		@Override
		public void set(SecurityContext securityContext, String value) {
			set(value);
		}

		@Override
		public String get(SecurityContext securityContext) {
			return get();
		}
	}

	public class ThreadLocalGson extends ThreadLocal<Gson> {

		private int outputNestingDepth = 3;

		public ThreadLocalGson(final int outputNestingDepth) {
			this.outputNestingDepth = outputNestingDepth;
		}

		@Override
		protected Gson initialValue() {

			ResultGSONAdapter resultGsonAdapter   = new ResultGSONAdapter(propertyView, outputNestingDepth);
			JsonInputGSONAdapter jsonInputAdapter = new JsonInputGSONAdapter();

			// create GSON serializer
			final GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.serializeNulls()
				.registerTypeHierarchyAdapter(FrameworkException.class, new FrameworkExceptionGSONAdapter())
				.registerTypeAdapter(IJsonInput.class, jsonInputAdapter)
				.registerTypeAdapter(Result.class, resultGsonAdapter);


			final boolean lenient = Boolean.parseBoolean(StructrApp.getConfigurationValue("json.lenient", "false"));
			if (lenient) {

				// Serializes NaN, -Infinity, Infinity, see http://code.google.com/p/google-gson/issues/detail?id=378
				gsonBuilder.serializeSpecialFloatingPointValues();

			}

			return gsonBuilder.create();
		}
	}
	// </editor-fold>
}
