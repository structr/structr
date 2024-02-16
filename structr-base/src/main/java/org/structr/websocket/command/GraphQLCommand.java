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
package org.structr.websocket.command;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.graphql.GraphQLQuery;
import org.structr.core.graphql.GraphQLRequest;
import org.structr.schema.SchemaService;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to respond to a GraphQL request
 *
 */
public class GraphQLCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(GraphQLCommand.class.getName());

	static {

		StructrWebSocket.addCommand(GraphQLCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final StructrWebSocket socket          = getWebSocket();
		
		final SecurityContext securityContext  = socket.getSecurityContext();
		final List<GraphObject> result         = new ArrayList<>();
		final String query                     = webSocketData.getNodeDataStringValue("query");

		if (query != null) {
			
			if (securityContext != null) {
				
				try {
					final Document doc = GraphQLRequest.parse(new Parser(), query);
					
					if (doc != null) {

						final List<ValidationError> errors = new Validator().validateDocument(SchemaService.getGraphQLSchema(), doc);
						if (errors.isEmpty()) {

							// no validation errors in query, do request
							result.addAll(createResult(securityContext, new GraphQLRequest(securityContext, doc, query)));
							
						} else {

							final Map<String, Object> map = new LinkedHashMap<>();
							map.put("errors", errors);
							
							logger.warn("Errors occured while processing GraphQL request.");

							getWebSocket().send(MessageBuilder.status().data(map).code(422).message("Errors occured while processing GraphQL request.").build(), true);
							
							return;

						}
					}
				} catch (IOException ioex) {

					logger.warn("Could not process GraphQL request.", ioex);
					getWebSocket().send(MessageBuilder.status().code(422).message(ioex.getMessage()).build(), true);
					
					return;
					
				} catch (FrameworkException ex) {

					logger.warn("Could not process GraphQL request.", ex);
					getWebSocket().send(MessageBuilder.status().code(ex.getStatus()).message(ex.getMessage()).build(), true);
					
					return;

				}

			}			
			
		}

		webSocketData.setResult(result);
		
		// send only over local connection (no broadcast)
		getWebSocket().send(webSocketData, true);
	}

	private List<GraphObject> createResult(final SecurityContext securityContext, final GraphQLRequest request) throws IOException, FrameworkException {
	
		final List<GraphObject> resultList = new ArrayList<>();
		
		if (request.hasSchemaQuery()) {

			// schema query is serialized from GraphQL execution result, doesn't need enclosing JSON object
			for (final GraphQLQuery query : request.getQueries()) {

				if (query.isSchemaQuery()) {

					// use graphql-java schema response
					final String originalQuery   = request.getOriginalQuery();
					final GraphQL graphQL        = GraphQL.newGraphQL(SchemaService.getGraphQLSchema()).build();
					final ExecutionResult result = graphQL.execute(originalQuery);

					if (result != null) {

						resultList.add(GraphObjectMap.fromMap(result.getData()));
					}
				}
			}

		} else {

			for (final GraphQLQuery query : request.getQueries()) {

				if (query.isSchemaQuery()) {

					// use graphql-java schema response
					final String originalQuery   = request.getOriginalQuery();
					final GraphQL graphQL        = GraphQL.newGraphQL(SchemaService.getGraphQLSchema()).build();
					final ExecutionResult result = graphQL.execute(originalQuery);

					if (result != null) {

						resultList.add(GraphObjectMap.fromMap(result.getData()));
					}

				} else {

					//writer.name(query.getFieldName());

					for (final GraphObject object : query.getEntities(securityContext)) {

						resultList.add(object);
					}
				}
			}

		}	
		return resultList;
	}
	
	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "GRAPHQL";
	}
}
