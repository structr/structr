/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.Tx;
import org.structr.flow.impl.FlowContainer;
import org.structr.rest.RestMethodResult;
import org.structr.rest.resource.Resource;
import org.structr.rest.servlet.JsonRestServlet;
import org.structr.rest.servlet.ResourceHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;

public class FlowServlet extends JsonRestServlet {

	private static final Logger logger = LoggerFactory.getLogger(FlowServlet.class);

	@Override
	protected void doGetOrHead(final HttpServletRequest request, final HttpServletResponse response, final boolean returnContent) throws ServletException, IOException {

		SecurityContext securityContext = null;
		Authenticator authenticator     = null;
		ResultStream result             = null;
		Resource resource               = null;

		setCustomResponseHeaders(response);

		try {

			final Map<String, Object> flowParameters = new HashMap<>();
			final Iterable<Object> flowResult;
			final int depth = Services.parseInt(request.getParameter(REQUEST_PARAMTER_OUTPUT_DEPTH), config.getOutputNestingDepth());

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

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				resource = ResourceHelper.optimizeNestedResourceChain(securityContext, request, resourceMap, propertyView);
				authenticator.checkResourceAccess(securityContext, request, resource.getResourceSignature(), propertyView.get(securityContext));
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			// evaluate constraints and measure query time
			double queryTimeStart    = System.nanoTime();

			try (final Tx tx = app.tx()) {

				final List<GraphObject> source = Iterables.toList(resource.doGet(null, -1, -1));

				if (!source.isEmpty() && source.size() == 1 && source.get(0) instanceof FlowContainer) {

					flowResult = ((FlowContainer)source.get(0)).evaluate(securityContext, flowParameters);

					result = new PagingIterable<>(flowResult);

					if (returnContent) {

						// timing..
						double queryTimeEnd = System.nanoTime();

						DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
						result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

						processResult(securityContext, request, response, result, depth, resource.isCollectionResource());
					}

					response.setStatus(HttpServletResponse.SC_OK);

				}

				tx.success();
			}

		} catch (FrameworkException frameworkException) {

			writeException(response, frameworkException);

		} catch (Throwable t) {

			logger.warn("Exception in GET (URI: {})", securityContext != null ? securityContext.getCompoundRequestURI() : "(null SecurityContext)");
			logger.warn(" => Error thrown: ", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
			response.getWriter().append(RestMethodResult.jsonError(code, "Exception in GET: " + t.getMessage()));

		} finally {

			try {
				//response.getWriter().flush();
				response.getWriter().close();

			} catch (Throwable t) {

				logger.warn("Unable to flush and close response: {}", t.getMessage());
			}

		}

	}

	@Override
	protected void writeHtml(final SecurityContext securityContext, final HttpServletResponse response, final ResultStream result, final String baseUrl, final int nestingDepth, final boolean wrapSingleResultInArray) throws FrameworkException, IOException {

		final App app                          = StructrApp.getInstance(securityContext);
		final boolean indentJson               = Settings.JsonIndentation.getValue();
		final StreamingFlowWriter flowStreamer = new StreamingFlowWriter(propertyView, indentJson, nestingDepth, wrapSingleResultInArray);

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
		throw new UnsupportedOperationException("POST is not supported by the FlowServlet");
	}

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		throw new UnsupportedOperationException("PUT is not supported by the FlowServlet");
	}

	protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		throw new UnsupportedOperationException("PATCH is not supported by the FlowServlet");
	}

}
