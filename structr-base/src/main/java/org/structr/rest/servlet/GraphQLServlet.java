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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.Tx;
import org.structr.core.graphql.GraphQLRequest;
import org.structr.rest.RestMethodResult;
import org.structr.rest.adapter.FrameworkExceptionGSONAdapter;
import org.structr.rest.serialization.GraphQLWriter;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.schema.SchemaService;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A servlet that implements the structr graphQL endpoint.
 */
public class GraphQLServlet extends AbstractServletBase implements HttpServiceServlet {

	public static final int DEFAULT_VALUE_PAGE_SIZE                     = 20;
	private static final Logger logger                                  = LoggerFactory.getLogger(GraphQLServlet.class.getName());

	// final fields
	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public String getModuleName() {
		return "graphql";
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			setCustomResponseHeaders(response);

			assertInitialized();

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			final String query = request.getParameter("query");

			handleGraphQLRequest(request, response, query);

		} catch (FrameworkException fex) {

			// set status & write JSON output
			response.setStatus(fex.getStatus());

			getGson().toJson(fex, response.getWriter());

			response.getWriter().println();
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

			setCustomResponseHeaders(response);

			assertInitialized();

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");

			// get reader before initalizing security context
			final String query = IOUtils.toString(request.getReader());

			handleGraphQLRequest(request, response, query);

		} catch (FrameworkException fex) {

			// set status & write JSON output
			response.setStatus(fex.getStatus());

			getGson().toJson(fex, response.getWriter());

			response.getWriter().println();
		}
	}

	// ----- private methods -----
	private void handleGraphQLRequest(final HttpServletRequest request, final HttpServletResponse response, final String query) throws IOException, FrameworkException {

		final SecurityContext securityContext;
		final Authenticator authenticator;

		try {

			setCustomResponseHeaders(response);

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {

				authenticator   = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);

				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			if (securityContext != null) {

				RuntimeEventLog.graphQL(query, securityContext.getUser(false));

				// isolate write output
				try (final Tx tx = app.tx()) {

					final Document doc = GraphQLRequest.parse(new Parser(), query);
					if (doc != null) {

						final List<ValidationError> errors = new Validator().validateDocument(SchemaService.getGraphQLSchema(), doc);
						if (errors.isEmpty()) {

							// no validation errors in query, do request
							final GraphQLWriter graphQLWriter  = new GraphQLWriter(true);

							// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
							response.setContentType("application/json; charset=utf-8");

							final Writer writer = response.getWriter();

							graphQLWriter.stream(securityContext, writer, new GraphQLRequest(securityContext, doc, query));
							writer.append("\n");    // useful newline

						} else {

							final Map<String, Object> map = new LinkedHashMap<>();
							final Writer writer           = response.getWriter();
							final Gson gson               = getGson();

							map.put("errors", errors);

							gson.toJson(map, writer);

							writer.append("\n");    // useful newline

							// send 422 status
							response.setStatus(422);
						}
					}

					tx.success();
				}
			}

		} catch (FrameworkException frameworkException) {

			// set status & write JSON output
			response.setStatus(frameworkException.getStatus());
			getGson().toJson(frameworkException, response.getWriter());
			response.getWriter().println();

		} catch (IllegalStateException | IllegalArgumentException iex) {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("code", 422);
			map.put("message", iex.getMessage());

			// set status & write JSON output
			response.setStatus(422);
			getGson().toJson(map, response.getWriter());
			response.getWriter().println();

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

	private Gson getGson() {

		return new GsonBuilder()
			.serializeNulls()
			.setPrettyPrinting()
			.registerTypeHierarchyAdapter(FrameworkException.class, new FrameworkExceptionGSONAdapter())
			.create();
	}
}
