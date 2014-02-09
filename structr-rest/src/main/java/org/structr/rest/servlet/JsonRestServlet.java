/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.servlet;

import org.structr.rest.JsonInputGSONAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang.StringUtils;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.rest.RestMethodResult;
import org.structr.common.PagingHelper;
import org.structr.rest.resource.Resource;
import org.structr.core.Result;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.Writer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.property.PropertyKey;
import org.structr.core.*;
import org.structr.core.Value;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.search.Search;
import org.structr.rest.serialization.StreamingWriter;
import org.structr.rest.adapter.FrameworkExceptionGSONAdapter;
import org.structr.rest.adapter.ResultGSONAdapter;
import org.structr.rest.serialization.StreamingHtmlWriter;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.rest.service.HttpServiceServlet;

//~--- classes ----------------------------------------------------------------

/**
 * Implements the structr REST API. The input and output format of the JSON
 * serialisation can be configured with the servlet init parameter "PropertyFormat".
 * ({@see PropertyFormat}).
 *
 * @author Christian Morgner
 */
public class JsonRestServlet extends HttpServiceServlet {

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
		
		// cross reference here, but these need to be added as well..
		commonRequestParameters.add(Search.DISTANCE_SEARCH_KEYWORD);
		commonRequestParameters.add(Search.LOCATION_SEARCH_KEYWORD);
		commonRequestParameters.add(Search.STREET_SEARCH_KEYWORD);
		commonRequestParameters.add(Search.HOUSE_SEARCH_KEYWORD);
		commonRequestParameters.add(Search.POSTAL_CODE_SEARCH_KEYWORD);
		commonRequestParameters.add(Search.CITY_SEARCH_KEYWORD);
		commonRequestParameters.add(Search.STATE_SEARCH_KEYWORD);
		commonRequestParameters.add(Search.COUNTRY_SEARCH_KEYWORD);
	}
	
	//~--- fields ---------------------------------------------------------

	private Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<>();
	private Value<String> propertyView                          = null;
	private ThreadLocalGson gson                                = null;
	private ThreadLocalJsonWriter jsonWriter                    = null;
	private ThreadLocalHtmlWriter htmlWriter                    = null;
	private Writer logWriter                                    = null;
	
	@Override
	public void init() {
		
		boolean indentJson = true;
		
		try {
			indentJson = Boolean.parseBoolean(StructrApp.getConfigurationValue(Services.JSON_INDENTATION, "true"));
		
		} catch (Throwable t) {
			
			logger.log(Level.WARNING, "Unable to parse value for {0}: {1}", new Object[] { Services.JSON_INDENTATION, t.getMessage() } );
		}
		
		
		// inject resources
		resourceMap.putAll(resourceProvider.getResources());

		// initialize variables
		this.propertyView   = new ThreadLocalPropertyView();
		this.gson           = new ThreadLocalGson(outputNestingDepth);
		this.jsonWriter     = new ThreadLocalJsonWriter(propertyView, indentJson, outputNestingDepth);
		this.htmlWriter     = new ThreadLocalHtmlWriter(propertyView, indentJson, outputNestingDepth);

	}

	@Override
	public void destroy() {

		if (logWriter != null) {

			try {

				logWriter.flush();
				logWriter.close();

			} catch (IOException ioex) {
				logger.log(Level.WARNING, "Could not close access log file.", ioex);
			}

		}
	}

	// <editor-fold defaultstate="collapsed" desc="DELETE">
	@Override
	protected void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		SecurityContext securityContext = null;

		try {

			Authenticator authenticator     = getAuthenticator();
			securityContext = authenticator.initializeAndExamineRequest(request, response);

//			logRequest("DELETE", request);
			request.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			List<Resource> chain            = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
			Resource resourceConstraint     = ResourceHelper.optimizeNestedResourceChain(chain, defaultIdProperty);
			String resourceSignature        = resourceConstraint.getResourceSignature();

			// let authenticator examine request again
			authenticator.checkResourceAccess(request, resourceSignature, propertyView.get(securityContext));

			// do action
			RestMethodResult result = resourceConstraint.doDelete();

			// commit response
			result.commitResponse(gson.get(), response);

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in DELETE", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in DELETE: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in DELETE", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in DELETE: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in DELETE", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in DELETE: " + t.getMessage()));
			
		} finally {

			try {
				response.getWriter().flush();
				response.getWriter().close();
				
			} catch (Throwable t) {
				
				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}
		
			securityContext.cleanUp();
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="GET">
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		SecurityContext securityContext = null;

		try {

			Authenticator authenticator = getAuthenticator();
			securityContext = authenticator.initializeAndExamineRequest(request, response);

//			logRequest("GET", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");

			// set default value for property view
			propertyView.set(securityContext, defaultPropertyView);

			// evaluate constraints and measure query time
			double queryTimeStart    = System.nanoTime();
			Resource resource        = ResourceHelper.applyViewTransformation(request, securityContext, ResourceHelper.optimizeNestedResourceChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty), defaultIdProperty), propertyView);
			String resourceSignature = resource.getResourceSignature();
			
			// check access rights for this resource
			authenticator.checkResourceAccess(request, resourceSignature, propertyView.get(securityContext));
			
			// add sorting & paging
			String pageSizeParameter = request.getParameter(REQUEST_PARAMETER_PAGE_SIZE);
			String pageParameter     = request.getParameter(REQUEST_PARAMETER_PAGE_NUMBER);
			String offsetId          = request.getParameter(REQUEST_PARAMETER_OFFSET_ID);
			String sortOrder         = request.getParameter(REQUEST_PARAMETER_SORT_ORDER);
			String sortKeyName       = request.getParameter(REQUEST_PARAMETER_SORT_KEY);
			boolean sortDescending   = (sortOrder != null && "desc".equals(sortOrder.toLowerCase()));
			int pageSize		 = parseInt(pageSizeParameter, NodeFactory.DEFAULT_PAGE_SIZE);
			int page                 = parseInt(pageParameter, NodeFactory.DEFAULT_PAGE);
			String baseUrl           = request.getRequestURI();
			PropertyKey sortKey      = null;

			// set sort key
			if (sortKeyName != null) {
				
				Class<? extends GraphObject> type = resource.getEntityClass();
				sortKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(type, sortKeyName);
			}
			
			// do action
			Result result = resource.doGet(sortKey, sortDescending, pageSize, page, offsetId);
			result.setIsCollection(resource.isCollectionResource());
			result.setIsPrimitiveArray(resource.isPrimitiveArray());
			
			PagingHelper.addPagingParameter(result, pageSize, page);

			// timing..
			double queryTimeEnd   = System.nanoTime();

			// store property view that will be used to render the results
			result.setPropertyView(propertyView.get(securityContext));

			// allow resource to modify result set
			resource.postProcessResultSet(result);

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

			Writer writer = response.getWriter();
			String accept = request.getHeader("Accept");

			if (accept != null && accept.contains("text/html")) {
				
				response.setContentType("text/html; charset=utf-8");
				htmlWriter.get().stream(writer, result, baseUrl);
			
			} else {
			
				response.setContentType("application/json; charset=utf-8");
				jsonWriter.get().stream(writer, result, baseUrl);
				
			}

			if (result.hasPartialContent()) {

				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

			} else {

				response.setStatus(HttpServletResponse.SC_OK);
			}

			writer.append("\n");    // useful newline

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in GET", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "Json syntax exception in GET: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in GET", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "Parser exception in GET: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in GET", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "Exception in GET: " + t.getMessage()));
			
		} finally {

			try {
				response.getWriter().flush();
				response.getWriter().close();
				
			} catch (Throwable t) {
				
				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}
			
			securityContext.cleanUp();
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="HEAD">
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		SecurityContext securityContext = null;

		try {

			Authenticator authenticator     = getAuthenticator();
			securityContext = authenticator.initializeAndExamineRequest(request, response);

//			logRequest("HEAD", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			List<Resource> chain      = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
			Resource resource         = ResourceHelper.optimizeNestedResourceChain(chain, defaultIdProperty);
			String resourceSignature  = resource.getResourceSignature();
			
			// check access rights for this resource
			authenticator.checkResourceAccess(request, resourceSignature, propertyView.get(securityContext));
			
			// do action
			RestMethodResult result = resource.doHead();

			// commit response
			result.commitResponse(gson.get(), response);

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in HEAD", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in HEAD: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in HEAD", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in HEAD: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in HEAD", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in HEAD: " + t.getMessage()));
			
		} finally {

			try {
				response.getWriter().flush();
				response.getWriter().close();
				
			} catch (Throwable t) {
				
				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}

			securityContext.cleanUp();
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="OPTIONS">
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		SecurityContext securityContext = null;

		try {

			Authenticator authenticator     = getAuthenticator();
			securityContext = authenticator.initializeAndExamineRequest(request, response);

//			logRequest("OPTIONS", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			List<Resource> chain      = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
			Resource resource         = ResourceHelper.optimizeNestedResourceChain(chain, defaultIdProperty);
			String resourceSignature  = resource.getResourceSignature();
			
			// check access rights for this resource
			authenticator.checkResourceAccess(request, resourceSignature, propertyView.get(securityContext));
			
			// do action
			RestMethodResult result = resource.doOptions();

			// commit response
			result.commitResponse(gson.get(), response);

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();
			
		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in OPTIONS", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in OPTIONS: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in OPTIONS", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in OPTIONS: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in OPTIONS", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in OPTIONS: " + t.getMessage()));
			
		} finally {

			try {
				response.getWriter().flush();
				response.getWriter().close();
				
			} catch (Throwable t) {
				
				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}

			securityContext.cleanUp();
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="POST">
	@Override
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		SecurityContext securityContext = null;

		try {

			Authenticator authenticator     = getAuthenticator();
			securityContext = authenticator.initializeAndExamineRequest(request, response);

//			logRequest("POST", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			final JsonInput propertySet   = gson.get().fromJson(request.getReader(), JsonInput.class);

			if (securityContext != null) {

				// evaluate constraint chain
				List<Resource> chain            = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
				Resource resource               = ResourceHelper.optimizeNestedResourceChain(chain, defaultIdProperty);
				Map<String, Object> properties  = convertPropertySetToMap(propertySet);
				String resourceSignature        = resource.getResourceSignature();

				// check access rights for this resource
				authenticator.checkResourceAccess(request, resourceSignature, propertyView.get(securityContext));
				
				// do action
				RestMethodResult result = resource.doPost(properties);

				// set default value for property view
				propertyView.set(securityContext, defaultPropertyView);

				// commit response
				result.commitResponse(gson.get(), response);
				
			} else {

				RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

				result.commitResponse(gson.get(), response);

			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in POST", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in POST: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in POST", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonParseException in POST: " + jpex.getMessage()));

		} catch (UnsupportedOperationException uoe) {

			logger.log(Level.WARNING, "POST not supported", uoe);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "POST not supported: " + uoe.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in POST", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in POST: " + t.getMessage()));
			
		} finally {

			try {
				response.getWriter().flush();
				response.getWriter().close();
				
			} catch (Throwable t) {
				
				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}

			securityContext.cleanUp();
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="PUT">
	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		SecurityContext securityContext = null;

		try {

			Authenticator authenticator     = getAuthenticator();
			securityContext = authenticator.initializeAndExamineRequest(request, response);

//			logRequest("PUT", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			final JsonInput propertySet = gson.get().fromJson(request.getReader(), JsonInput.class);

			if (securityContext != null) {

				// evaluate constraint chain
				List<Resource> chain	       = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
				Resource resource	       = ResourceHelper.optimizeNestedResourceChain(chain, defaultIdProperty);
				String resourceSignature       = resource.getResourceSignature();
				Map<String, Object> properties = convertPropertySetToMap(propertySet);

				// check access rights for this resource
				authenticator.checkResourceAccess(request, resourceSignature, propertyView.get(securityContext));
				
				// do action
				RestMethodResult result = resource.doPut(properties);

				result.commitResponse(gson.get(), response);
				
			} else {

				RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

				result.commitResponse(gson.get(), response);

			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.get().toJson(frameworkException, response.getWriter());
			response.getWriter().println();
			
		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in PUT", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in PUT: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in PUT", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in PUT: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in PUT", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in PUT: " + t.getMessage()));
			
		} finally {

			try {
				response.getWriter().flush();
				response.getWriter().close();
				
			} catch (Throwable t) {
				
				logger.log(Level.WARNING, "Unable to flush and close response: {0}", t.getMessage());
			}

			securityContext.cleanUp();
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="TRACE">
	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

//		logRequest("TRACE", request);
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		int code = HttpServletResponse.SC_METHOD_NOT_ALLOWED;

		response.setStatus(code);
		response.getWriter().append(jsonError(code, "TRACE method not allowed"));
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="private methods">

	private String jsonError(final int code, final String message) {

		StringBuilder buf = new StringBuilder(100);

		buf.append("{\n");
		buf.append("  \"code\" : ").append(code);

		if (message != null) {

			buf.append(",\n  \"error\" : \"").append(StringUtils.replace(message, "\"", "\\\"")).append("\"\n");

		} else {

			buf.append("\n");

		}

		buf.append("}\n");

		return buf.toString();
	}

	private Map<String, Object> convertPropertySetToMap(JsonInput propertySet) {
		
		if (propertySet != null) {
			return propertySet.getAttributes();
		}
		
		return new LinkedHashMap<>();
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="nested classes">
	private class ThreadLocalPropertyView extends ThreadLocal<String> implements Value<String> {

		@Override
		protected String initialValue() {
			return defaultPropertyView;
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

	private class ThreadLocalGson extends ThreadLocal<Gson> {
		
		private int outputNestingDepth = 3;
		
		public ThreadLocalGson(final int outputNestingDepth) {
			this.outputNestingDepth = outputNestingDepth;
		}
		
		@Override
		protected Gson initialValue() {
			
			JsonInputGSONAdapter jsonInputAdapter = new JsonInputGSONAdapter(propertyView, defaultIdProperty);
			ResultGSONAdapter resultGsonAdapter   = new ResultGSONAdapter(propertyView, defaultIdProperty, outputNestingDepth);

			// create GSON serializer
			return new GsonBuilder()
				.setPrettyPrinting()
				.serializeNulls()
				.registerTypeHierarchyAdapter(FrameworkException.class, new FrameworkExceptionGSONAdapter())
				.registerTypeAdapter(JsonInput.class, jsonInputAdapter)
				.registerTypeAdapter(Result.class, resultGsonAdapter)
				.create();
		}
	}

	private class ThreadLocalJsonWriter extends ThreadLocal<StreamingWriter> {
		
		private Value<String> propertyView;
		private boolean indent = false;
		private int depth      = 3;
		
		public ThreadLocalJsonWriter(Value<String> propertyView, boolean indent, final int outputNestingDepth) {
			
			this.propertyView = propertyView;
			this.indent       = indent;
			this.depth        = outputNestingDepth;
		}
		
		@Override
		protected StreamingWriter initialValue() {
			
			return new StreamingJsonWriter(this.propertyView, indent, depth);
		}

	}

	private class ThreadLocalHtmlWriter extends ThreadLocal<StreamingHtmlWriter> {
		
		private Value<String> propertyView;
		private boolean indent = false;
		private int depth      = 3;
		
		public ThreadLocalHtmlWriter(Value<String> propertyView, boolean indent, final int outputNestingDepth) {
			
			this.propertyView = propertyView;
			this.indent       = indent;
			this.depth        = outputNestingDepth;
		}
		
		@Override
		protected StreamingHtmlWriter initialValue() {
			
			return new StreamingHtmlWriter(this.propertyView, indent, depth);
		}

	}
	// </editor-fold>
}
