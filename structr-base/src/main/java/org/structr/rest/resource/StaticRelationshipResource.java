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


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.Node;
import org.structr.api.search.SortOrder;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.OtherNodeTypeRelationFilter;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.core.property.*;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.schema.action.EvaluationHints;

import java.util.*;

/**
 *
 */
public class StaticRelationshipResource extends WrappingResource {

	private static final Logger logger = LoggerFactory.getLogger(StaticRelationshipResource.class.getName());

	TypeResource typeResource       = null;
	TypedIdResource typedIdResource = null;
	PropertyKey propertyKey         = null;
	boolean isCollectionResource    = true;

	public StaticRelationshipResource(final SecurityContext securityContext, final TypedIdResource typedIdResource, final TypeResource typeResource) {

		this.securityContext = securityContext;
		this.typedIdResource = typedIdResource;
		this.typeResource    = typeResource;
		this.propertyKey     = findPropertyKey(typedIdResource, typeResource);
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, final int pageSize, final int page) throws FrameworkException {

		// ok, source node exists, fetch it
		final GraphObject sourceEntity = typedIdResource.getEntity();
		if (sourceEntity != null) {

			// first try: look through existing relations
			if (propertyKey == null) {

				if (sourceEntity instanceof NodeInterface) {

					if (!typeResource.isNode) {

						final NodeInterface source   = (NodeInterface) sourceEntity;
						final Node sourceNode        = source.getNode();
						final Class relationshipType = typeResource.entityClass;
						final Relation relation      = AbstractNode.getRelationshipForType(relationshipType);
						final Class destNodeType     = relation.getOtherType(typedIdResource.getEntityClass());
						final Set partialResult      = new LinkedHashSet<>(Iterables.toList(typeResource.doGet(sortOrder, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE)));

						// filter list according to end node type
						final Set<GraphObject> set = Iterables.toSet(Iterables.filter(new OtherNodeTypeRelationFilter(securityContext, sourceNode, destNodeType), source.getRelationships(relationshipType)));

						// intersect partial result with result list
						set.retainAll(partialResult);

						final List<GraphObject> finalResult = new ArrayList<>(set);

						// sort after merge
						applyDefaultSorting(finalResult, sortOrder);

						// return result
						//return new ResultStream(PagingHelper.subList(finalResult, pageSize, page), isCollectionResource(), isPrimitiveArray());
						return new PagingIterable<>("/" + getUriPart(), finalResult, pageSize, page);

					} else {

						// what here?
						throw new NotFoundException("Cannot access relationship collection " + typeResource.getRawType());
					}
				}

			} else {

				Query query = typeResource.query;
				if (query == null) {

					query = StructrApp.getInstance(securityContext).nodeQuery();
				}

				// use search context from type resource
				typeResource.collectSearchAttributes(query);

				final Predicate<GraphObject> predicate = query.toPredicate();
				final Object value = sourceEntity.getProperty(propertyKey, predicate);

				if (value != null) {

					if (value instanceof Iterable) {

						final Set<Object> propertyResults = new LinkedHashSet<>();
						Iterator<Object> iter = ((Iterable<Object>) value).iterator();
						boolean iterableContainsGraphObject = false;

						while (iter.hasNext()) {

							Object obj = iter.next();
							propertyResults.add(obj);

							if (obj != null && !iterableContainsGraphObject) {

								if (obj instanceof GraphObject) {

									iterableContainsGraphObject = true;

								}

							}

						}

						int rawResultCount = propertyResults.size();

						if (rawResultCount > 0 && !iterableContainsGraphObject) {

							GraphObjectMap gObject = new GraphObjectMap();

							gObject.setProperty(new ArrayProperty(this.typeResource.rawType, Object.class), propertyResults.toArray());

							return new PagingIterable<>("/" + getUriPart(), Arrays.asList(gObject));

							//final ResultStream r = new ResultStream(gObject, true);
							//return r;

						}

						final List<GraphObject> finalResult = new ArrayList<>();

						propertyResults.forEach(
							v -> finalResult.add((GraphObject) v)
						);

						applyDefaultSorting(finalResult, sortOrder);

						// return result
						//return new ResultStream(PagingHelper.subList(finalResult, pageSize, page), isCollectionResource(), isPrimitiveArray());
						return new PagingIterable<>("/" + getUriPart(), finalResult, pageSize, page);

					} else if (value instanceof GraphObject) {

						return new PagingIterable<>("/" + getUriPart(), Arrays.asList(value));
						//return new ResultStream((GraphObject) value, isPrimitiveArray());

					} else {

						GraphObjectMap gObject = new GraphObjectMap();
						PropertyKey key;
						String keyName = this.typeResource.rawType;

						//FIXME: Dynamically resolve all property types and their result count
						if (value instanceof String) {
							key = new StringProperty(keyName);
						} else if (value instanceof Integer) {
							key = new IntProperty(keyName);
						} else if (value instanceof Long) {
							key = new LongProperty(keyName);
						} else if (value instanceof Double) {
							key = new DoubleProperty(keyName);
						} else if (value instanceof Boolean) {
							key = new BooleanProperty(keyName);
						} else if (value instanceof Date) {
							key = new DateProperty(keyName);
						} else if (value instanceof String[]) {

							key = new ArrayProperty(keyName, String.class);

						} else {

							key = new GenericProperty(keyName);
						}

						gObject.setProperty(key, value);

						return new PagingIterable<>("/" + getUriPart(), Arrays.asList(gObject));
						//return new ResultStream(gObject, true);

					}
				}

				// check propertyKey to return the right variant of empty result
				if (!(propertyKey instanceof StartNode || propertyKey instanceof EndNode)) {

					return PagingIterable.EMPTY_ITERABLE;
					//return new ResultStream(Collections.EMPTY_LIST, false, true);

				}

			}
		}

		return PagingIterable.EMPTY_ITERABLE;
		//return new ResultStream(Collections.EMPTY_LIST, false, true);
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		final List<? extends GraphObject> results = Iterables.toList(typedIdResource.doGet(null, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE));
		final App app = StructrApp.getInstance(securityContext);

		if (results != null) {

			// fetch static relationship definition
			if (propertyKey != null && propertyKey instanceof RelationProperty) {

				final GraphObject sourceEntity = typedIdResource.getEntity();
				if (sourceEntity != null) {

					if (propertyKey.isReadOnly()) {

						logger.info("Read-only property on {}: {}", new Object[]{sourceEntity.getClass(), typeResource.getRawType()});
						return new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

					}

					final List<GraphObject> nodes = new ArrayList<>();

					// Now add new relationships for any new id: This should be the rest of the property set
					for (final Object obj : propertySet.values()) {

						nodes.add(app.getNodeById(obj.toString()));
					}

					// set property on source node
					sourceEntity.setProperty(propertyKey, nodes);
				}

			}
		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		final GraphObject sourceNode = typedIdResource.getEntity();
		RestMethodResult result = null;

		if (sourceNode != null && propertyKey != null && propertyKey instanceof RelationProperty) {

			final RelationProperty relationProperty = (RelationProperty) propertyKey;
			final Class sourceNodeType = sourceNode.getClass();
			NodeInterface newNode = null;

			if (propertyKey.isReadOnly()) {

				logger.info("Read-only property on {}: {}", new Object[]{sourceNodeType, typeResource.getRawType()});

				return null;
			}

			// fetch notion
			final Notion notion = relationProperty.getNotion();
			final PropertyKey primaryPropertyKey = notion.getPrimaryPropertyKey();

			// apply notion if the property set contains the ID property as the only element
			if (primaryPropertyKey != null && propertySet.containsKey(primaryPropertyKey.jsonName()) && propertySet.size() == 1) {

				// FIXME: what happens here?

			} else {

				// the notion can not deserialize objects with a single key, or the POSTed propertySet did not contain a key to deserialize,
				// so we create a new node from the POSTed properties and link the source node to it. (this is the "old" implementation)
				newNode = typeResource.createNode(propertySet);
				if (newNode != null) {

					relationProperty.addSingleElement(securityContext, sourceNode, newNode);
				}
			}

			if (newNode != null) {

				result = new RestMethodResult(HttpServletResponse.SC_CREATED);
				result.addHeader("Location", buildLocationHeader(newNode));

				return result;
			}

		} else {

			/**
			 * The below code is used when a schema method is called on an entity via REST. The
			 * result of this operation should not be wrapped in an array, so we set the flag
			 * accordingly.
			 */
			this.isCollectionResource = false;

			final GraphObject entity = typedIdResource.getEntity();
			final Class entityType   = entity != null ? entity.getClass() : typedIdResource.getTypeResource().getEntityClass();
			final String methodName  = typeResource.getRawType();

			try {
				final SchemaMethod method = SchemaMethodResource.findMethod(entityType, methodName);
				final String source       = method.getProperty(SchemaMethod.source);

				result = SchemaMethodResource.invoke(securityContext, entity, source, propertySet, methodName, method.getUuid());

			} catch (IllegalPathException ex) {

				// try direct invocation of the schema method on the node type
				result = SchemaMethodResource.wrapInResult(entity.invokeMethod(securityContext, methodName, propertySet, true, new EvaluationHints()));
			}
		}

		if (result == null) {
			throw new IllegalPathException("Illegal path");
		} else {
			return result;
		}
	}

	@Override
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) {
		return false;
	}

	@Override
	public Resource tryCombineWith(final Resource next) throws FrameworkException {

		if (next instanceof TypeResource) {

			throw new IllegalPathException("Unable to resolve URL path, no type resource expected here");

		}

		return super.tryCombineWith(next);
	}

	@Override
	public Class getEntityClass() {
		Class type = typeResource.getEntityClass();
		if (type == null && propertyKey != null) {
			return propertyKey.relatedType();
		}
		return type;
	}

	@Override
	public String getUriPart() {
		return typedIdResource.getUriPart().concat("/").concat(typeResource.getUriPart());
	}

	public TypedIdResource getTypedIdConstraint() {
		return typedIdResource;
	}

	public TypeResource getTypeConstraint() {
		return typeResource;
	}

	@Override
	public boolean isCollectionResource() {
		return isCollectionResource;
	}

	@Override
	public String getResourceSignature() {
		return typedIdResource.getResourceSignature().concat("/").concat(typeResource.getResourceSignature());
	}
}
