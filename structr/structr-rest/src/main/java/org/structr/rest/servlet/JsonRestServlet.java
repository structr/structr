/*
 *  Copyright (C) 2010-2012 Axel Morgner
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
import org.structr.core.PropertySet;
import org.structr.core.PropertySet.PropertyFormat;
import org.structr.core.PropertySetGSONAdapter;
import org.structr.core.Value;
import org.structr.core.node.NodeAttribute;
import org.structr.rest.ResourceProvider;
import org.structr.rest.RestMethodResult;
import org.structr.rest.adapter.FrameworkExceptionGSONAdapter;
import org.structr.rest.adapter.ResultGSONAdapter;
import org.structr.rest.resource.PagingHelper;
import org.structr.rest.resource.RelationshipFollowingResource;
import org.structr.rest.resource.Resource;
import org.structr.core.Result;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.Writer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.*;
import org.structr.core.entity.RelationshipMapping;
import org.structr.core.node.NodeFactory;
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
	private static final String SERVLET_PARAMETER_RESOURCE_PROVIDER     = "ResourceProvider";
	private static final String SERVLET_PARAMETER_DEFAULT_PROPERTY_VIEW = "DefaultPropertyView";
	private static final String SERVLET_PARAMETER_ID_PROPERTY           = "IdProperty";
	private static final String SERVLET_PARAMETER_PROPERTY_FORMAT       = "PropertyFormat";
//	private static final String SERVLET_PARAMETER_REQUEST_LOGGING       = "RequestLogging";
	private static final Logger logger                                  = Logger.getLogger(JsonRestServlet.class.getName());

	//~--- fields ---------------------------------------------------------

	private Map<Pattern, Class> resourceMap           = null;
	private String defaultIdProperty                  = null;
	private String defaultPropertyView                = PropertyView.Public;
	private Gson gson                                 = null;
	private Writer logWriter                          = null;
	private PropertySetGSONAdapter propertySetAdapter = null;
	private Value<String> propertyView                = null;
	private ResultGSONAdapter resultGsonAdapter       = null;

	//~--- methods --------------------------------------------------------

	@Override
	public void init() {

		// initialize variables
		this.resourceMap = new LinkedHashMap<Pattern, Class>();
		this.propertyView  = new ThreadLocalPropertyView();

		// initialize internal resources with exact matching from EntityContext
		for(RelationshipMapping relMapping : EntityContext.getNamedRelations()) {
			resourceMap.put(Pattern.compile(relMapping.getName()), NamedRelationResource.class);
		}

		// external resource constraint initialization
		String externalProviderName = this.getInitParameter(SERVLET_PARAMETER_RESOURCE_PROVIDER);

		if (externalProviderName != null) {

			String[] parts = externalProviderName.split("[, ]+");

			for (String part : parts) {

				try {

					logger.log(Level.INFO, "Injecting resources from provider {0}", part);

					Class providerClass       = Class.forName(part);
					ResourceProvider provider = (ResourceProvider) providerClass.newInstance();

					// inject constraints
					resourceMap.putAll(provider.getResources());

				} catch (Throwable t) {
					logger.log(Level.WARNING, "Unable to inject external resources", t);
				}

			}

		}

		// property view initialization
		String defaultPropertyViewName = this.getInitParameter(SERVLET_PARAMETER_DEFAULT_PROPERTY_VIEW);

		if (defaultPropertyViewName != null) {

			logger.log(Level.FINE, "Setting default property view to {0}", defaultPropertyViewName);

			this.defaultPropertyView = defaultPropertyViewName;

		}

		// primary key
		String defaultIdPropertyName = this.getInitParameter(SERVLET_PARAMETER_ID_PROPERTY);

		if (defaultIdPropertyName != null) {

			logger.log(Level.FINE, "Setting default id property to {0}", defaultIdPropertyName);

			this.defaultIdProperty = defaultIdPropertyName;

		}

		PropertyFormat propertyFormat = initializePropertyFormat();

		// initialize adapters
		this.resultGsonAdapter  = new ResultGSONAdapter(propertyFormat, propertyView, defaultIdProperty);
		this.propertySetAdapter = new PropertySetGSONAdapter(propertyFormat, propertyView, defaultIdProperty);

		// create GSON serializer
		this.gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .serializeNulls()
                        .registerTypeHierarchyAdapter(FrameworkException.class, new FrameworkExceptionGSONAdapter())
                        .registerTypeAdapter(PropertySet.class, propertySetAdapter)
                        .registerTypeAdapter(Result.class, resultGsonAdapter)
                        .create();

//		String requestLoggingParameter = this.getInitParameter(SERVLET_PARAMETER_REQUEST_LOGGING);
//
//		if ((requestLoggingParameter != null) && "true".equalsIgnoreCase(requestLoggingParameter)) {
//
//			// initialize access log
//			String logFileName = Services.getBasePath().concat("/logs/access.log");
//
//			try {
//
//				File logFile = new File(logFileName);
//
//				logFile.getParentFile().mkdir();
//
//				logWriter = new FileWriter(logFileName);
//
//			} catch (IOException ioex) {
//				logger.log(Level.WARNING, "Could not open access log file {0}: {1}", new Object[] { logFileName, ioex.getMessage() });
//			}
//		}
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

		try {

//			logRequest("DELETE", request);
			request.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			SecurityContext securityContext = getSecurityContext(request, response);
			List<Resource> chain        = parsePath(securityContext, request);
			Resource resourceConstraint = optimizeConstraintChain(chain);

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
			response.getWriter().flush();
			response.getWriter().close();
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
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="GET">
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

//			logRequest("GET", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			SecurityContext securityContext = getSecurityContext(request, response);

			// set default value for property view
			propertyView.set(securityContext, defaultPropertyView);

			// evaluate constraints and measure query time
			double queryTimeStart = System.nanoTime();
			Resource resource     = applyViewTransformation(request, securityContext, optimizeConstraintChain(parsePath(securityContext, request)));
			
			// let authenticator examine request again
			securityContext.examineRequest(request, resource.getResourceSignature(), resource.getGrant(), propertyView.get(securityContext));
			
			// add sorting & paging
			String pageSizeParameter = request.getParameter(REQUEST_PARAMETER_PAGE_SIZE);
			String pageParameter     = request.getParameter(REQUEST_PARAMETER_PAGE_NUMBER);
			String offsetId          = request.getParameter(REQUEST_PARAMETER_OFFSET_ID);
			String sortOrder         = request.getParameter(REQUEST_PARAMETER_SORT_ORDER);
			String sortKey           = request.getParameter(REQUEST_PARAMETER_SORT_KEY);
			boolean sortDescending   = (sortOrder != null && "desc".equals(sortOrder.toLowerCase()));
			int pageSize		 = parseInt(pageSizeParameter, NodeFactory.DEFAULT_PAGE_SIZE);
			int page                 = parseInt(pageParameter, NodeFactory.DEFAULT_PAGE);
			
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
				writer.flush();
				writer.close();

			} else {

				logger.log(Level.WARNING, "Result was null!");

				int code = HttpServletResponse.SC_NO_CONTENT;

				response.setStatus(code);
				Writer writer = response.getWriter();
				writer.append(jsonError(code, "Result was null!"));
				writer.flush();
				writer.close();

			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			gson.toJson(frameworkException, response.getWriter());
			response.getWriter().println();
			response.getWriter().flush();
			response.getWriter().close();
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
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="HEAD">
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

//			logRequest("HEAD", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			SecurityContext securityContext = getSecurityContext(request, response);
			List<Resource> chain            = parsePath(securityContext, request);
			Resource resourceConstraint     = optimizeConstraintChain(chain);
			
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
			response.getWriter().flush();
			response.getWriter().close();
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
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="OPTIONS">
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

//			logRequest("OPTIONS", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			SecurityContext securityContext = getSecurityContext(request, response);
			List<Resource> chain            = parsePath(securityContext, request);
			Resource resourceConstraint     = optimizeConstraintChain(chain);
			
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
			response.getWriter().flush();
			response.getWriter().close();
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
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="POST">
	@Override
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

//			logRequest("POST", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			final PropertySet propertySet   = gson.fromJson(request.getReader(), PropertySet.class);
			SecurityContext securityContext = getSecurityContext(request, response);

			if (securityContext != null) {

				// evaluate constraint chain
				List<Resource> chain           = parsePath(securityContext, request);
				Resource resourceConstraint    = optimizeConstraintChain(chain);
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
			response.getWriter().flush();
			response.getWriter().close();
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
		}
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="PUT">
	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

//			logRequest("PUT", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");

			final PropertySet propertySet   = gson.fromJson(request.getReader(), PropertySet.class);
			SecurityContext securityContext = getSecurityContext(request, response);

			if (securityContext != null) {

				// evaluate constraint chain
				List<Resource> chain        = parsePath(securityContext, request);
				Resource resourceConstraint = optimizeConstraintChain(chain);
				Map<String, Object> properties        = convertPropertySetToMap(propertySet);

				// do action
				RestMethodResult result = resourceConstraint.doPut(properties);

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
			response.getWriter().flush();
			response.getWriter().close();
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
	private List<Resource> parsePath(SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		String path = request.getPathInfo();

		// intercept empty path and send 204 No Content
		if (!StringUtils.isNotBlank(path)) {

			throw new NoResultsException();

		}

		// 1.: split request path into URI parts
		String[] pathParts = path.split("[/]+");

		// 2.: create container for resource constraints
		List<Resource> constraintChain = new ArrayList<Resource>(pathParts.length);

		// 3.: try to assign resource constraints for each URI part
		for (int i = 0; i < pathParts.length; i++) {

			// eliminate empty strings
			String part = pathParts[i].trim();

			if (part.length() > 0) {

				boolean found = false;

				// TEST: schema information
				if ("_schema".equals(part)) {
					
					SchemaResource resource = new SchemaResource();
					resource.checkAndConfigure("_schema", securityContext, request);
					constraintChain.add(resource);
					
					found = true;
					break;
					
				}
				
				// look for matching pattern
				for (Entry<Pattern, Class> entry : resourceMap.entrySet()) {

					Pattern pattern = entry.getKey();
					Matcher matcher = pattern.matcher(pathParts[i]);

					if (matcher.matches()) {

						Class type = entry.getValue();
						Resource resource = null;
						
						try {

							// instantiate resource constraint
							resource = (Resource) type.newInstance();

						} catch (Throwable t) {
							logger.log(Level.WARNING, "Error instantiating constraint class", t);
						}

						if(resource != null) {
							
							// set security context
							resource.setSecurityContext(securityContext);

							if (resource.checkAndConfigure(part, securityContext, request)) {

								logger.log(Level.FINE, "{0} matched, adding constraint of type {1} for part {2}", new Object[] { matcher.pattern(), type.getName(),
									part });

								// allow constraint to modify context
								resource.configurePropertyView(propertyView);
								resource.configureIdProperty(defaultIdProperty);

								// add constraint and go on
								constraintChain.add(resource);

								found = true;

								// first match wins, so choose priority wisely ;)
								break;

							}
						}

					}

				}

				if (!found) {

					throw new NotFoundException();

				}

			}
		}

		return constraintChain;
	}

	private Resource optimizeConstraintChain(List<Resource> constraintChain) throws FrameworkException {

		ViewFilterResource view = null;
		int num                 = constraintChain.size();
		boolean found           = false;
		int iterations          = 0;

		do {

			StringBuilder chain = new StringBuilder();

			for(Iterator<Resource> it = constraintChain.iterator(); it.hasNext();) {
				
				Resource constr = it.next();

				chain.append(constr.getClass().getSimpleName());
				chain.append(", ");
				
				if(constr instanceof ViewFilterResource) {
					view = (ViewFilterResource)constr;
					it.remove();
				}

			}

			logger.log(Level.FINE, "########## Constraint chain after iteration {0}: {1}", new Object[] { iterations, chain.toString() });

			found = false;

			try {
				
				for(int i=0; i<num; i++) {
					Resource firstElement       = constraintChain.get(i);
					Resource secondElement      = constraintChain.get(i + 1);
					Resource combinedConstraint = firstElement.tryCombineWith(secondElement);

					if (combinedConstraint != null) {

						logger.log(Level.FINE, "Combined constraint {0} and {1} to {2}", new Object[] { 
							firstElement.getClass().getSimpleName(), 
							secondElement.getClass().getSimpleName(),
							combinedConstraint.getClass().getSimpleName()
						} );

						// remove source constraints
						constraintChain.remove(firstElement);
						constraintChain.remove(secondElement);

						// add combined constraint
						constraintChain.add(i, combinedConstraint);

						// signal success
						found = true;

						if (combinedConstraint instanceof RelationshipFollowingResource) {

							break;

						}
					}
				}

			} catch(Throwable t) {
				// ignore exceptions thrown here
			}

			iterations++;

		} while (found);

		StringBuilder chain = new StringBuilder();

		for (Resource constr : constraintChain) {

			chain.append(constr.getClass().getSimpleName());
			chain.append(", ");

		}

		logger.log(Level.FINE, "Final constraint chain {0}", chain.toString());

		if (constraintChain.size() == 1) {

			Resource finalConstraint = constraintChain.get(0);
			if(view != null) {
				finalConstraint = finalConstraint.tryCombineWith(view);
			}
			
			// inform final constraint about the configured ID property
			finalConstraint.configureIdProperty(defaultIdProperty);
			
			return finalConstraint;

		}

		throw new IllegalPathException();
	}

	private PropertyFormat initializePropertyFormat() {

		// ----- set property format from init parameters -----
		String propertyFormatParameter = this.getInitParameter(SERVLET_PARAMETER_PROPERTY_FORMAT);
		PropertyFormat propertyFormat  = PropertyFormat.NestedKeyValueType;

		if (propertyFormatParameter != null) {

			try {

				propertyFormat = PropertyFormat.valueOf(propertyFormatParameter);

				logger.log(Level.FINE, "Setting property format to {0}", propertyFormatParameter);

			} catch (Throwable t) {
				logger.log(Level.WARNING, "Cannot use property format {0}, unknown format.", propertyFormatParameter);
			}

		}

		return propertyFormat;
	}

	private Resource applyViewTransformation(HttpServletRequest request, SecurityContext securityContext, Resource finalResource) throws FrameworkException {

		Resource transformedResource = finalResource;

		// add view transformation
		Class type = finalResource.getEntityClass();
		if(type != null) {
			
			ViewTransformation transformation = EntityContext.getViewTransformation(type, propertyView.get(securityContext));
			if(transformation != null) {
				transformedResource = transformedResource.tryCombineWith(new TransformationResource(securityContext, transformation));
			}
		}

		return transformedResource;
	}

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

	private Map<String, Object> convertPropertySetToMap(PropertySet propertySet) {

		Map<String, Object> properties        = new LinkedHashMap<String, Object>();

		// copy properties to map
		if(propertySet != null) {

			for (NodeAttribute attr : propertySet.getAttributes()) {
				String key = attr.getKey();
				Object val = attr.getValue();

				// store value
				properties.put(key, val);
			}
		}

		return properties;
	}

//	private void logRequest(String method, HttpServletRequest request) {
//
////
////              if(logWriter != null) {
////
////                      try {
////                              logWriter.append(accessLogDateFormat.format(System.currentTimeMillis()));
////                              logWriter.append(" ");
////                              logWriter.append(StringUtils.rightPad(method, 8));
////                              logWriter.append(request.getRequestURI());
////                              logWriter.append("\n");
////
////                              BufferedReader reader = request.getReader();
////                              if(reader.markSupported()) {
////                                      reader.mark(65535);
////                              }
////
////                              String line = reader.readLine();
////                              while(line != null) {
////                                      logWriter.append("        ");
////                                      logWriter.append(line);
////                                      line = reader.readLine();
////                                      logWriter.append("\n");
////                              }
////
////                              reader.reset();
////
////                              logWriter.flush();
////
////                      } catch(IOException ioex) {
////                              // ignore
////                      }
////              }
//	}

	// </editor-fold>

	//~--- get methods ----------------------------------------------------

	private SecurityContext getSecurityContext(HttpServletRequest request, HttpServletResponse response) throws FrameworkException {

		SecurityContext securityContext = SecurityContext.getInstance(this.getServletConfig(), request, response, AccessMode.Backend);
		
		// let module-specific authenticator examine the request first
		securityContext.initializeAndExamineRequest(request, response);
		
		return securityContext;
	}

	//~--- inner classes --------------------------------------------------

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
