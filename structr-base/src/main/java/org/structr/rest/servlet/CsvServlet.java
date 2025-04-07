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

import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.PropertyView;
import org.structr.common.RequestKeywords;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.GraphObject;
import org.structr.core.JsonInput;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTEndpoints;
import org.structr.rest.common.CsvHelper;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.schema.parser.DatePropertyGenerator;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * This servlet produces CSV (comma separated value) lists out of a search
 * result.
 */
public class CsvServlet extends AbstractDataServlet implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(CsvServlet.class.getName());

	public static final String DEFAULT_FIELD_SEPARATOR_HEADER_NAME          = "X-CSV-Field-Separator";
	public static final String DEFAULT_QUOTE_CHARACTER_HEADER_NAME          = "X-CSV-Quote-Character";
	public static final String DEFAULT_PERIODIC_COMMIT_HEADER_NAME          = "X-CSV-Periodic-Commit";
	public static final String DEFAULT_PERIODIC_COMMIT_INTERVAL_HEADER_NAME = "X-CSV-Periodic-Commit-Interval";
	public static final String DEFAULT_RANGE_HEADER_NAME                    = "X-CSV-Range";

	public static final char DEFAULT_FIELD_SEPARATOR = ';';
	public static final char DEFAULT_QUOTE_CHARACTER = '"';
	public static final boolean DEFAULT_PERIODIC_COMMIT = false;
	public static final int DEFAULT_PERIODIC_COMMIT_INTERVAL = 1000;

	public static final char DEFAULT_FIELD_SEPARATOR_COLLECTION_CONTENTS = ',';
	public static final char DEFAULT_QUOTE_CHARACTER_COLLECTION_CONTENTS = '"';


	private static final String REMOVE_LINE_BREAK_PARAM = "nolinebreaks";
	private static final String WRITE_BOM = "bom";

	private SecurityContext securityContext;

	private static boolean removeLineBreaks = false;
	private static boolean writeBom = false;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws UnsupportedEncodingException {

		Authenticator authenticator = null;
		RESTCallHandler handler      = null;

		setCustomResponseHeaders(response);

		try {

			assertInitialized();

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/csv; charset=utf-8");

			// isolate resource authentication
			try (final Tx tx = app.tx()) {

				final Principal currentUser = securityContext.getUser(false);

				handler = RESTEndpoints.resolveRESTCallHandler(request, config.getDefaultPropertyView(), getTypeOrDefault(currentUser, StructrTraits.USER));
				authenticator.checkResourceAccess(securityContext, request, handler.getResourceSignature(), handler.getRequestedView());

				RuntimeEventLog.csv("Get", handler.getResourceSignature(), currentUser);

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				final String resourceSignature = handler.getResourceSignature();

				// let authenticator examine request again
				authenticator.checkResourceAccess(securityContext, request, resourceSignature, handler.getRequestedView());

				// add sorting & paging
				final String pageSizeParameter          = request.getParameter(RequestKeywords.PageSize.keyword());
				final String pageParameter              = request.getParameter(RequestKeywords.PageNumber.keyword());
				final String[] sortOrders               = request.getParameterValues(RequestKeywords.SortOrder.keyword());
				final String[] sortKeyNames             = request.getParameterValues(RequestKeywords.SortKey.keyword());
				final int pageSize                      = Services.parseInt(pageSizeParameter, NodeFactory.DEFAULT_PAGE_SIZE);
				final int page                          = Services.parseInt(pageParameter, NodeFactory.DEFAULT_PAGE);
				final String type                       = handler.getEntityClassOrDefault(securityContext);
				final SortOrder sortOrder               = new DefaultSortOrder(type, sortKeyNames, sortOrders);

				// Should line breaks be removed?
				removeLineBreaks = StringUtils.equals(request.getParameter(REMOVE_LINE_BREAK_PARAM), "1");

				// Should a leading BOM be written?
				writeBom = StringUtils.equals(request.getParameter(WRITE_BOM), "1");

				// do action
				try (final ResultStream result = handler.doGet(securityContext, sortOrder, pageSize, page)) {

					if (result != null) {

						try (Writer writer = response.getWriter()) {

							if (writeBom) {
								writeUtf8Bom(writer);
							}

							writeCsv(result, writer, handler.getRequestedView());
							response.setStatus(HttpServletResponse.SC_OK);
							writer.flush();
						}

					} else {

						logger.warn("Result was null!");

						int code = HttpServletResponse.SC_NO_CONTENT;

						response.setStatus(code);

						try (Writer writer = response.getWriter()) {
							writer.flush();
						}

					}
				}

				tx.success();
			}

		} catch (FrameworkException frameworkException) {

			// set status
			response.setStatus(frameworkException.getStatus());

		} catch (JsonSyntaxException jsex) {

			logger.warn("JsonSyntaxException in GET", jsex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);

		} catch (JsonParseException jpex) {

			logger.warn("JsonParseException in GET", jpex);

			int code = HttpServletResponse.SC_BAD_REQUEST;

			response.setStatus(code);

		} catch (Throwable t) {

			logger.warn("Exception in GET", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final String fieldSeparatorHeader         = request.getHeader(DEFAULT_FIELD_SEPARATOR_HEADER_NAME);
		final char fieldSeparator                 = (fieldSeparatorHeader == null) ? DEFAULT_FIELD_SEPARATOR : fieldSeparatorHeader.charAt(0);
		final String quoteCharacterHeader         = request.getHeader(DEFAULT_QUOTE_CHARACTER_HEADER_NAME);
		final char quoteCharacter                 = (quoteCharacterHeader == null) ? DEFAULT_QUOTE_CHARACTER : quoteCharacterHeader.charAt(0);
		final String doPeridicCommitHeader        = request.getHeader(DEFAULT_PERIODIC_COMMIT_HEADER_NAME);
		final boolean doPeriodicCommit            = (doPeridicCommitHeader == null) ? DEFAULT_PERIODIC_COMMIT : Boolean.parseBoolean(doPeridicCommitHeader);
		final String periodicCommitIntervalHeader = request.getHeader(DEFAULT_PERIODIC_COMMIT_INTERVAL_HEADER_NAME);
		final int periodicCommitInterval          = (periodicCommitIntervalHeader == null) ? DEFAULT_PERIODIC_COMMIT_INTERVAL : Integer.parseInt(periodicCommitIntervalHeader);
		final String rangeHeader                  = request.getHeader(DEFAULT_RANGE_HEADER_NAME);
		final List<RestMethodResult> results      = new LinkedList<>();

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
			final Reader input = request.getReader();

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			if (securityContext != null) {

				String username = null;

				// isolate resource authentication
				try (final Tx tx = app.tx()) {

					final Principal currentUser = securityContext.getUser(false);

					username = currentUser.getName();
					handler = RESTEndpoints.resolveRESTCallHandler(request, config.getDefaultPropertyView(), getTypeOrDefault(currentUser, StructrTraits.USER));
					authenticator.checkResourceAccess(securityContext, request, handler.getResourceSignature(), handler.getRequestedView());

					RuntimeEventLog.csv("Post", handler.getResourceSignature(), currentUser);

					tx.success();
				}

				// do not send websocket notifications for created objects
				securityContext.setDoTransactionNotifications(false);
				securityContext.disableModificationOfAccessTime();
				securityContext.disablePreventDuplicateRelationships();

				final long startTime = System.currentTimeMillis();

				final Map<String, Object> data = new LinkedHashMap();
				data.put("type", "CSV_IMPORT_STATUS");
				data.put("subtype", "BEGIN");
				data.put("username", username);
				TransactionCommand.simpleBroadcastGenericMessage(data);

				// isolate doPost
				boolean retry = true;
				while (retry) {

					retry = false;

					final Iterable<JsonInput> csv = CsvHelper.cleanAndParseCSV(securityContext, input, handler.getTypeName(securityContext), fieldSeparator, quoteCharacter, rangeHeader);

					if (handler.createPostTransaction()) {

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

										handleCsvPropertySet(securityContext, results, handler, propertySet);

									}

									tx.success();

									logger.info("CSV: Finished importing chunk " + currentChunkNo + " / " + totalChunkNo);

									final Map<String, Object> chunkMsgData = new LinkedHashMap();
									chunkMsgData.put("type",           "CSV_IMPORT_STATUS");
									chunkMsgData.put("subtype",        "CHUNK");
									chunkMsgData.put("currentChunkNo", currentChunkNo);
									chunkMsgData.put("totalChunkNo",   totalChunkNo);
									chunkMsgData.put("username",       username);
									TransactionCommand.simpleBroadcastGenericMessage(chunkMsgData);

								} catch (RetryException ddex) {
									retry = true;
								}

							}

						} else {

							try (final Tx tx = app.tx()) {

								for (final JsonInput propertySet : csv) {

									handleCsvPropertySet(securityContext, results, handler, propertySet);
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

								handleCsvPropertySet(securityContext, results, handler, propertySet);
							}

						} catch (RetryException ddex) {
							retry = true;
						}
					}
				}

				final long endTime = System.currentTimeMillis();
				DecimalFormat decimalFormat  = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
				final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

				logger.info("CSV: Finished importing CSV data (Time: {})", duration);

				final Map<String, Object> endMsgData = new LinkedHashMap();
				endMsgData.put("type", "CSV_IMPORT_STATUS");
				endMsgData.put("subtype", "END");
				endMsgData.put("duration", duration);
				endMsgData.put("username", username);
				TransactionCommand.simpleBroadcastGenericMessage(endMsgData);

				// isolate write output
				try (final Tx tx = app.tx()) {

					if (!results.isEmpty()) {

						final RestMethodResult result = results.get(0);
						final int resultCount         = results.size();

						if (result != null) {

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

							commitResponse(securityContext, request, response, result, handler.getRequestedView(), handler.isCollection());
						}

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

		} catch (FrameworkException frameworkException) {

			writeException(response, frameworkException);

		} catch (JsonSyntaxException jsex) {

			logger.warn("POST: Invalid JSON syntax", jsex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonSyntaxException in POST: " + jsex.getMessage());

		} catch (JsonParseException jpex) {

			logger.warn("Unable to parse JSON string", jpex.getMessage());

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "JsonParseException in POST: " + jpex.getMessage());

		} catch (UnsupportedOperationException uoe) {

			logger.warn("POST not supported");

			writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "POST not supported: " + uoe.getMessage());

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

	// ---- interface Feature -----
	@Override
	public String getModuleName() {
		return "csv";
	}

	private void handleCsvPropertySet(final SecurityContext securityContext, final List<RestMethodResult> results, final RESTCallHandler handler, final JsonInput propertySet) throws FrameworkException {

		try {

			results.add(handler.doPost(securityContext, convertPropertySetToMap(propertySet)));

		} catch (FrameworkException fxe) {

			logger.warn("CSV Import Error: " + fxe.getMessage() + "\n" + fxe.toString() + "\n{}", propertySet);

			final Map<String, Object> data = new LinkedHashMap();
			data.put("type", "CSV_IMPORT_ERROR");
			data.put("title", "CSV Import Error");
			data.put("text", fxe.getMessage() + "<br>" + fxe.toString() + "<br>" + propertySet.toString());
			data.put("username", securityContext.getUser(false).getName());
			TransactionCommand.simpleBroadcastGenericMessage(data);

			throw fxe;
		}
	}

	private static String escapeForCsv(final Object value) {

		final String escaped = escapeForCsv(value, '"');

		// post-process escaped string
		if (!removeLineBreaks) {
			return StringUtils.replace(StringUtils.replace(escaped, "\r\n", "\n"), "\r", "\n");
		}

		return StringUtils.replace(StringUtils.replace(escaped, "\r\n", ""), "\r", "");
	}

	public static String escapeForCsv(final Object value, final char quoteChar) {

		String result;

		if (value == null) {

			result = "";

		} else if (value instanceof String[]) {

			final ArrayList<String> quotedStrings = new ArrayList();
			for (final String str : Arrays.asList((String[])value)) {
				// The strings can contain quotes - these need to be escaped with 3 slashes in the output
				quotedStrings.add("\\" + quoteChar + StringUtils.replace(str, ""+quoteChar, "\\\\\\" + quoteChar) + "\\" + quoteChar);
			}

			result = quotedStrings.toString();

		} else if (value instanceof Date[]) {

			final ArrayList<String> dateStrings = new ArrayList();
			for (final Date d : Arrays.asList((Date[])value)) {
				dateStrings.add("\\" + quoteChar + DatePropertyGenerator.format(d, DateProperty.getDefaultFormat()) + "\\" + quoteChar);
			}

			result = dateStrings.toString();

		} else if (value instanceof Object[]) {

			final ArrayList<String> quotedStrings = new ArrayList();
			for (final Object o : Arrays.asList((Object[])value)) {
				// The strings can contain quotes - these need to be escaped with 3 slashes in the output
				quotedStrings.add("\\" + quoteChar + StringUtils.replace(o.toString(), ""+quoteChar, "\\\\\\" + quoteChar) + "\\" + quoteChar);
			}

			result = quotedStrings.toString();

		} else if (value instanceof Iterable) {

			// Special handling for collections of nodes
			ArrayList<String> quotedStrings = new ArrayList();
			for (final Object obj : (Iterable)value) {
				quotedStrings.add("\\" + quoteChar + obj.toString() + "\\" + quoteChar);
			}

			result = quotedStrings.toString();

		} else if (value instanceof Date) {

			result = DatePropertyGenerator.format((Date) value, DateProperty.getDefaultFormat());

		} else {

			result = StringUtils.replace(value.toString(), ""+quoteChar, "\\" + quoteChar);
		}

		return result;
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
	public static void writeCsv(final ResultStream<GraphObject> result, final Writer out, final String propertyView) throws IOException {

		final StringBuilder row      = new StringBuilder();
		boolean headerWritten        = false;

		for (final GraphObject obj : result) {

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

				row.append("\"").append(escapeForCsv(value)).append("\"").append(DEFAULT_FIELD_SEPARATOR);
			}

			// remove last ;
			row.deleteCharAt(row.lastIndexOf("" + DEFAULT_FIELD_SEPARATOR));
			out.append(row).append("\r\n");

			// flush each line
			out.flush();
		}
	}

	private Map<String, Object> convertPropertySetToMap(JsonInput propertySet) {

		if (propertySet != null) {
			return propertySet.getAttributes();
		}

		return new LinkedHashMap<>();
	}
}
