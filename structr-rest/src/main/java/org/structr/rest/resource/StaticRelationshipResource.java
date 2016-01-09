/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Collection;
import org.structr.common.PagingHelper;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;

import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.OtherNodeTypeRelationFilter;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.core.property.RelationProperty;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
//~--- JDK imports ------------------------------------------------------------

//~--- classes ----------------------------------------------------------------
/**
 *
 *
 */
public class StaticRelationshipResource extends SortableResource {

	private static final Logger logger = Logger.getLogger(StaticRelationshipResource.class.getName());

	//~--- fields ---------------------------------------------------------
	TypeResource typeResource = null;
	TypedIdResource typedIdResource = null;
	PropertyKey propertyKey = null;

	//~--- constructors ---------------------------------------------------constructors
	public StaticRelationshipResource(final SecurityContext securityContext, final TypedIdResource typedIdResource, final TypeResource typeResource) {

		this.securityContext = securityContext;
		this.typedIdResource = typedIdResource;
		this.typeResource = typeResource;
		this.propertyKey = findPropertyKey(typedIdResource, typeResource);
	}

	//~--- methods --------------------------------------------------------
	@Override
	public Result doGet(final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {

		// ok, source node exists, fetch it
		final GraphObject sourceEntity = typedIdResource.getEntity();
		if (sourceEntity != null) {

			// first try: look through existing relations
			if (propertyKey == null) {

				if (sourceEntity instanceof NodeInterface) {

					if (!typeResource.isNode) {

						final NodeInterface source = (NodeInterface) sourceEntity;
						final Node sourceNode = source.getNode();
						final Class relationshipType = typeResource.entityClass;
						final Relation relation = AbstractNode.getRelationshipForType(relationshipType);
						final Class destNodeType = relation.getOtherType(typedIdResource.getEntityClass());
						final Set partialResult = new LinkedHashSet<>(typeResource.doGet(sortKey, sortDescending, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null).getResults());

						// filter list according to end node type
						final Set<GraphObject> set = Iterables.toSet(Iterables.filter(new OtherNodeTypeRelationFilter(securityContext, sourceNode, destNodeType), source.getRelationships(relationshipType)));

						// intersect partial result with result list
						set.retainAll(partialResult);

						final List<GraphObject> finalResult = new LinkedList<>(set);

						// sort after merge
						applyDefaultSorting(finalResult, sortKey, sortDescending);

						// return result
						return new Result(PagingHelper.subList(finalResult, pageSize, page, offsetId), finalResult.size(), isCollectionResource(), isPrimitiveArray());

					} else {

						// what here?
						throw new NotFoundException();
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

						final Set<GraphObject> propertyResults = new LinkedHashSet<>();

						// fill set with data
						for (final GraphObject obj : ((Iterable<GraphObject>) value)) {
							propertyResults.add(obj);
						}

						final List<GraphObject> finalResult = new LinkedList<>(propertyResults);
						applyDefaultSorting(finalResult, sortKey, sortDescending);

						// return result
						return new Result(PagingHelper.subList(finalResult, pageSize, page, offsetId), finalResult.size(), isCollectionResource(), isPrimitiveArray());

					} else if (value instanceof GraphObject) {

						return new Result((GraphObject)value, isPrimitiveArray());

					} else {

						logger.log(Level.INFO, "Found object {0}, but will not return as it is no graph object or iterable", value);

					}
				}

			}
		}

		return Result.EMPTY_RESULT;
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		final List<? extends GraphObject> results = typedIdResource.doGet(null, false, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null).getResults();
		final App app = StructrApp.getInstance(securityContext);

		if (results != null) {

			// fetch static relationship definition
			if (propertyKey != null && propertyKey instanceof RelationProperty) {

				final GraphObject sourceEntity = typedIdResource.getEntity();
				if (sourceEntity != null) {

					if (propertyKey.isReadOnly()) {

						logger.log(Level.INFO, "Read-only property on {1}: {0}", new Object[]{sourceEntity.getClass(), typeResource.getRawType()});
						return new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

					}

					final List<GraphObject> nodes = new LinkedList<>();

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

		if (sourceNode != null && propertyKey != null && propertyKey instanceof RelationProperty) {

			final RelationProperty relationProperty = (RelationProperty) propertyKey;
			final Class sourceNodeType = sourceNode.getClass();
			NodeInterface newNode = null;

			if (propertyKey.isReadOnly()) {

				logger.log(Level.INFO, "Read-only property on {0}: {1}", new Object[]{sourceNodeType, typeResource.getRawType()});

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

				final RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
				result.addHeader("Location", buildLocationHeader(newNode));

				return result;
			}

		} else {

			// look for methods that have an @Export annotation
			final GraphObject entity = typedIdResource.getIdResource().getEntity();
			final Class entityType   = typedIdResource.getEntityClass();
			final String methodName  = typeResource.getRawType();

			if (entity != null && entityType != null && methodName != null) {

				final Object obj = entity.invokeMethod(methodName, propertySet, true);

				if (obj instanceof RestMethodResult) {

					return (RestMethodResult)obj;

				} else {

					final RestMethodResult result = new RestMethodResult(200);

					// unwrap nested object(s)
					unwrapTo(obj, result);

					return result;
				}
			}
		}

		throw new IllegalPathException();
	}

	private void unwrapTo(final Object source, final RestMethodResult result) {

		if (source != null) {

			final Object unwrapped = Context.jsToJava(source, ScriptRuntime.ObjectClass);
			if (unwrapped.getClass().isArray()) {

				for (final Object element : (Object[])unwrapped) {
					unwrapTo(element, result);
				}

			} else if (unwrapped instanceof Collection) {

				for (final Object element : (Collection)unwrapped) {
					unwrapTo(element, result);
				}

			} else if (unwrapped instanceof GraphObject) {

				result.addContent((GraphObject)unwrapped);
			}
		}
	}

	@Override
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) {
		return false;
	}

	@Override
	public Resource tryCombineWith(final Resource next) throws FrameworkException {

		if (next instanceof TypeResource) {

			throw new IllegalPathException();

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
		return true;
	}

	@Override
	public String getResourceSignature() {
		return typedIdResource.getResourceSignature().concat("/").concat(typeResource.getResourceSignature());
	}
}
