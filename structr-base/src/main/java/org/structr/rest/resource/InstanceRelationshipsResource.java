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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTEndpoint;
import org.structr.api.config.Settings;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.schema.SchemaHelper;
import org.structr.rest.api.parameter.RESTParameter;

/**
 *
 *
 */
public class InstanceRelationshipsResource extends RESTEndpoint {

	public InstanceRelationshipsResource() {

		super(
			RESTParameter.forPattern("type", SchemaNode.schemaNodeNamePattern),
			RESTParameter.forPattern("uuid", Settings.getValidUUIDRegexStringForURLParts()),
			RESTParameter.forPattern("rel",  "in|out")
		);
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {

		final String typeName  = call.get("type");
		final String uuid      = call.get("uuid");
		final String direction = call.get("rel");

		if (typeName != null && uuid != null && direction != null) {

			// test if resource class exists
			final Class entityClass = SchemaHelper.getEntityClassForRawType(typeName);
			if (entityClass != null && NodeInterface.class.isAssignableFrom(entityClass)) {

				if ("in".equals(direction)) {

					return new RelationshipsResourceHandler(securityContext, call, entityClass, typeName, uuid, Direction.INCOMING);

				} else {

					return new RelationshipsResourceHandler(securityContext, call, entityClass, typeName, uuid, Direction.OUTGOING);
				}
			}
		}

		// only return a handler if there is actually a type with the requested name
		return null;
	}

	private class RelationshipsResourceHandler extends RESTCallHandler {

		public static final String REQUEST_PARAMETER_FILTER_INTERNAL_RELATIONSHIP_TYPES = "domainOnly";
		private static final Logger logger = LoggerFactory.getLogger(InstanceRelationshipsResource.class.getName());

		private Class entityClass   = null;
		private String typeName     = null;
		private String uuid         = null;
		private Direction direction = null;

		public RelationshipsResourceHandler(final SecurityContext securityContext, final RESTCall call, final Class entityClass, final String typeName, final String uuid, final Direction direction) {

			super(securityContext, call);

			this.entityClass = entityClass;
			this.typeName    = typeName;
			this.uuid        = uuid;
			this.direction   = direction;
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			final List<GraphObject> resultList = new LinkedList<>();
			final GraphObject obj              = getEntity(securityContext, entityClass, typeName, uuid);

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
