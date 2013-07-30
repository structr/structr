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

import org.structr.common.AccessMode;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.JsonInput;
import org.structr.core.JsonInputGSONAdapter;
import org.structr.core.Result;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationshipMapping;
import org.structr.core.graph.NodeFactory;
import org.structr.core.property.PropertyKey;
import org.structr.rest.ResourceProvider;
import org.structr.rest.adapter.FrameworkExceptionGSONAdapter;
import org.structr.rest.adapter.ResultGSONAdapter;
import org.structr.rest.resource.NamedRelationResource;
import org.structr.common.PagingHelper;
import org.structr.rest.resource.Resource;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.Services;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.AuthenticatorCommand;
import org.structr.core.entity.ResourceAccess;

//~--- classes ----------------------------------------------------------------

/**
 * This servlet produces CSV (comma separated value) lists out of a search result
 *
 * @author Axel Morgner
 */
public class CsvServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger(CsvServlet.class.getName());

	//~--- fields ---------------------------------------------------------

	private Gson gson                                           = null;
	private JsonInputGSONAdapter jsonInputAdapter               = null;
	private Writer logWriter                                    = null;
	private Value<String> propertyView                          = null;
	private Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<Pattern, Class<? extends Resource>>();
	private String defaultPropertyView                          = PropertyView.Public;
	private PropertyKey defaultIdProperty                       = AbstractNode.uuid;
	private ResourceProvider resourceProvider                   = null;
	private ResultGSONAdapter resultGsonAdapter                 = null;

	//~--- constructors ---------------------------------------------------

	public CsvServlet(final ResourceProvider resourceProvider, final String defaultPropertyView, final PropertyKey<String> idProperty) {

		this.resourceProvider    = resourceProvider;
		this.defaultPropertyView = defaultPropertyView;
		this.defaultIdProperty   = idProperty;

	}

	//~--- methods --------------------------------------------------------

	@Override
	public void init() {

		// initialize internal resources with exact matching from EntityContext
		for (RelationshipMapping relMapping : EntityContext.getNamedRelations()) {

			resourceMap.put(Pattern.compile(relMapping.getName()), NamedRelationResource.class);
		}

		// inject resources
		resourceMap.putAll(resourceProvider.getResources());

		// initialize variables
		this.propertyView = new ThreadLocalPropertyView();

		// initialize adapters
		this.resultGsonAdapter = new ResultGSONAdapter(propertyView, defaultIdProperty);
		this.jsonInputAdapter  = new JsonInputGSONAdapter(propertyView, defaultIdProperty);

		// create GSON serializer
		this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().registerTypeHierarchyAdapter(FrameworkException.class,
			new FrameworkExceptionGSONAdapter()).registerTypeAdapter(JsonInput.class, jsonInputAdapter).registerTypeAdapter(Result.class, resultGsonAdapter).create();
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws UnsupportedEncodingException {

		SecurityContext securityContext = null;

		try {

			Authenticator authenticator     = getAuthenticator();
			securityContext = authenticator.initializeAndExamineRequest(request, response);

//                      logRequest("GET", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/csv; charset=utf-8");

			// set default value for property view
			propertyView.set(securityContext, defaultPropertyView);

			// evaluate constraints and measure query time
			double queryTimeStart = System.nanoTime();
			Resource resource     = ResourceHelper.applyViewTransformation(request, securityContext,
							ResourceHelper.optimizeNestedResourceChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, defaultIdProperty),
								defaultIdProperty), propertyView);
			String resourceSignature = resource.getResourceSignature();

			// let authenticator examine request again
			authenticator.checkResourceAccess(request, resourceSignature, propertyView.get(securityContext));

			// add sorting & paging
			String pageSizeParameter = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_PAGE_SIZE);
			String pageParameter     = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_PAGE_NUMBER);
			String offsetId          = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_OFFSET_ID);
			String sortOrder         = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_ORDER);
			String sortKeyName       = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_KEY);
			boolean sortDescending   = (sortOrder != null && "desc".equals(sortOrder.toLowerCase()));
			int pageSize             = parseInt(pageSizeParameter, NodeFactory.DEFAULT_PAGE_SIZE);
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

			// Integer rawResultCount = (Integer) Services.getAttribute(NodeFactory.RAW_RESULT_COUNT + Thread.currentThread().getId());
			PagingHelper.addPagingParameter(result, pageSize, page);

			// Services.removeAttribute(NodeFactory.RAW_RESULT_COUNT + Thread.currentThread().getId());
			// timing..
			double queryTimeEnd = System.nanoTime();

			// commit response
			if (result != null) {

				// store property view that will be used to render the results
				result.setPropertyView(propertyView.get(securityContext));

				// allow resource to modify result set
				resource.postProcessResultSet(result);

				DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

				result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

				Writer writer = response.getWriter();

				// gson.toJson(result, writer);
				writeCsv(result, writer);
				response.setStatus(HttpServletResponse.SC_OK);
				writer.append("\n");    // useful newline
				writer.flush();
				writer.close();
			} else {

				logger.log(Level.WARNING, "Result was null!");

				int code = HttpServletResponse.SC_NO_CONTENT;

				response.setStatus(code);

				Writer writer = response.getWriter();

				// writer.append(jsonError(code, "Result was null!"));
				writer.flush();
				writer.close();

			}
		} catch (FrameworkException frameworkException) {

			// set status
			response.setStatus(frameworkException.getStatus());

			// gson.toJson(frameworkException, response.getWriter());
//                      response.getWriter().println();
//                      response.getWriter().flush();
//                      response.getWriter().close();
		} catch (JsonSyntaxException jsex) {

			logger.log(Level.WARNING, "JsonSyntaxException in GET", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);

			// response.getWriter().append(jsonError(code, "JsonSyntaxException in GET: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.log(Level.WARNING, "JsonParseException in GET", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);

			// response.getWriter().append(jsonError(code, "JsonSyntaxException in GET: " + jpex.getMessage()));

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception in GET", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);

			// response.getWriter().append(jsonError(code, "JsonSyntaxException in GET: " + t.getMessage()));

		}

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
	 * Write list of objects to output
	 *
	 * @param result
	 * @param out
	 * @throws IOException
	 */
	private void writeCsv(final Result result, Writer out) throws IOException {

		List<GraphObject> list = result.getResults();
		boolean headerWritten  = false;

		for (GraphObject obj : list) {

			// Write column headers
			if (!headerWritten) {

				StringBuilder row = new StringBuilder();

				for (PropertyKey key : obj.getPropertyKeys(defaultPropertyView)) {

					row.append("\"").append(key.dbName()).append("\",");
				}

				// remove last ,
				row.deleteCharAt(row.lastIndexOf(","));
				out.append(row).append("\n");

				// flush each line
				out.flush();

				headerWritten = true;

			}

			StringBuilder row = new StringBuilder();

			for (PropertyKey key : obj.getPropertyKeys(defaultPropertyView)) {

				Object value = obj.getProperty(key);

				row.append("\"").append((value != null
							 ? StringUtils.replace(value.toString(), "\"", "\\\"")    // escaping for CSV
							 : "")).append("\",");

			}

			// remove last ,
			row.deleteCharAt(row.lastIndexOf(","));
			out.append(row).append("\n");

			// flush each line
			out.flush();
		}

	}

	//~--- get methods ----------------------------------------------------
	
	private Authenticator getAuthenticator() throws FrameworkException {
		
		return (Authenticator) Services.command(null, AuthenticatorCommand.class).execute(getServletConfig());
		
	}

	//~--- inner classes --------------------------------------------------

	// <editor-fold defaultstate="collapsed" desc="nested classes">
	private class ThreadLocalPropertyView extends ThreadLocal<String> implements Value<String> {

		@Override
		protected String initialValue() {

			return defaultPropertyView;

		}

		//~--- get methods --------------------------------------------

		@Override
		public String get(SecurityContext securityContext) {

			return get();

		}

		//~--- set methods --------------------------------------------

		@Override
		public void set(SecurityContext securityContext, String value) {

			set(value);

		}

	}

}
