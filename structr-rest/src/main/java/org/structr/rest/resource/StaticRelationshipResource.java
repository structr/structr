/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import java.lang.reflect.InvocationTargetException;
import org.structr.common.PagingHelper;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;

import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
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
//~--- JDK imports ------------------------------------------------------------

//~--- classes ----------------------------------------------------------------
/**
 *
 * @author Christian Morgner
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
					}
				}

			} else {
//
//			// second try: property key
//			if (propertyKey != null) {

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

						/*
						 if (typeResource.getEntityClass() != null) {

						 final Set<GraphObject> typeResourceResults = new LinkedHashSet<>(typeResource.doGet(null, sortDescending, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null).getResults());

						 // merge list with results from type resource (which includes request parameter based filtering)
						 propertyResults.retainAll(typeResourceResults);
						 }
						 */
						final List<GraphObject> finalResult = new LinkedList<>(propertyResults);
						applyDefaultSorting(finalResult, sortKey, sortDescending);

						// return result
						return new Result(PagingHelper.subList(finalResult, pageSize, page, offsetId), finalResult.size(), isCollectionResource(), isPrimitiveArray());
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

						nodes.add(app.get(obj.toString()));
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
		final App app = StructrApp.getInstance(securityContext);

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

				/*
				 * FIXME: is this needed at all??
				 *
				 // the notion that is defined for this relationship can deserialize
				 // objects with a single key (uuid for example), and the POSTed
				 // property set contains value(s) for this key, so we only need
				 // to create relationships
				 final Object keySource = propertySet.get(primaryPropertyKey.jsonName());
				 if (keySource != null) {

				 if (keySource instanceof Collection) {

				 sourceNode.setProperty(propertyKey, notion.getCollectionAdapterForSetter(securityContext).adapt(keySource));

				 } else {

				 sourceNode.setProperty(propertyKey, notion.getAdapterForSetter(securityContext).adapt(keySource));
				 }

				 /*
				 GraphObject otherNode = null;

				 if (keySource instanceof Collection) {

				 final Collection collection = (Collection) keySource;

				 for (final Object key : collection) {

				 otherNode = deserializationStrategy.adapt(key);

				 if (otherNode != null && otherNode instanceof AbstractNode) {

				 relationshipProperty.createRelationship(securityContext, sourceNode, (AbstractNode)otherNode);

				 } else {

				 logger.log(Level.WARNING, "Relationship end node has invalid type {0}", otherNode.getClass().getName());
				 }

				 }

				 } else {

				 // create a single relationship
				 otherNode = deserializationStrategy.adapt(keySource);

				 if (otherNode != null && otherNode instanceof AbstractNode) {

				 relationshipProperty.createRelationship(securityContext, sourceNode, (AbstractNode)otherNode);

				 } else {

				 logger.log(Level.WARNING, "Relationship end node has invalid type {0}", otherNode.getClass().getName());

				 }
				 }

				 return otherNode;

				 } else {

				 logger.log(Level.INFO, "Key {0} not found in {1}", new Object[] { primaryPropertyKey.jsonName(), propertySet.toString() });

				 }
				 */
			} else {

				// the notion can not deserialize objects with a single key, or the POSTed propertySet did not contain a key to deserialize,
				// so we create a new node from the POSTed properties and link the source node to it. (this is the "old" implementation)
				newNode = typeResource.createNode(propertySet);
				if (newNode != null) {

					relationProperty.addSingleElement(securityContext, sourceNode, newNode);
				}
			}

			if (newNode != null) {

				RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
				result.addHeader("Location", buildLocationHeader(newNode));

				return result;
			}

		} else {

			// look for methods that have an @Export annotation
			GraphObject entity = typedIdResource.getIdResource().getEntity();
			Class entityType = typedIdResource.getEntityClass();
			String methodName = typeResource.getRawType();

			if (entity != null && entityType != null && methodName != null) {

				for (Method method : StructrApp.getConfiguration().getExportedMethodsForType(entityType)) {

					if (methodName.equals(method.getName())) {

						if (method.getAnnotation(Export.class) != null) {

							try {
								Object[] parameters = extractParameters(propertySet, method.getParameterTypes());
								return (RestMethodResult) method.invoke(entity, parameters);

							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException t) {

								if (t instanceof FrameworkException) {

									throw (FrameworkException) t;

								} else if (t.getCause() instanceof FrameworkException) {

									throw (FrameworkException) t.getCause();

								} else {

									logger.log(Level.WARNING, "Unable to call RPC method {0}: {1}", new Object[]{methodName, t.getMessage()});
								}
							}
						}
					}
				}
			}

		}

		throw new IllegalPathException();
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

	// ----- private methods -----
	private <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R getRelationshipForType(final Class<R> type) {

		try {

			return type.newInstance();

		} catch (Throwable t) {

			// TODO: throw meaningful exception here,
			// should be a RuntimeException that indicates
			// wrong use of Relationships etc.
			t.printStackTrace();
		}

		return null;
	}

	private Object[] extractParameters(Map<String, Object> properties, Class[] parameterTypes) {

		List<Object> values = new ArrayList<>(properties.values());
		List<Object> parameters = new ArrayList<>();
		int index = 0;

		// only try to convert when both lists have equal size
		if (values.size() == parameterTypes.length) {

			for (Class parameterType : parameterTypes) {

				Object value = convert(values.get(index++), parameterType);
				if (value != null) {

					parameters.add(value);
				}
			}
		}

		return parameters.toArray(new Object[0]);
	}

	/*
	 * Tries to convert the given value into an object
	 * of the given type, using an intermediate type
	 * of String for the conversion.
	 */
	private Object convert(Object value, Class type) {

		Object convertedObject = null;

		if (type.equals(String.class)) {

			// strings can be returned immediately
			return value.toString();

		} else if (value instanceof Number) {

			Number number = (Number) value;

			if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
				return number.intValue();

			} else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
				return number.longValue();

			} else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
				return number.doubleValue();

			} else if (type.equals(Float.class) || type.equals(Float.TYPE)) {
				return number.floatValue();

			} else if (type.equals(Short.class) || type.equals(Integer.TYPE)) {
				return number.shortValue();

			} else if (type.equals(Byte.class) || type.equals(Byte.TYPE)) {
				return number.byteValue();

			}

		} else if (value instanceof List) {

			return value;
		}

		// fallback
		try {

			Method valueOf = type.getMethod("valueOf", String.class);
			if (valueOf != null) {

				convertedObject = valueOf.invoke(null, value.toString());

			} else {

				logger.log(Level.WARNING, "Unable to find static valueOf method for type {0}", type);
			}

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Unable to deserialize value {0} of type {1}, Class has no static valueOf method.", new Object[]{value, type});
		}

		return convertedObject;
	}
}
