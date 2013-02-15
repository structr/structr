/*
 *  Copyright (C) 2010-2013 Axel Morgner
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



package org.structr.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang.StringUtils;

import org.structr.common.AccessMode;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.core.JsonInputGSONAdapter;
import org.structr.core.Value;
import org.structr.rest.ResourceProvider;
import org.structr.rest.RestMethodResult;
import org.structr.rest.adapter.FrameworkExceptionGSONAdapter;
import org.structr.rest.adapter.ResultGSONAdapter;
import org.structr.rest.resource.PagingHelper;
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.property.PropertyKey;
import org.structr.core.*;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationshipMapping;
import org.structr.core.graph.NodeFactory;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.resource.*;

//~--- classes ----------------------------------------------------------------

/**
 * Implements the structr REST API. The input and output format of the JSON
 * serialisation can be configured with the servlet init parameter "PropertyFormat".
 * ({@see PropertyFormat}).
 *
 * @author Christian Morgner
 */
public class JsonRestServlet extends HttpServlet {

	public static final int DEFAULT_VALUE_PAGE_SIZE                     = 20;
	public static final String DEFAULT_VALUE_SORT_ORDER                 = "asc";
	public static final String REQUEST_PARAMETER_LOOSE_SEARCH           = "loose";
	public static final String REQUEST_PARAMETER_PAGE_NUMBER            = "page";
	public static final String REQUEST_PARAMETER_PAGE_SIZE              = "pageSize";
	public static final String REQUEST_PARAMETER_OFFSET_ID              = "pageStartId";
	public static final String REQUEST_PARAMETER_SORT_KEY               = "sort";
	public static final String REQUEST_PARAMETER_SORT_ORDER             = "order";
	private static final Logger logger                                  = Logger.getLogger(JsonRestServlet.class.getName());

	//~--- fields ---------------------------------------------------------

	private Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<Pattern, Class<? extends Resource>>();
	private PropertyKey defaultIdProperty                       = AbstractNode.uuid;
	private String defaultPropertyView                          = PropertyView.Public;
	private Gson gson                                           = null;
	private Writer logWriter                                    = null;
	private JsonInputGSONAdapter jsonInputAdapter               = null;
	private Value<String> propertyView                          = null;
	private ResultGSONAdapter resultGsonAdapter                 = null;
	private ResourceProvider resourceProvider                   = null;

	public JsonRestServlet(final ResourceProvider resourceProvider, final String defaultPropertyView, final PropertyKey<String> idProperty) {
		
		this.resourceProvider    = resourceProvider;
		this.defaultPropertyView = defaultPropertyView;
		this.defaultIdProperty   = idProperty;
	}
	
	@Override
	public void init() {
		
		// initialize internal resources with exact matching from EntityContext
		for(RelationshipMapping relMapping : EntityContext.getNamedRelations()) {
			resourceMap.put(Pattern.compile(relMapping.getName()), NamedRelationResource.class);
		}

		// inject resources
		resourceMap.putAll(resourceProvider.getResources());

		// initialize variables
		this.propertyView  = new ThreadLocalPropertyView();

		// initialize adapters
		this.resultGsonAdapter  = new ResultGSONAdapter(propertyView, defaultIdProperty);
		this.jsonInputAdapter = new JsonInputGSONAdapter(propertyView, defaultIdProperty);

		// create GSON serializer
		this.gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .serializeNulls()
                        .registerTypeHierarchyAdapter(FrameworkException.class, new FrameworkExceptionGSONAdapter())
                        .registerTypeAdapter(JsonInput.class, jsonInputAdapter)
                        .registerTypeAdapter(Result.class, resultGsonAdapter)
                        .create();
		
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

		SecurityContext securityContext = getSecurityContext(request, response);
		
		try {

//			logRequest("DELETE", request);
			request.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// let module-specific authenticator examine the request first
			securityContext.initializeAndExamineRequest(request, response);

			List<Resource> chain            = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
			Resource resourceConstraint     = ResourceHelper.optimizeConstraintChain(chain, defaultIdProperty);

			// let authenticator examine request again
			securityContext.examineRequest(request, resourceConstraint.getResourceSignature(), resourceConstraint.getGrant(), propertyView.get(securityContext));

			// do action
			RestMethodResult result = resourceConstraint.doDelete();

			// commit response
			result.commitResponse(gson, response);

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.toJson(frameworkException, response.getWriter());
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

		SecurityContext securityContext = getSecurityContext(request, response);

		try {

//			logRequest("GET", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// let module-specific authenticator examine the request first
			securityContext.initializeAndExamineRequest(request, response);

			// set default value for property view
			propertyView.set(securityContext, defaultPropertyView);

			// evaluate constraints and measure query time
			double queryTimeStart = System.nanoTime();
			Resource resource     = ResourceHelper.applyViewTransformation(request, securityContext, ResourceHelper.optimizeConstraintChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty), defaultIdProperty), propertyView);
			
			// let authenticator examine request again
			securityContext.examineRequest(request, resource.getResourceSignature(), resource.getGrant(), propertyView.get(securityContext));
			
			// add sorting & paging
			String pageSizeParameter = request.getParameter(REQUEST_PARAMETER_PAGE_SIZE);
			String pageParameter     = request.getParameter(REQUEST_PARAMETER_PAGE_NUMBER);
			String offsetId          = request.getParameter(REQUEST_PARAMETER_OFFSET_ID);
			String sortOrder         = request.getParameter(REQUEST_PARAMETER_SORT_ORDER);
			String sortKeyName       = request.getParameter(REQUEST_PARAMETER_SORT_KEY);
			boolean sortDescending   = (sortOrder != null && "desc".equals(sortOrder.toLowerCase()));
			int pageSize		 = parseInt(pageSizeParameter, NodeFactory.DEFAULT_PAGE_SIZE);
			int page                 = parseInt(pageParameter, NodeFactory.DEFAULT_PAGE);
			PropertyKey sortKey      = null;

			// set sort key
			if (sortKeyName != null) {
				
				Class<? extends GraphObject> type = resource.getEntityClass();
				sortKey = EntityContext.getPropertyKeyForDatabaseName(type, sortKeyName);
			}
			
			// do action
			Result result            = resource.doGet(sortKey, sortDescending, pageSize, page, offsetId);
			result.setIsCollection(resource.isCollectionResource());
			result.setIsPrimitiveArray(resource.isPrimitiveArray());
			
			//Integer rawResultCount = (Integer) Services.getAttribute(NodeFactory.RAW_RESULT_COUNT + Thread.currentThread().getId());
			
			PagingHelper.addPagingParameter(result, pageSize, page);
			//Services.removeAttribute(NodeFactory.RAW_RESULT_COUNT + Thread.currentThread().getId());

			// timing..
			double queryTimeEnd   = System.nanoTime();

			// commit response
			if (result != null) {

				// store property view that will be used to render the results
				result.setPropertyView(propertyView.get(securityContext));
				
				// allow resource to modify result set
				resource.postProcessResultSet(result);
				
				DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

				result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

				Writer writer = response.getWriter();

				gson.toJson(result, writer);
				response.setStatus(HttpServletResponse.SC_OK);
				writer.append("\n");    // useful newline

			} else {

				logger.log(Level.WARNING, "Result was null!");

				int code = HttpServletResponse.SC_NO_CONTENT;

				response.setStatus(code);
				Writer writer = response.getWriter();
				writer.append(jsonError(code, "Result was null!"));

			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in GET", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in GET: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in GET", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in GET: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in GET", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(jsonError(code, "JsonSyntaxException in GET: " + t.getMessage()));
			
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

		SecurityContext securityContext = getSecurityContext(request, response);
		
		try {

//			logRequest("HEAD", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			List<Resource> chain            = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
			Resource resourceConstraint     = ResourceHelper.optimizeConstraintChain(chain, defaultIdProperty);
			
			// let authenticator examine request again
			securityContext.examineRequest(request, resourceConstraint.getResourceSignature(), resourceConstraint.getGrant(), propertyView.get(securityContext));
			
			// do action
			RestMethodResult result = resourceConstraint.doHead();

			// commit response
			result.commitResponse(gson, response);

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.toJson(frameworkException, response.getWriter());
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

		SecurityContext securityContext = getSecurityContext(request, response);
		
		try {

//			logRequest("OPTIONS", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			List<Resource> chain            = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
			Resource resourceConstraint     = ResourceHelper.optimizeConstraintChain(chain, defaultIdProperty);
			
			// let authenticator examine request again
			securityContext.examineRequest(request, resourceConstraint.getResourceSignature(), resourceConstraint.getGrant(), propertyView.get(securityContext));
			
			// do action
			RestMethodResult result = resourceConstraint.doOptions();

			// commit response
			result.commitResponse(gson, response);

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.toJson(frameworkException, response.getWriter());
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

		SecurityContext securityContext = getSecurityContext(request, response);
		
		try {

//			logRequest("POST", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			final JsonInput propertySet   = gson.fromJson(request.getReader(), JsonInput.class);

			// let module-specific authenticator examine the request first
			securityContext.initializeAndExamineRequest(request, response);

			if (securityContext != null) {

				// evaluate constraint chain
				List<Resource> chain            = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
				Resource resourceConstraint     = ResourceHelper.optimizeConstraintChain(chain, defaultIdProperty);
				Map<String, Object> properties = convertPropertySetToMap(propertySet);

				// let authenticator examine request again
				securityContext.examineRequest(request, resourceConstraint.getResourceSignature(), resourceConstraint.getGrant(), propertyView.get(securityContext));
				
				// do action
				RestMethodResult result = resourceConstraint.doPost(properties);

				// set default value for property view
				propertyView.set(securityContext, defaultPropertyView);

				// commit response
				result.commitResponse(gson, response);
				
			} else {

				RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

				result.commitResponse(gson, response);

			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.toJson(frameworkException, response.getWriter());
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

		SecurityContext securityContext = getSecurityContext(request, response);
		
		try {

//			logRequest("PUT", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			final JsonInput propertySet   = gson.fromJson(request.getReader(), JsonInput.class);

			// let module-specific authenticator examine the request first
			securityContext.initializeAndExamineRequest(request, response);

			if (securityContext != null) {

				// evaluate constraint chain
				List<Resource> chain	       = ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty);
				Resource resource	       = ResourceHelper.optimizeConstraintChain(chain, defaultIdProperty);
				Map<String, Object> properties = convertPropertySetToMap(propertySet);

				securityContext.examineRequest(request, resource.getResourceSignature(), resource.getGrant(), propertyView.get(securityContext));
				
				// do action
				RestMethodResult result = resource.doPut(properties);

				result.commitResponse(gson, response);
			} else {

				RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

				result.commitResponse(gson, response);

			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.toJson(frameworkException, response.getWriter());
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

	/**
	 * Tries to parse the given String to an int value, returning
	 * defaultValue on error.
	 *
	 * @param value the source String to parse
	 * @param defaultValue the default value that will be returned when parsing fails
	 * @return the parsed value or the given default value when parsing fails
	 */
	private int parseInt(String value, int defaultValue) {

		if (value == null) {

			return defaultValue;

		}

		try {
			return Integer.parseInt(value);
		} catch (Throwable ignore) {}

		return defaultValue;
	}

	/**
	 * Tries to parse the given String to a long value, returning
	 * defaultValue on error.
	 *
	 * @param value the source String to parse
	 * @param defaultValue the default value that will be returned when parsing fails
	 * @return the parsed value or the given default value when parsing fails
	 */
	private long parseLong(String value, long defaultValue) {

		if (value == null) {

			return defaultValue;

		}

		try {
			return Long.parseLong(value);
		} catch (Throwable ignore) {}

		return defaultValue;
	}

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

	private String jsonMsg(final String message) {

		StringBuilder buf = new StringBuilder(100);

		buf.append("{\n");

		if (message != null) {

			buf.append("    \"message\" : \"").append(StringUtils.replace(message, "\"", "\\\"")).append("\"\n");

		} else {

			buf.append("    \"message\" : \"\"\n");

		}

		buf.append("}\n");

		return buf.toString();
	}

	private Map<String, Object> convertPropertySetToMap(JsonInput propertySet) {
		
		if (propertySet != null) {
			return propertySet.getAttributes();
		}
		
		return new LinkedHashMap<String, Object>();
	}

	private SecurityContext getSecurityContext(HttpServletRequest request, HttpServletResponse response) {

		return SecurityContext.getInstance(this.getServletConfig(), request, response, AccessMode.Backend);
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

	// </editor-fold>
}
