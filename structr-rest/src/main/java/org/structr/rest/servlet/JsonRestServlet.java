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


package org.structr.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang.StringUtils;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.rest.ResourceProvider;
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.property.PropertyKey;
import org.structr.core.*;
import org.structr.core.Value;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.AuthenticatorCommand;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationshipMapping;
import org.structr.core.graph.NodeFactory;
import org.structr.core.property.Property;
import org.structr.rest.StreamingJsonWriter;
import org.structr.rest.adapter.FrameworkExceptionGSONAdapter;
import org.structr.rest.adapter.ResultGSONAdapter;
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
	private Property<String> defaultIdProperty                  = AbstractNode.uuid;
	private String defaultPropertyView                          = PropertyView.Public;
	private ThreadLocalGson gson                                = null;
	private ThreadLocalJsonWriter jsonWriter                    = null;
	private Writer logWriter                                    = null;
	private Value<String> propertyView                          = null;
	private ResourceProvider resourceProvider                   = null;

	public JsonRestServlet(final ResourceProvider resourceProvider, final String defaultPropertyView, final PropertyKey<String> idProperty) {
		
		this.resourceProvider    = resourceProvider;
		this.defaultPropertyView = defaultPropertyView;
		
		// CHM (2013-04-21): id property will be ignored from now on..
		// this.defaultIdProperty   = idProperty;
	}
	
	@Override
	public void init() {
		
		// initialize internal resources with exact matching from EntityContext
		for (RelationshipMapping relMapping : EntityContext.getNamedRelations()) {
			resourceMap.put(Pattern.compile(relMapping.getName()), NamedRelationResource.class);
		}

		boolean indentJson = true;
		
		try {
			indentJson = Boolean.parseBoolean(Services.getConfigurationValue(Services.JSON_INDENTATION, "true"));
		
		} catch (Throwable t) {
			
			logger.log(Level.WARNING, "Unable to parse value for {0}: {1}", new Object[] { Services.JSON_INDENTATION, t.getMessage() } );
		}
		
		
		// inject resources
		resourceMap.putAll(resourceProvider.getResources());

		// initialize variables
		this.propertyView  = new ThreadLocalPropertyView();
		this.gson          = new ThreadLocalGson();
		this.jsonWriter    = new ThreadLocalJsonWriter(propertyView, indentJson);

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

			Authenticator authenticator     = getAuthenticator();
			securityContext = authenticator.initializeAndExamineRequest(request, response);

//			logRequest("GET", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

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
			PropertyKey sortKey      = null;

			// set sort key
			if (sortKeyName != null) {
				
				Class<? extends GraphObject> type = resource.getEntityClass();
				sortKey = EntityContext.getPropertyKeyForDatabaseName(type, sortKeyName);
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
			jsonWriter.get().stream(writer, result);

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
		
		return new LinkedHashMap<String, Object>();
	}

	private Authenticator getAuthenticator() throws FrameworkException {
		
		return (Authenticator) Services.command(null, AuthenticatorCommand.class).execute(getServletConfig());
		
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
		
		@Override
		protected Gson initialValue() {
			
			JsonInputGSONAdapter jsonInputAdapter = new JsonInputGSONAdapter(propertyView, defaultIdProperty);
			ResultGSONAdapter resultGsonAdapter   = new ResultGSONAdapter(propertyView, defaultIdProperty);

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

	private class ThreadLocalJsonWriter extends ThreadLocal<StreamingJsonWriter> {
		
		private Value<String> propertyView;
		private boolean indent = false;
		
		public ThreadLocalJsonWriter(Value<String> propertyView, boolean indent) {
			
			this.propertyView = propertyView;
			this.indent       = indent;
		}
		
		@Override
		protected StreamingJsonWriter initialValue() {
			
			return new StreamingJsonWriter(this.propertyView, indent);
		}

	}
	// </editor-fold>
}
