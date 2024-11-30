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

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.notion.Notion;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.GraphObject;
import org.structr.core.traits.NodeTrait;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotFoundException;

/**
 *
 */
public class CollectionResourceHandler extends RESTCallHandler {

	private static final Logger logger = LoggerFactory.getLogger(TypeResource.class.getName());

	private Traits entityClass = null;
	private String typeName    = null;
	private boolean isNode     = true;

	public CollectionResourceHandler(final RESTCall call, final Traits entityClass, final String typeName, final boolean isNode) {

		super(call);

		this.entityClass       = entityClass;
		this.typeName          = typeName;
		this.isNode            = isNode;
	}

	@Override
	public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		boolean includeHidden   = true;
		boolean publicOnly      = false;

		if (typeName != null) {

			if (entityClass == null) {
				throw new NotFoundException("Type " + typeName + " does not exist");
			}

			final Query query = createQuery(StructrApp.getInstance(securityContext), entityClass, isNode);

			collectSearchAttributes(securityContext, entityClass, query);

			return query
				.includeHidden(includeHidden)
				.publicOnly(publicOnly)
				.sort(sortOrder)
				.pageSize(pageSize)
				.page(page)
				.getResultStream();

		} else {

			logger.warn("type was null");
		}

		return PagingIterable.EMPTY_ITERABLE;
	}

	@Override
	public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

		if (isNode) {

			final RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
			final NodeInterface newNode   = createNode(securityContext, entityClass, typeName, propertySet);

			if (newNode != null) {

				result.addHeader("Location", buildLocationHeader(securityContext, newNode));
				result.addContent(newNode.getUuid());
			}

			// finally: return 201 Created
			return result;

		} else {

			final App app                         = StructrApp.getInstance(securityContext);
			final Relation template               = getRelationshipTemplate();
			final ErrorBuffer errorBuffer         = new ErrorBuffer();

			if (template != null) {

				final NodeTrait sourceNode        = identifyStartNode(securityContext, template, propertySet);
				final NodeTrait targetNode        = identifyEndNode(securityContext, template, propertySet);
				final PropertyMap properties          = PropertyMap.inputTypeToJavaType(securityContext, entityClass, propertySet);
				RelationshipInterface newRelationship = null;

				if (sourceNode == null) {
					errorBuffer.add(new EmptyPropertyToken(entityClass.getName(), template.getSourceIdProperty().jsonName()));
				}

				if (targetNode == null) {
					errorBuffer.add(new EmptyPropertyToken(entityClass.getName(), template.getTargetIdProperty().jsonName()));
				}

				if (errorBuffer.hasError()) {
					throw new FrameworkException(422, "Source node ID and target node ID of relationship must be set", errorBuffer);
				}

				template.ensureCardinality(securityContext, sourceNode, targetNode);

				newRelationship = app.create(sourceNode, targetNode, entityClass, properties);

				RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
				if (newRelationship != null) {

					result.addHeader("Location", buildLocationHeader(securityContext, newRelationship));
					result.addContent(newRelationship.getUuid());
				}

				// finally: return 201 Created
				return result;
			}

			// shouldn't happen
			throw new NotFoundException("Type" + typeName + " does not exist");
		}
	}

	@Override
	public RestMethodResult doPatch(final SecurityContext securityContext, final List<Map<String, Object>> propertySets) throws FrameworkException {
		return genericPatch(securityContext, propertySets, entityClass, typeName);
	}

	@Override
	public RestMethodResult doPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {
		// override super method to provide more specific error message
		throw new IllegalMethodException("PUT not allowed on ‛" + typeName + "‛ collection resource", getAllowedHttpMethodsForOptionsCall());
	}

	@Override
	public RestMethodResult doDelete(final SecurityContext securityContext) throws FrameworkException {
		return genericDelete(securityContext);
	}

	@Override
	public Class getEntityClass(final SecurityContext securityContext) {
		return entityClass;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Set<String> getAllowedHttpMethodsForOptionsCall() {
		return Set.of("DELETE", "GET", "OPTIONS", "PATCH", "POST");
	}

	// ----- private methods -----
	private Relation getRelationshipTemplate() {

		try {

			return (Relation)entityClass.getDeclaredConstructor().newInstance();

		} catch (Throwable t) {

		}

		return null;
	}

	private NodeTrait identifyStartNode(final SecurityContext securityContext, final Relation template, final Map<String, Object> properties) throws FrameworkException {

		final Property<String> sourceIdProperty = template.getSourceIdProperty();
		final Trait<?> sourceType               = template.getSourceType();
		final Notion notion                     = template.getStartNodeNotion();

		notion.setType(sourceType);

		PropertyKey startNodeIdentifier = notion.getPrimaryPropertyKey();

		if (startNodeIdentifier != null) {

			Object identifierValue = properties.get(startNodeIdentifier.dbName());

			properties.remove(sourceIdProperty.dbName());

			return (NodeTrait)notion.getAdapterForSetter(securityContext).adapt(identifierValue);

		}

		return null;
	}

	private NodeTrait identifyEndNode(final SecurityContext securityContext, final Relation template, final Map<String, Object> properties) throws FrameworkException {

		final Property<String> targetIdProperty = template.getTargetIdProperty();
		final Trait<?> targetType               = template.getTargetType();
		final Notion notion                     = template.getEndNodeNotion();

		notion.setType(targetType);

		final PropertyKey endNodeIdentifier = notion.getPrimaryPropertyKey();
		if (endNodeIdentifier != null) {

			Object identifierValue = properties.get(endNodeIdentifier.dbName());

			properties.remove(targetIdProperty.dbName());

			return (NodeTrait)notion.getAdapterForSetter(securityContext).adapt(identifierValue);

		}

		return null;
	}

	private int intOrDefault(final String source, final int defaultValue) {

		if (source != null) {

			try {

				return Integer.parseInt(source);

			} catch (Throwable t) {}

		}

		return defaultValue;
	}

	private <T extends GraphObject> Query<T> createQuery(final App app, final Traits type, final boolean isNode) {

		if (isNode) {

			return app.nodeQuery(type);
		}

		return app.relationshipQuery(type);
	}
}
