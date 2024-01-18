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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.RequestKeywords;
import org.structr.common.ResultTransformer;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.notion.Notion;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotFoundException;
import org.structr.schema.SchemaHelper;

/**
 *
 */
public class CollectionResourceHandler extends RESTCallHandler {

	private static final Logger logger = LoggerFactory.getLogger(TypeResource.class.getName());

	private ResultTransformer virtualType   = null;
	private Class entityClass               = null;
	private String typeName                 = null;
	private boolean isNode                  = true;

	public CollectionResourceHandler(final RESTCall call, final Class entityClass, final String typeName, final boolean isNode) {

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

			if (virtualType != null) {

				final ResultStream untransformedResult = query
					.includeHidden(includeHidden)
					.publicOnly(publicOnly)
					.sort(sortOrder)
					.getResultStream();

				return virtualType.transformOutput(securityContext, entityClass, untransformedResult);

			} else {

				return query
					.includeHidden(includeHidden)
					.publicOnly(publicOnly)
					.sort(sortOrder)
					.pageSize(pageSize)
					.page(page)
					.getResultStream();
			}

		} else {

			logger.warn("type was null");
		}

		return PagingIterable.EMPTY_ITERABLE;
	}

	@Override
	public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

		// virtual type?
		if (virtualType != null) {
			virtualType.transformInput(securityContext, entityClass, propertySet);
		}

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

				final NodeInterface sourceNode        = identifyStartNode(securityContext, template, propertySet);
				final NodeInterface targetNode        = identifyEndNode(securityContext, template, propertySet);
				final PropertyMap properties          = PropertyMap.inputTypeToJavaType(securityContext, entityClass, propertySet);
				RelationshipInterface newRelationship = null;

				if (sourceNode == null) {
					errorBuffer.add(new EmptyPropertyToken(entityClass.getSimpleName(), template.getSourceIdProperty().jsonName()));
				}

				if (targetNode == null) {
					errorBuffer.add(new EmptyPropertyToken(entityClass.getSimpleName(), template.getTargetIdProperty().jsonName()));
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

		final RestMethodResult result                = new RestMethodResult(HttpServletResponse.SC_OK);
		final App app                                = StructrApp.getInstance(securityContext);
		final Iterator<Map<String, Object>> iterator = propertySets.iterator();
		final int batchSize                          = intOrDefault(RequestKeywords.BatchSize.keyword(), 1000);
		int overallCount                             = 0;

		while (iterator.hasNext()) {

			try (final Tx tx = app.tx()) {

				int count = 0;

				while (iterator.hasNext() && count++ < batchSize) {

					final Map<String, Object> propertySet = iterator.next();
					Class localType                       = entityClass;

					overallCount++;

					// determine type of object
					final Object typeSource = propertySet.get("type");
					if (typeSource != null && typeSource instanceof String) {

						final String typeString = (String)typeSource;

						Class type = SchemaHelper.getEntityClassForRawType(typeString);
						if (type != null) {

							localType = type;
						}
					}

					// virtual type?
					if (virtualType != null) {
						virtualType.transformInput(securityContext, localType, propertySet);
					}

					// find object by id, apply PATCH
					final Object idSource = propertySet.get("id");
					if (idSource != null) {

						if (idSource instanceof String) {

							final String id       = (String)idSource;
							final GraphObject obj = app.get(localType, id);

							if (obj != null) {

								// test
								localType = obj.getClass();

								propertySet.remove("id");

								final PropertyMap data = PropertyMap.inputTypeToJavaType(securityContext, localType, propertySet);

								obj.setProperties(securityContext, data);

							} else {

								throw new NotFoundException("Object with ID " + id + " not found.");
							}

						} else {

							throw new FrameworkException(422, "Invalid PATCH input, object id must be of type string.");
						}

					} else {

						createNode(securityContext, entityClass, typeName, propertySet);
					}
				}

				logger.info("Committing PATCH transaction batch, {} objects processed.", overallCount);

				tx.success();
			}
		}

		return result;
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

	private NodeInterface identifyStartNode(final SecurityContext securityContext, final Relation template, final Map<String, Object> properties) throws FrameworkException {

		final Property<String> sourceIdProperty = template.getSourceIdProperty();
		final Class sourceType                  = template.getSourceType();
		final Notion notion                     = template.getStartNodeNotion();

		notion.setType(sourceType);

		PropertyKey startNodeIdentifier = notion.getPrimaryPropertyKey();

		if (startNodeIdentifier != null) {

			Object identifierValue = properties.get(startNodeIdentifier.dbName());

			properties.remove(sourceIdProperty.dbName());

			return (NodeInterface)notion.getAdapterForSetter(securityContext).adapt(identifierValue);

		}

		return null;
	}

	private NodeInterface identifyEndNode(final SecurityContext securityContext, final Relation template, final Map<String, Object> properties) throws FrameworkException {

		final Property<String> targetIdProperty = template.getTargetIdProperty();
		final Class targetType                  = template.getTargetType();
		final Notion notion                     = template.getEndNodeNotion();

		notion.setType(targetType);

		final PropertyKey endNodeIdentifier = notion.getPrimaryPropertyKey();
		if (endNodeIdentifier != null) {

			Object identifierValue = properties.get(endNodeIdentifier.dbName());

			properties.remove(targetIdProperty.dbName());

			return (NodeInterface)notion.getAdapterForSetter(securityContext).adapt(identifierValue);

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

	private Query createQuery(final App app, final Class type, final boolean isNode) {

		if (isNode) {

			return app.nodeQuery(type);
		}

		return app.relationshipQuery(type);
	}
}
