/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.function;

import graphql.language.Document;
import graphql.parser.Parser;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graphql.GraphQLRequest;
import org.structr.rest.serialization.GraphQLWriter;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class GraphQLFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_GRAPHQL    = "Usage: ${graphql(query[, parse = true ])}. Example: ${graphql(\"{ User { id, type, name, groups { id, name } }}\")}";
	public static final String ERROR_MESSAGE_GRAPHQL_JS = "Usage: ${{Structr.graphql(query[, parse = true ])}}. Example: ${{Structr.graphql(\"{ User { id, type, name, groups { id, name } }}\")}}";

	@Override
	public String getName() {
		return "graphql";
	}

	@Override
	public String getSignature() {
		return "query[, parse = true ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			final String query  = sources[0].toString();
			final boolean parse = (sources.length > 1) ? Boolean.TRUE.equals(sources[1]) : false;
			final SecurityContext securityContext = ctx.getSecurityContext();

			if (query != null && securityContext != null) {

				try {

					return executeGraphQLQuery(securityContext, query, parse);

				} catch (Throwable t) {

					logException(caller, t, sources);
				}
			}

			return "";

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	public static Object executeGraphQLQuery (final SecurityContext securityContext, final String query, final boolean parse) throws IOException, FrameworkException {

		final Document doc = GraphQLRequest.parse(new Parser(), query);
		if (doc != null) {

			final List<ValidationError> errors = new Validator().validateDocument(SchemaService.getGraphQLSchema(), doc);
			if (errors.isEmpty()) {

				final GraphQLWriter graphQLWriter = new GraphQLWriter(false);

				final StringWriter buffer = new StringWriter();

				graphQLWriter.stream(securityContext, buffer, new GraphQLRequest(securityContext, doc, query));

				if (parse) {
					return FromJsonFunction.parseJson(buffer.toString());
				}

				return buffer.toString();

			} else {

				logger.warn("Errors occured while processing GraphQL request: {}", errors);
			}
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GRAPHQL_JS : ERROR_MESSAGE_GRAPHQL);
	}

	@Override
	public String shortDescription() {
		return "Executes the given Graph QL query and returns the results (by default as plain javascript objects, optionally as JSON)";
	}
}