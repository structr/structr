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
package org.structr.rest.resource;


import org.structr.api.graph.Direction;
import org.structr.api.search.SortOrder;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.RelationshipInterface;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.core.graph.NodeInterface;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.schema.SchemaHelper;

/**
 *
 */
public class CollectionRelationshipsResource extends ExactMatchEndpoint {

	public CollectionRelationshipsResource() {

		super(
			RESTParameter.forPattern("type", SchemaNode.schemaNodeNamePattern),
			RESTParameter.forPattern("rel",  "in|out")
		);
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {

		final String typeName  = call.get("type");
		final String direction = call.get("rel");

		if (typeName != null && direction != null) {

			// test if resource class exists
			final Class entityClass = SchemaHelper.getEntityClassForRawType(typeName);
			if (entityClass != null && NodeInterface.class.isAssignableFrom(entityClass)) {

				if ("in".equals(direction)) {

					return new RelationshipsResourceHandler(securityContext, call, entityClass, typeName, Direction.INCOMING);

				} else {

					return new RelationshipsResourceHandler(securityContext, call, entityClass, typeName, Direction.OUTGOING);
				}
			}
		}

		// only return a handler if there is actually a type with the requested name
		return null;
	}

	private class RelationshipsResourceHandler extends CollectionResourceHandler {

		public static final String REQUEST_PARAMETER_FILTER_INTERNAL_RELATIONSHIP_TYPES = "domainOnly";

		private Direction direction = null;

		public RelationshipsResourceHandler(final SecurityContext securityContext, final RESTCall call, final Class entityClass, final String typeName, final Direction direction) {

			super(securityContext, call, StructrApp.getInstance(securityContext).nodeQuery(entityClass), entityClass, typeName, true);

			this.direction = direction;
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			final List<GraphObject> resultList = new LinkedList<>();

			for (final Object obj : super.doGet(sortOrder, pageSize, page)) {

				if (obj instanceof AbstractNode node) {
					List<? extends RelationshipInterface> relationships = null;

					switch (direction) {

						case INCOMING:

							relationships = Iterables.toList(node.getIncomingRelationships());
							break;


						case OUTGOING:

							relationships = Iterables.toList(node.getOutgoingRelationships());
							break;

					}


					if (relationships != null) {

						boolean filterInternalRelationshipTypes = false;

						if (securityContext != null && securityContext.getRequest() != null) {

							final String filterInternal = securityContext.getRequest().getParameter(REQUEST_PARAMETER_FILTER_INTERNAL_RELATIONSHIP_TYPES);
							if (filterInternal != null) {

								filterInternalRelationshipTypes = "true".equals(filterInternal);
							}
						}

						// allow the user to remove internal relationship types from
						// the result set using the request parameter "filterInternal=true"
						if (filterInternalRelationshipTypes) {

							for (final RelationshipInterface rel : relationships) {

								if (!rel.isInternal()) {
									resultList.add(rel);
								}
							}

						} else {

							resultList.addAll(relationships);
						}
					}
				}
			}

			return new PagingIterable<>(getURL(), resultList, pageSize, page);
		}

		@Override
		public boolean isCollection() {
			return true;
		}

		@Override
		public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("POST not allowed on " + getURL());
		}

		@Override
		public Class getEntityClass() {
			return null;
		}
	}
}
