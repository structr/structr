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
package org.structr.core.graphql;

import graphql.language.*;
import graphql.parser.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

import java.util.*;

/**
 * A GraphQL request that contains a list of query objects.
 */
public class GraphQLRequest {

	private static final Logger logger = LoggerFactory.getLogger(GraphQLRequest.class);

	private final List<GraphQLQuery> queries = new ArrayList<>();
	private boolean hasSchemaQuery           = false;
	private String originalQuery             = null;

	public GraphQLRequest(final SecurityContext securityContext, final Document document, final String originalQuery) throws FrameworkException {

		this.originalQuery = originalQuery;

		initialize(securityContext, document);
		checkSchemaRequest();
	}

	public boolean hasSchemaQuery() {
		return hasSchemaQuery;
	}

	public Iterable<GraphQLQuery> getQueries() {
		return queries;
	}

	public String getOriginalQuery() {
		return originalQuery;
	}

	public static Document parse(final Parser parser, final String query) throws FrameworkException {

		try {

			return parser.parseDocument(query);

		} catch (Throwable t) {

			String message = t.getMessage();
			if (message == null) {

				message = t.getClass().getName();

				if (t instanceof ParseCancellationException) {

					final Throwable cause = t.getCause();
					if (cause instanceof RecognitionException) {

						final RecognitionException err = (RecognitionException)cause;
						final Token offendingToken     = err.getOffendingToken();

						if (offendingToken != null) {

							final int line    = offendingToken.getLine();
							final int column  = offendingToken.getCharPositionInLine();
							final String text = offendingToken.getText();

							message = "Parse error at " + text + " in line " + line + ", column " + column;
						}
					}

				}
			}

			final FrameworkException fex   = new FrameworkException(422, message);
			final Map<String, String> data = new LinkedHashMap<>();

			// do not output an empty array of errors
			fex.setErrorBuffer(null);
			fex.setData(data);

			data.put("query", query);

			throw fex;
		}
	}

	// ----- private methods -----
	private void initialize(final SecurityContext securityContext,  Document document) throws FrameworkException {

		for (final Node child : document.getChildren()) {

			if (child instanceof OperationDefinition) {

				final OperationDefinition operationDefinition = (OperationDefinition)child;
				final SelectionSet selectionSet               = operationDefinition.getSelectionSet();

				for (final Selection selection : selectionSet.getSelections()) {

					if (selection instanceof Field) {

						queries.add(new GraphQLQuery(securityContext, (Field)selection));

					} else {

						logger.warn("Unknown selection set element {} in GraphQL query, ignoring.", selection.getClass());
					}
				}

			} else {

				logger.warn("Unknown document element {} in GraphQL query, ignoring.", child.getClass());
			}
		}
	}

	private void checkSchemaRequest() throws FrameworkException {

		boolean hasDataQuery   = false;

		for (final GraphQLQuery query : queries) {

			if (query.isSchemaQuery()) {

				hasSchemaQuery = true;

			} else {

				hasDataQuery = true;
			}
		}

		if (hasSchemaQuery && hasDataQuery) {

			final FrameworkException fex = new FrameworkException(422, "Unsupported query type, schema and data queries cannot be mixed.");
			final Map<String, String> data = new HashMap<>();

			fex.setData(data);

			data.put("query", originalQuery);

			throw fex;
		}
	}
}
