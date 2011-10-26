/*
 *  Copyright (C) 2011 Axel Morgner
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
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.structr.common.AccessMode;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.Value;
import org.structr.core.node.NodeAttribute;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.PathException;
import org.structr.rest.ResourceConstraintProvider;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.adapter.PropertySetGSONAdapter;
import org.structr.rest.adapter.ResultGSONAdapter;
import org.structr.rest.constraint.PagingConstraint;
import org.structr.rest.constraint.RelationshipFollowingConstraint;
import org.structr.rest.constraint.ResourceConstraint;
import org.structr.rest.constraint.Result;
import org.structr.rest.constraint.SearchConstraint;
import org.structr.rest.constraint.SortConstraint;
import org.structr.rest.exception.MessageException;
import org.structr.rest.wrapper.PropertySet;
import org.structr.rest.wrapper.PropertySet.PropertyFormat;

/**
 * Implements the structr REST API. The input and output format of the JSON
 * serialisation can be configured with the servlet init parameter "PropertyFormat".
 * ({@see PropertyFormat}).
 *
 * @author Christian Morgner
 */
public class JsonRestServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger(JsonRestServlet.class.getName());

	public static final String			REQUEST_PARAMETER_SEARCH_STRING =		"q";
	public static final String			REQUEST_PARAMETER_SORT_KEY =			"sort";
	public static final String			REQUEST_PARAMETER_SORT_ORDER =			"order";
	public static final String			REQUEST_PARAMETER_PAGE_NUMBER =			"page";
	public static final String			REQUEST_PARAMETER_PAGE_SIZE =			"pageSize";

	public static final int				DEFAULT_VALUE_PAGE_SIZE =			20;
	public static final String			DEFAULT_VALUE_SORT_ORDER =			"asc";

	private static final String			SERVLET_PARAMETER_PROPERTY_FORMAT =		"PropertyFormat";
	private static final String			SERVLET_PARAMETER_CONSTRAINT_PROVIDER =		"ConstraintProvider";
	private static final String			SERVLET_PARAMETER_MODIFICATION_LISTENER =	"ModificationListener";

	private List<VetoableGraphObjectListener>	graphObjectListeners =				null;
	private PropertySetGSONAdapter			propertySetAdapter =				null;
	private Map<Pattern, Class>			constraintMap =					null;
	private Value<PropertyView>			propertyView =					null;
	private ResultGSONAdapter			resultGsonAdapter =				null;
	private Gson					gson =						null;

	@Override
	public void init() {

		// init parameters
		PropertyFormat propertyFormat =		initializePropertyFormat();

		// initialize variables
		this.graphObjectListeners =		new LinkedList<VetoableGraphObjectListener>();
		this.constraintMap =			new LinkedHashMap<Pattern, Class>();
		this.propertyView =			new ThreadLocalPropertyView();

		// initialize adapters
		this.resultGsonAdapter =		new ResultGSONAdapter(propertyFormat, propertyView);
		this.propertySetAdapter =		new PropertySetGSONAdapter(propertyFormat);

		// create GSON serializer
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.serializeNulls()
			.registerTypeAdapter(PropertySet.class,	propertySetAdapter)
			.registerTypeAdapter(Result.class,	resultGsonAdapter)
			.create();

		// external resource constraint initialization
		String externalProviderName = this.getInitParameter(SERVLET_PARAMETER_CONSTRAINT_PROVIDER);
		if(externalProviderName != null) {

			String[] parts = externalProviderName.split("[, ]+");
			for(String part : parts) {

				try {

					logger.log(Level.INFO, "Injecting constraints from provider {0}", part);
					
					Class providerClass = Class.forName(part);
					ResourceConstraintProvider provider = (ResourceConstraintProvider)providerClass.newInstance();

					// inject constraints
					constraintMap.putAll(provider.getConstraints());

				} catch(Throwable t) {
					logger.log(Level.WARNING, "Unable to inject external resource constraints", t);
				}
			}
		}

		// modification listener intializiation
		String externalListenerName = this.getInitParameter(SERVLET_PARAMETER_MODIFICATION_LISTENER);
		if(externalListenerName != null) {

			String[] parts = externalListenerName.split("[, ]+");
			for(String part : parts) {

				try {

					logger.log(Level.INFO, "Injecting listener {0}", part);

					Class listenerClass = Class.forName(part);
					VetoableGraphObjectListener listener = (VetoableGraphObjectListener)listenerClass.newInstance();

					graphObjectListeners.add(listener);


				} catch(Throwable t) {
					logger.log(Level.WARNING, "Unable to instantiate listener", t);
				}
			}
		}
	}

	@Override
	public void destroy() {
	}

	// <editor-fold defaultstate="collapsed" desc="DELETE">
	@Override
	protected void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			SecurityContext securityContext = getSecurityContext(request);

			// evaluate constraint chain
			List<ResourceConstraint> chain = parsePath(securityContext, request);
			ResourceConstraint resourceConstraint = optimizeConstraintChain(chain);

			// do action
			RestMethodResult result = resourceConstraint.doDelete(graphObjectListeners);
			result.commitResponse(response);


		} catch(IllegalArgumentException illegalArgumentException) {

			handleValidationError(illegalArgumentException, response);
			
		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="GET">
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			SecurityContext securityContext = getSecurityContext(request);

			// set default value for property view
			propertyView.set(PropertyView.Public);

			// evaluate constraints and measure query time
			double queryTimeStart = System.nanoTime();
			List<ResourceConstraint> chain = parsePath(securityContext, request);
			ResourceConstraint resourceConstraint = optimizeConstraintChain(chain);
			double queryTimeEnd = System.nanoTime();

			// create result set
			Result result = new Result(resourceConstraint.doGet(graphObjectListeners));
			if(result != null) {

				DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
				result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

				response.setContentType("application/json; charset=utf-8");
				Writer writer = response.getWriter();

				gson.toJson(result, writer);
				
				writer.flush();
				writer.close();

				response.setStatus(HttpServletResponse.SC_OK);

			} else {

				logger.log(Level.WARNING, "Result was null!");

				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			}

		} catch(MessageException msgException) {
			
			response.setStatus(msgException.getStatus());
			response.setContentLength(msgException.getMessage().length());
			response.getWriter().append(msgException.getMessage());
			response.getWriter().flush();
			response.getWriter().close();

		} catch(IllegalArgumentException illegalArgumentException) {

			handleValidationError(illegalArgumentException, response);

		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			logger.log(Level.WARNING, "Exception in GET", t);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="HEAD">
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

			SecurityContext securityContext = getSecurityContext(request);

			List<ResourceConstraint> chain = parsePath(securityContext, request);
			ResourceConstraint resourceConstraint = optimizeConstraintChain(chain);

			RestMethodResult result = resourceConstraint.doHead();
			result.commitResponse(response);


		} catch(IllegalArgumentException illegalArgumentException) {

			handleValidationError(illegalArgumentException, response);

		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			logger.log(Level.WARNING, "Exception in HEAD", t);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="OPTIONS">
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

			SecurityContext securityContext = getSecurityContext(request);

			List<ResourceConstraint> chain = parsePath(securityContext, request);
			ResourceConstraint resourceConstraint = optimizeConstraintChain(chain);

			RestMethodResult result = resourceConstraint.doOptions();
			result.commitResponse(response);


		} catch(IllegalArgumentException illegalArgumentException) {

			handleValidationError(illegalArgumentException, response);

		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			logger.log(Level.WARNING, "Exception in OPTIONS", t);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="POST">
	@Override
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

			final PropertySet propertySet = gson.fromJson(request.getReader(), PropertySet.class);
			SecurityContext securityContext = getSecurityContext(request);

			// evaluate constraint chain
			List<ResourceConstraint> chain = parsePath(securityContext, request);
			ResourceConstraint resourceConstraint = optimizeConstraintChain(chain);

			// create Map with properties
			Map<String, Object> properties = new LinkedHashMap<String, Object>();
			for(NodeAttribute attr : propertySet.getAttributes()) {
				properties.put(attr.getKey(), attr.getValue());
			}

			// do action
			RestMethodResult result = resourceConstraint.doPost(properties, graphObjectListeners);
			result.commitResponse(response);

			
		} catch(IllegalArgumentException illegalArgumentException) {

			handleValidationError(illegalArgumentException, response);

		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			logger.log(Level.WARNING, "Exception in POST", t);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="PUT">
	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			final PropertySet propertySet = gson.fromJson(request.getReader(), PropertySet.class);
			SecurityContext securityContext = getSecurityContext(request);

			// evaluate constraint chain
			List<ResourceConstraint> chain = parsePath(securityContext, request);
			ResourceConstraint resourceConstraint = optimizeConstraintChain(chain);

			// create Map with properties
			Map<String, Object> properties = new LinkedHashMap<String, Object>();
			for(NodeAttribute attr : propertySet.getAttributes()) {
				properties.put(attr.getKey(), attr.getValue());
			}

			// do action
			RestMethodResult result = resourceConstraint.doPut(properties, graphObjectListeners);
			result.commitResponse(response);


		} catch(IllegalArgumentException illegalArgumentException) {

			handleValidationError(illegalArgumentException, response);

		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			logger.log(Level.WARNING, "Exception in PUT", t);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="TRACE">
	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private List<ResourceConstraint> parsePath(SecurityContext securityContext, HttpServletRequest request) throws PathException {

		String path  = request.getPathInfo();

		// 1.: split request path into URI parts
		String[] pathParts = path.split("[/]+");

		// 2.: create container for resource constraints
		List<ResourceConstraint> constraintChain = new ArrayList<ResourceConstraint>(pathParts.length);

		// 3.: try to assign resource constraints for each URI part
		for(int i=0; i<pathParts.length; i++) {

			// eliminate empty strings
			String part = pathParts[i].trim();
			if(part.length() > 0) {

				// look for matching pattern
				for(Entry<Pattern, Class> entry : constraintMap.entrySet()) {

					Pattern pattern = entry.getKey();
					Matcher matcher = pattern.matcher(pathParts[i]);

					if(matcher.matches()) {

						try {
							Class type = entry.getValue();

							// instantiate resource constraint
							ResourceConstraint constraint = (ResourceConstraint)type.newInstance();
							if(constraint.checkAndConfigure(part, securityContext, request)) {

								logger.log(Level.INFO, "{0} matched, adding constraint of type {1} for part {2}", new Object[] {
									matcher.pattern(),
									type.getName(),
									part
								});

								// allow constraint to modify context
								constraint.configurePropertyView(propertyView);

								// add constraint and go on
								constraintChain.add(constraint);

								// first match wins, so choose priority wisely ;)
								break;
							}

						} catch(Throwable t) {

							logger.log(Level.WARNING, "Error instantiating constraint class", t);
						}

					}
				}
			}
		}

		// search
		String searchString = request.getParameter(REQUEST_PARAMETER_SEARCH_STRING);
		if(searchString != null) {
			constraintChain.add(new SearchConstraint(securityContext, searchString));
		}

		// sorting
		String sortKey = request.getParameter(REQUEST_PARAMETER_SORT_KEY);
		if(sortKey != null) {

			String sortOrder = request.getParameter(REQUEST_PARAMETER_SORT_ORDER);
			if(sortOrder == null) {
				sortOrder = DEFAULT_VALUE_SORT_ORDER;
			}

			constraintChain.add(new SortConstraint(securityContext, sortKey, sortOrder));
		}

		// paging
		String pageSizeParameter = request.getParameter(REQUEST_PARAMETER_PAGE_SIZE);
		if(pageSizeParameter != null) {

			String pageParameter = request.getParameter(REQUEST_PARAMETER_PAGE_NUMBER);
			int pageSize = parseInt(pageSizeParameter, DEFAULT_VALUE_PAGE_SIZE);
			int page = parseInt(pageParameter, 1);

			if(pageSize <= 0) {
				throw new IllegalPathException();
			}

			constraintChain.add(new PagingConstraint(securityContext, page, pageSize));
		}

		return constraintChain;
	}

	private ResourceConstraint optimizeConstraintChain(List<ResourceConstraint> constraintChain) throws PathException {

		int num = constraintChain.size();
		boolean found = false;
		int iterations = 0;

		do {
			
			StringBuilder chain = new StringBuilder();
			for(ResourceConstraint constr : constraintChain) {
				chain.append(constr.getClass().getSimpleName());
				chain.append(", ");
			}
			logger.log(Level.INFO, "########## Constraint chain after iteration {0}: {1}", new Object[] { iterations, chain.toString() } );

			found = false;
			for(int i=0; i<num; i++) {

				try {
					ResourceConstraint firstElement = constraintChain.get(i);
					ResourceConstraint secondElement = constraintChain.get(i+1);

					ResourceConstraint combinedConstraint = firstElement.tryCombineWith(secondElement);
					if(combinedConstraint != null) {

						logger.log(Level.INFO, "Combined constraint {0}", combinedConstraint.getClass().getSimpleName());

						// remove source constraints
						constraintChain.remove(firstElement);
						constraintChain.remove(secondElement);

						// add combined constraint
						constraintChain.add(i, combinedConstraint);

						// signal success
						found = true;

						// 
						if(combinedConstraint instanceof RelationshipFollowingConstraint) {
							break;
						}
					}

				} catch(PathException p) {

					// re-throw any PathException on the way
					throw p;
					
				} catch(Throwable t) {

					// logger.log(Level.WARNING, "Exception while combining constraints", t);
				}
			}

			iterations++;

		} while(found);

		StringBuilder chain = new StringBuilder();
		for(ResourceConstraint constr : constraintChain) {
			chain.append(constr.getClass().getSimpleName());
			chain.append(", ");
		}
		logger.log(Level.INFO, "########## Final constraint chain {0}", chain.toString() );

		if(constraintChain.size() == 1) {
			return constraintChain.get(0);
		}

		throw new IllegalPathException();
	}

	private PropertyFormat initializePropertyFormat() {

		// ----- set property format from init parameters -----
		String propertyFormatParameter = this.getInitParameter(SERVLET_PARAMETER_PROPERTY_FORMAT);
		PropertyFormat propertyFormat = PropertyFormat.NestedKeyValueType;

		if(propertyFormatParameter != null) {

			try {
				propertyFormat = PropertyFormat.valueOf(propertyFormatParameter);
				logger.log(Level.INFO, "Setting property format to {0}", propertyFormatParameter);

			} catch(Throwable t) {

				logger.log(Level.WARNING, "Cannot use property format {0}, unknown format.", propertyFormatParameter);
			}
		}

		return propertyFormat;
	}

	private void handleValidationError(IllegalArgumentException illegalArgumentException, HttpServletResponse response) {

		// illegal state exception, return error
		StringBuilder errorBuffer = new StringBuilder(100);
		errorBuffer.append(illegalArgumentException.getMessage());

		// send response
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setContentLength(errorBuffer.length());

		try {
			response.getWriter().append(errorBuffer.toString());
			response.getWriter().flush();
			response.getWriter().close();

		} catch(Throwable t) {

			logger.log(Level.WARNING, "Unable to commit response", t);
		}
	}

	private SecurityContext getSecurityContext(HttpServletRequest request) {

		// return SecurityContext.getSuperUserInstance();
		return SecurityContext.getInstance(this.getServletConfig(), request, AccessMode.Backend);
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

		if(value == null) {
			return defaultValue;
		}

		try {

			return Integer.parseInt(value);

		} catch(Throwable ignore) {
		}

		return defaultValue;
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="nested classes">
	private class ThreadLocalPropertyView extends ThreadLocal<PropertyView> implements Value<PropertyView> {

		@Override
		protected PropertyView initialValue() {
			return PropertyView.Public;
		}
	}
	// </editor-fold>
}




















































	/*
	private ResourceConstraint parsePath(String path, ResourceConstraint rootConstraint) {

		ResourceConstraint currentConstraint = rootConstraint;

		// split request path into URI parts
		String[] pathParts = path.split("[/]+");
		for(int i=0; i<pathParts.length; i++) {

			// eliminate empty strings
			String part = pathParts[i].trim();
			if(part.length() > 0) {

				// look for matching pattern
				for(Entry<Pattern, Class> entry : constraintMap.entrySet()) {

					Pattern pattern = entry.getKey();
					Matcher matcher = pattern.matcher(pathParts[i]);

					if(matcher.matches()) {

						try {
							Class type = entry.getValue();

							// instantiate resource constraint
							ResourceConstraint constraint = (ResourceConstraint)type.newInstance();
							if(constraint.checkAndConfigure(part)) {

								logger.log(Level.INFOST, "{0} matched, adding constraint of type {1} for part {2}", new Object[] {
									matcher.pattern(),
									type.getName(),
									part
								});

								// nest constraint and go on
								currentConstraint.setChild(constraint);
								currentConstraint = constraint;

								// first match wins, so choose priority wisely ;)
								break;
							}

						} catch(Throwable t) {

							logger.log(Level.WARNING, "Error instantiating constraint class", t);
						}
					}
				}
			}
		}

		return currentConstraint;
	}
	*/
