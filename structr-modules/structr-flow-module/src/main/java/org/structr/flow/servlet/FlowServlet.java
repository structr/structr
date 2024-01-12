/*
 * Copyright (C) 2010-2024 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.RequestKeywords;
import org.structr.common.SecurityContext;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.Tx;
import org.structr.flow.impl.FlowContainer;
import org.structr.rest.RestMethodResult;
import org.structr.rest.servlet.JsonRestServlet;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FlowServlet extends JsonRestServlet {

	private static final Logger logger = LoggerFactory.getLogger(FlowServlet.class);

	@Override
	protected void doGetOrHead(final HttpServletRequest request, final HttpServletResponse response, final boolean returnContent) throws ServletException, IOException {

		SecurityContext securityContext = null;
		Authenticator authenticator     = null;
		ResultStream result             = null;

		setCustomResponseHeaders(response);

		try {

			final Map<String, Object> flowParameters = new HashMap<>();
			final Iterable<Object> flowResult;
			final int depth = Services.parseInt(request.getParameter(RequestKeywords.OutputDepth.keyword()), config.getOutputNestingDepth());

			// set default value for property view
			propertyView.set(securityContext, config.getDefaultPropertyView());

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			// evaluate constraints and measure query time
			double queryTimeStart    = System.nanoTime();

			try (final Tx tx = app.tx()) {

				final String flowName = request.getPathInfo().substring(1);
				final FlowContainer flow = flowName.length() > 0 ? StructrApp.getInstance(securityContext).nodeQuery(FlowContainer.class).and(FlowContainer.effectiveName, flowName).getFirst() : null;

				if (flow != null) {

					flowResult = flow.evaluate(securityContext, flowParameters);

					result = new PagingIterable<>("FlowContainer " + flow.getUuid(), flowResult);

					if (returnContent) {

						// timing..
						double queryTimeEnd = System.nanoTime();

						DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
						result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

						processResult(securityContext, request, response, result, depth, false);
					}

					response.setStatus(HttpServletResponse.SC_OK);

				} else {

					response.setStatus(404);
					response.getWriter().append(RestMethodResult.jsonError(404, "Requested flow could not be found."));
				}

				tx.success();
			}

		} catch (FrameworkException frameworkException) {

			writeException(response, frameworkException);

		} catch (Throwable t) {

			logger.warn("Exception in GET (URI: {})", securityContext != null ? securityContext.getCompoundRequestURI() : "(null SecurityContext)");
			logger.warn(" => Error thrown: ", t);

			writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getClass().getSimpleName() + " in GET: " + t.getMessage());

		} finally {

			try {

				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}

		}

	}

	@Override
	protected void writeHtml(final SecurityContext securityContext, final HttpServletResponse response, final ResultStream result, final String baseUrl, final int nestingDepth, final boolean wrapSingleResultInArray, final boolean serializeNulls) throws FrameworkException, IOException {

		final App app                          = StructrApp.getInstance(securityContext);
		final boolean indentJson               = Settings.JsonIndentation.getValue();
		final StreamingFlowWriter flowStreamer = new StreamingFlowWriter(propertyView, indentJson, nestingDepth, wrapSingleResultInArray, serializeNulls);

		// isolate write output
		try (final Tx tx = app.tx()) {

			// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
			response.setContentType("text/html; charset=utf-8");

			final Writer writer = response.getWriter();

			flowStreamer.stream(securityContext, writer, result, baseUrl);
			writer.append("\n");    // useful newline

			tx.success();

		}
	}

	@Override
	public String getModuleName() {
		return "api-builder";
	}

	@Override
	protected void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		throw new UnsupportedOperationException("DELETE is not supported by the FlowServlet");
	}

	@Override
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		SecurityContext securityContext = null;
		Authenticator authenticator     = null;
		ResultStream result             = null;

		setCustomResponseHeaders(response);

		String requestBody = IOUtils.toString(request.getReader());

		Gson gson = getGson();
		Map<String, Object> flowParameters = gson.fromJson(requestBody, Map.class);

		try {

			final Iterable<Object> flowResult;
			final int depth = Services.parseInt(request.getParameter(RequestKeywords.OutputDepth.keyword()), config.getOutputNestingDepth());

			// set default value for property view
			propertyView.set(securityContext, config.getDefaultPropertyView());

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			// evaluate constraints and measure query time
			double queryTimeStart    = System.nanoTime();

			try (final Tx tx = app.tx()) {

				final String flowName = request.getPathInfo().substring(1);
				final FlowContainer flow = flowName.length() > 0 ? StructrApp.getInstance(securityContext).nodeQuery(FlowContainer.class).and(FlowContainer.effectiveName, flowName).getFirst() : null;

				if (flow != null) {

					flowResult = flow.evaluate(securityContext, flowParameters);

					result = new PagingIterable<>("FlowContainer " + flow.getUuid(), flowResult);

					// timing..
					double queryTimeEnd = System.nanoTime();

					DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
					result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

					processResult(securityContext, request, response, result, depth, false);

					response.setStatus(HttpServletResponse.SC_OK);

				} else {

					response.setStatus(404);
					response.getWriter().append(RestMethodResult.jsonError(404, "Requested flow could not be found."));
				}

				tx.success();
			}

		} catch (FrameworkException frameworkException) {

			writeException(response, frameworkException);

		} catch (Throwable t) {

			logger.warn("Exception in POST (URI: {})", securityContext != null ? securityContext.getCompoundRequestURI() : "(null SecurityContext)");
			logger.warn(" => Error thrown: ", t);

			int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			if (t instanceof AssertException) {
				statusCode = ((AssertException)t).getStatus();
			}

			writeJsonError(response, statusCode, t.getClass().getSimpleName() + " in POST: " + t.getMessage());

		} finally {

			try {

				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}

		}
	}

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		throw new UnsupportedOperationException("PUT is not supported by the FlowServlet");
	}

	protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		throw new UnsupportedOperationException("PATCH is not supported by the FlowServlet");
	}

}
