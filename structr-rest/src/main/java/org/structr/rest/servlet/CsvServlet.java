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

import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
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
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PagingHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.rest.resource.Resource;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;

//~--- classes ----------------------------------------------------------------
/**
 * This servlet produces CSV (comma separated value) lists out of a search
 * result
 *
 *
 */
public class CsvServlet extends HttpServlet implements HttpServiceServlet {

	private static final Logger logger = Logger.getLogger(CsvServlet.class.getName());

	private static final String DELIMITER = ";";
	private static final String REMOVE_LINE_BREAK_PARAM = "nolinebreaks";
	private static final String WRITE_BOM = "bom";

	//~--- fields ---------------------------------------------------------
	private final Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<>();
	private Value<String> propertyView = null;

	private static boolean removeLineBreaks = false;
	private static boolean writeBom = false;

	private String defaultPropertyView;
	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();


	//~--- methods --------------------------------------------------------

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public void init() {

		// inject resources
		resourceMap.putAll(config.getResourceProvider().getResources());

		// initialize variables
		this.propertyView        = new ThreadLocalPropertyView();
		this.defaultPropertyView = config.getDefaultPropertyView();
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws UnsupportedEncodingException {

		SecurityContext securityContext = null;
		Authenticator authenticator = null;
		Result result = null;
		Resource resource = null;

		try {

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}
			final App app = StructrApp.getInstance(securityContext);

//                      logRequest("GET", request);
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/csv; charset=utf-8");

			// set default value for property view
			propertyView.set(securityContext, defaultPropertyView);

			// evaluate constraints and measure query time
			double queryTimeStart = System.nanoTime();

			// isolate resource authentication
			try (final Tx tx = app.tx()) {

				resource = ResourceHelper.optimizeNestedResourceChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView));
				authenticator.checkResourceAccess(securityContext, request, resource.getResourceSignature(), propertyView.get(securityContext));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				String resourceSignature = resource.getResourceSignature();

				// let authenticator examine request again
				authenticator.checkResourceAccess(securityContext, request, resourceSignature, propertyView.get(securityContext));

				// add sorting & paging
				String pageSizeParameter = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_PAGE_SIZE);
				String pageParameter = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_PAGE_NUMBER);
				String offsetId = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_OFFSET_ID);
				String sortOrder = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_ORDER);
				String sortKeyName = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_KEY);
				boolean sortDescending = (sortOrder != null && "desc".equals(sortOrder.toLowerCase()));
				int pageSize = Services.parseInt(pageSizeParameter, NodeFactory.DEFAULT_PAGE_SIZE);
				int page = Services.parseInt(pageParameter, NodeFactory.DEFAULT_PAGE);
				PropertyKey sortKey = null;

				// set sort key
				if (sortKeyName != null) {

					Class<? extends GraphObject> type = resource.getEntityClass();

					sortKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(type, sortKeyName, false);

				}

				// Should line breaks be removed?
				removeLineBreaks = StringUtils.equals(request.getParameter(REMOVE_LINE_BREAK_PARAM), "1");

				// Should a leading BOM be written?
				writeBom = StringUtils.equals(request.getParameter(WRITE_BOM), "1");

				// do action
				result = resource.doGet(sortKey, sortDescending, pageSize, page, offsetId);

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

					if (writeBom) {
						writeUtf8Bom(writer);
					}

					// gson.toJson(result, writer);
					writeCsv(result, writer, propertyView.get(securityContext));
					response.setStatus(HttpServletResponse.SC_OK);
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
				tx.success();
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

	private static String escapeForCsv(final Object value) {

		String result = StringUtils.replace(value.toString(), "\"", "\\\"");

		if (!removeLineBreaks) {
			return StringUtils.replace(StringUtils.replace(result, "\r\n", "\n"), "\r", "\n");
		}

		return StringUtils.replace(StringUtils.replace(result, "\r\n", ""), "\r", "");

	}

	private void writeUtf8Bom(Writer out) {
		try {
			out.write("\ufeff");
		} catch (IOException ex) {
			logger.log(Level.WARNING, "Unable to write UTF-8 BOM", ex);
		}
	}

	/**
	 * Write list of objects to output
	 *
	 * @param result
	 * @param out
	 * @param propertyView
	 * @throws IOException
	 */
	public static void writeCsv(final Result result, final Writer out, final String propertyView) throws IOException {

		List<GraphObject> list = result.getResults();
		boolean headerWritten = false;

		for (GraphObject obj : list) {

			// Write column headers
			if (!headerWritten) {

				StringBuilder row = new StringBuilder();

				for (PropertyKey key : obj.getPropertyKeys(propertyView)) {

					row.append("\"").append(key.dbName()).append("\"").append(DELIMITER);
				}

				// remove last ,
				int pos = row.lastIndexOf(DELIMITER);
				if (pos >= 0) {

					row.deleteCharAt(pos);
				}

				// append DOS-style line feed as defined in RFC 4180
				out.append(row).append("\r\n");

				// flush each line
				out.flush();

				headerWritten = true;

			}

			StringBuilder row = new StringBuilder();

			for (PropertyKey key : obj.getPropertyKeys(propertyView)) {

				Object value = obj.getProperty(key);

				row.append("\"").append((value != null
					? escapeForCsv(value)
					: "")).append("\"").append(DELIMITER);

			}

			// remove last ,
			row.deleteCharAt(row.lastIndexOf(DELIMITER));
			out.append(row).append("\r\n");

			// flush each line
			out.flush();
		}

	}

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
