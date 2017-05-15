/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import au.com.bytecode.opencsv.CSVParser;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.common.PagingHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.JsonInput;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.StructrTransactionListener;
import org.structr.core.Value;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.resource.Resource;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.schema.parser.DatePropertyParser;

//~--- classes ----------------------------------------------------------------
/**
 * This servlet produces CSV (comma separated value) lists out of a search
 * result
 *
 *
 */
public class CsvServlet extends HttpServlet implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(CsvServlet.class.getName());

	private static final char DEFAULT_FIELD_SEPARATOR = ';';
	private static final char DEFAULT_QUOTE_CHARACTER = '"';

	private static final char DEFAULT_FIELD_SEPARATOR_COLLECTION_CONTENTS = ',';
	private static final char DEFAULT_QUOTE_CHARACTER_COLLECTION_CONTENTS = '"';

	private static final boolean DEFAULT_PERIODIC_COMMIT = false;
	private static final int DEFAULT_PERIODIC_COMMIT_INTERVAL = 1000;
	private static final String REMOVE_LINE_BREAK_PARAM = "nolinebreaks";
	private static final String WRITE_BOM = "bom";

	//~--- fields ---------------------------------------------------------
	private SecurityContext securityContext;
	private final Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<>();
	private Value<String> propertyView = null;

	private static boolean removeLineBreaks = false;
	private static boolean writeBom = false;

	private String defaultPropertyView;
	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();
	private ThreadLocalGson gson                  = null;


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
		this.gson                = new ThreadLocalGson(propertyView, config.getOutputNestingDepth());

	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws UnsupportedEncodingException {

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

				resource = ResourceHelper.optimizeNestedResourceChain(securityContext, request, resourceMap, propertyView);
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

					logger.warn("Result was null!");

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

			logger.warn("JsonSyntaxException in GET", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);

			// response.getWriter().append(jsonError(code, "JsonSyntaxException in GET: " + jsex.getMessage()));
		} catch (JsonParseException jpex) {

			logger.warn("JsonParseException in GET", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);

			// response.getWriter().append(jsonError(code, "JsonSyntaxException in GET: " + jpex.getMessage()));
		} catch (Throwable t) {

			logger.warn("Exception in GET", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);

			// response.getWriter().append(jsonError(code, "JsonSyntaxException in GET: " + t.getMessage()));
		}

	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final String fieldSeparatorHeader = request.getHeader("X-CSV-Field-Separator");
		final char fieldSeparator = (fieldSeparatorHeader == null) ? DEFAULT_FIELD_SEPARATOR : fieldSeparatorHeader.charAt(0);

		final String quoteCharacterHeader = request.getHeader("X-CSV-Quote-Character");
		final char quoteCharacter = (quoteCharacterHeader == null) ? DEFAULT_QUOTE_CHARACTER : quoteCharacterHeader.charAt(0);

		final String doPeridicCommitHeader = request.getHeader("X-CSV-Periodic-Commit");
		final boolean doPeriodicCommit = (doPeridicCommitHeader == null) ? DEFAULT_PERIODIC_COMMIT : Boolean.parseBoolean(doPeridicCommitHeader);

		final String periodicCommitIntervalHeader = request.getHeader("X-CSV-Periodic-Commit-Interval");
		final Integer periodicCommitInterval = (periodicCommitIntervalHeader == null) ? DEFAULT_PERIODIC_COMMIT_INTERVAL : Integer.parseInt(periodicCommitIntervalHeader);

		final List<RestMethodResult> results = new LinkedList<>();
		final Authenticator authenticator;
		final Resource resource;

		try {

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

			final App app = StructrApp.getInstance(securityContext);

			if (securityContext != null) {

				// isolate resource authentication
				try (final Tx tx = app.tx()) {

					resource = ResourceHelper.applyViewTransformation(request, securityContext, ResourceHelper.optimizeNestedResourceChain(securityContext, request, resourceMap, propertyView), propertyView);
					authenticator.checkResourceAccess(securityContext, request, resource.getResourceSignature(), propertyView.get(securityContext));
					tx.success();
				}

				// do not send websocket notifications for created objects
				securityContext.setDoTransactionNotifications(false);

				// isolate doPost
				boolean retry = true;
				while (retry) {

					retry = false;

					final Iterable<JsonInput> csv = cleanAndParseCSV(input, resource, fieldSeparator, quoteCharacter);

					if (resource.createPostTransaction()) {

						if (doPeriodicCommit) {

							final List<JsonInput> list = new ArrayList<>();
							csv.iterator().forEachRemaining(list::add);
							final List<List<JsonInput>> chunkedCsv = ListUtils.partition(list, periodicCommitInterval);

							final int totalChunkNo = chunkedCsv.size();
							int currentChunkNo = 0;

							for (final List<JsonInput> currentChunk : chunkedCsv) {

								try (final Tx tx = app.tx()) {

									currentChunkNo++;

									for (final JsonInput propertySet : currentChunk) {

										handleCsvPropertySet(results, resource, propertySet);

									}

									tx.success();

									logger.info("CSV: Finished importing chunk " + currentChunkNo + " / " + totalChunkNo);

									for (final StructrTransactionListener listener : TransactionCommand.getTransactionListeners()) {

										final Map<String, Object> data = new LinkedHashMap();
										data.put("type", "CSV_IMPORT_STATUS");
										data.put("title", "CSV Import Status");
										data.put("text", "Finished importing chunk " + currentChunkNo + " / " + totalChunkNo);
										data.put("username", securityContext.getUser(false).getName());
										listener.simpleBroadcast("GENERIC_MESSAGE", data);

									}

								} catch (RetryException ddex) {
									retry = true;
								}

							}

						} else {

							try (final Tx tx = app.tx()) {

								for (final JsonInput propertySet : csv) {

									handleCsvPropertySet(results, resource, propertySet);
								}

								tx.success();

							} catch (RetryException ddex) {
								retry = true;
							}
						}

					} else {

						if (doPeriodicCommit) {
							logger.warn("Resource auto-creates POST transaction - can not commit periodically!");
						}

						try {

							for (final JsonInput propertySet : csv) {

								handleCsvPropertySet(results, resource, propertySet);
							}

						} catch (RetryException ddex) {
							retry = true;
						}
					}
				}

				logger.info("CSV: Finished importing csv data.");

				for (final StructrTransactionListener listener : TransactionCommand.getTransactionListeners()) {

					final Map<String, Object> data = new LinkedHashMap();
					data.put("type", "CSV_IMPORT_STATUS");
					data.put("title", "CSV Import Done");
					data.put("text", "Finished importing csv data.");
					data.put("username", securityContext.getUser(false).getName());
					listener.simpleBroadcast("GENERIC_MESSAGE", data);

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

			logger.warn("POST: Invalid JSON syntax", jsex.getMessage());

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in POST: " + jsex.getMessage()));

		} catch (JsonParseException jpex) {

			logger.warn("Unable to parse JSON string", jpex.getMessage());

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonParseException in POST: " + jpex.getMessage()));

		} catch (UnsupportedOperationException uoe) {

			logger.warn("POST not supported");

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "POST not supported: " + uoe.getMessage()));

		} catch (Throwable t) {

			logger.warn("Exception in POST", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "JsonSyntaxException in POST: " + t.getMessage()));

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}

		}
	}

	private void handleCsvPropertySet (final List<RestMethodResult> results, final Resource resource, final JsonInput propertySet) throws FrameworkException {

		try {

			results.add(resource.doPost(convertPropertySetToMap(propertySet)));

		} catch (FrameworkException fxe) {

			logger.warn("CSV Import Error: " + fxe.getMessage() + "\n" + fxe.toString() + "\n{}", propertySet);

			for (final StructrTransactionListener listener : TransactionCommand.getTransactionListeners()) {

				final Map<String, Object> data = new LinkedHashMap();
				data.put("type", "CSV_IMPORT_ERROR");
				data.put("title", "CSV Import Error");
				data.put("text", fxe.getMessage() + "<br>" + fxe.toString() + "<br>" + propertySet.toString());
				data.put("username", securityContext.getUser(false).getName());
				listener.simpleBroadcast("GENERIC_MESSAGE", data);

			}

			throw fxe;
		}
	}

	private static String escapeForCsv(final Object value) {

		String result;

		if (value instanceof String[]) {

			// Special handling for StringArrays
			ArrayList<String> quotedStrings = new ArrayList();
			for (final String str : Arrays.asList((String[])value)) {
				// The strings can contain quotes - these need to be escaped with 3 slashes in the output
				quotedStrings.add("\\\"" + StringUtils.replace(str, "\"", "\\\\\\\"") + "\\\"");
			}

			result = quotedStrings.toString();

		} else if (value instanceof Collection) {

			// Special handling for collections of nodes
			ArrayList<String> quotedStrings = new ArrayList();
			for (final Object obj : (Collection)value) {
				quotedStrings.add("\\\"" + obj.toString() + "\\\"");
			}

			result = quotedStrings.toString();

		} else if (value instanceof Date) {

			result = DatePropertyParser.format((Date) value, DateProperty.getDefaultFormat());

		} else {

			result = StringUtils.replace(value.toString(), "\"", "\\\"");

		}

		if (!removeLineBreaks) {
			return StringUtils.replace(StringUtils.replace(result, "\r\n", "\n"), "\r", "\n");
		}

		return StringUtils.replace(StringUtils.replace(result, "\r\n", ""), "\r", "");

	}

	private void writeUtf8Bom(Writer out) {
		try {
			out.write("\ufeff");
		} catch (IOException ex) {
			logger.warn("Unable to write UTF-8 BOM", ex);
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

		final List<GraphObject> list = result.getResults();
		final StringBuilder row      = new StringBuilder();
		boolean headerWritten        = false;

		for (final GraphObject obj : list) {

			// Write column headers
			if (!headerWritten) {

				row.setLength(0);

				for (PropertyKey key : obj.getPropertyKeys(propertyView)) {

					row.append("\"").append(key.dbName()).append("\"").append(DEFAULT_FIELD_SEPARATOR);
				}

				// remove last ;
				int pos = row.lastIndexOf("" + DEFAULT_FIELD_SEPARATOR);
				if (pos >= 0) {

					row.deleteCharAt(pos);
				}

				// append DOS-style line feed as defined in RFC 4180
				out.append(row).append("\r\n");

				// flush each line
				out.flush();

				headerWritten = true;

			}

			row.setLength(0);

			for (PropertyKey key : obj.getPropertyKeys(propertyView)) {

				Object value = obj.getProperty(key);

				row.append("\"").append((value != null
					? escapeForCsv(value)
					: "")).append("\"").append(DEFAULT_FIELD_SEPARATOR);

			}

			// remove last ;
			row.deleteCharAt(row.lastIndexOf("" + DEFAULT_FIELD_SEPARATOR));
			out.append(row).append("\r\n");

			// flush each line
			out.flush();
		}

	}

	private Iterable<JsonInput> cleanAndParseCSV(final String input, final Resource resource, final char fieldSeparator, final char quoteCharacter) throws FrameworkException, IOException {

		final BufferedReader reader  = new BufferedReader(new StringReader(input));
		final String headerLine      = reader.readLine();
		final CSVParser parser       = new CSVParser(fieldSeparator, quoteCharacter);
		final String[] propertyNames = parser.parseLine(headerLine);

		return new Iterable<JsonInput>() {

			@Override
			public Iterator<JsonInput> iterator() {

				return new Iterator<JsonInput>() {

					String line = null;

					@Override
					public boolean hasNext() {

						try {

							line = reader.readLine();

							return StringUtils.isNotBlank(line);

						} catch (IOException ioex) {
							logger.warn("", ioex);
						}

						return false;
					}

					@Override
					public JsonInput next() {

						try {

							if (StringUtils.isNotBlank(line)) {

								final JsonInput jsonInput = new JsonInput();
								final String[] columns    = parser.parseLine(line);
								final int len             = columns.length;

								for (int i=0; i<len; i++) {

									final String key = propertyNames[i];

									if (StructrApp.getConfiguration().getPropertyKeyForJSONName(resource.getEntityClass(), key).isCollection()) {

										// if the current property is a collection, split it into its parts
										jsonInput.add(key, extractArrayContentsFromArray(columns[i], key));

									} else {
										jsonInput.add(key, columns[i]);
									}
								}

								return jsonInput;
							}

						} catch (IOException ioex) {
							logger.warn("Exception in CSV line: {}", line);
							logger.warn("", ioex);

							for (final StructrTransactionListener listener : TransactionCommand.getTransactionListeners()) {

								final Map<String, Object> data = new LinkedHashMap();
								data.put("type", "CSV_IMPORT_ERROR");
								data.put("title", "CSV Import Error");
								data.put("text", "Error occured with dataset: " + line);
								data.put("username", securityContext.getUser(false).getName());
								listener.simpleBroadcast("GENERIC_MESSAGE", data);

							}

						}

						return null;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException("Removal not supported.");
					}

				};
			}
		};
	}

	private ArrayList<String> extractArrayContentsFromArray (final String value, final String propertyName) throws IOException {

		final CSVParser arrayParser              = new CSVParser(DEFAULT_QUOTE_CHARACTER_COLLECTION_CONTENTS, DEFAULT_FIELD_SEPARATOR_COLLECTION_CONTENTS);
		final ArrayList<String> extractedStrings = new ArrayList();

		extractedStrings.addAll(Arrays.asList(arrayParser.parseLine(stripArrayBracketsFromString(value, propertyName))));

		return extractedStrings;
	}

	private String stripArrayBracketsFromString (final String value, final String propertyName) {

		if (value.length() > 0) {

			if (value.charAt(0) != '[' || value.charAt(value.length() - 1) != ']') {

				logger.warn("Missing opening/closing brackets for array {}: {} ", propertyName, value);
				return value;

			} else {

				return value.substring(1, value.length() - 1);

			}

		} else {
			return "";
		}

	}

	private Map<String, Object> convertPropertySetToMap(JsonInput propertySet) {

		if (propertySet != null) {
			return propertySet.getAttributes();
		}

		return new LinkedHashMap<>();
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
