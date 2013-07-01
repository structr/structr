/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.rest.resource;

import org.structr.common.PagingHelper;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TypeToken;
import org.structr.core.Adapter;
import org.structr.core.EntityContext;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.notion.Notion;
import org.structr.core.property.AbstractRelationProperty;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.SystemException;
//~--- JDK imports ------------------------------------------------------------

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class StaticRelationshipResource extends SortableResource {

	private static final Logger logger = Logger.getLogger(StaticRelationshipResource.class.getName());

	//~--- fields ---------------------------------------------------------

	TypeResource typeResource       = null;
	TypedIdResource typedIdResource = null;

	//~--- constructors ---------------------------------------------------constructors

	public StaticRelationshipResource(final SecurityContext securityContext, final TypedIdResource typedIdResource, final TypeResource typeResource) {

		this.securityContext = securityContext;
		this.typedIdResource = typedIdResource;
		this.typeResource    = typeResource;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public Result doGet(final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {

		// ok, source node exists, fetch it
		final AbstractNode sourceNode = typedIdResource.getTypesafeNode();
		if (sourceNode != null) {

			final PropertyKey key = findPropertyKey(typedIdResource, typeResource);
			if (key != null) {

				final Object value = sourceNode.getProperty(key);
				if (value != null) {

					if (value instanceof List) {

						final List<GraphObject> list = (List<GraphObject>)value;
						applyDefaultSorting(list, sortKey, sortDescending);

						//return new Result(list, null, isCollectionResource(), isPrimitiveArray());
						return new Result(PagingHelper.subList(list, pageSize, page, offsetId), list.size(), isCollectionResource(), isPrimitiveArray());

					} else if (value instanceof Iterable) {

						// check type of value (must be an Iterable of GraphObjects in order to proceed here)
						final List<GraphObject> propertyListResult = new LinkedList<GraphObject>();
						final Iterable sourceIterable              = (Iterable) value;

						for (final Object o : sourceIterable) {

							if (o instanceof GraphObject) {

								propertyListResult.add((GraphObject) o);
							}
						}

						applyDefaultSorting(propertyListResult, sortKey, sortDescending);

						//return new Result(propertyListResult, null, isCollectionResource(), isPrimitiveArray());
						return new Result(PagingHelper.subList(propertyListResult, pageSize, page, offsetId), propertyListResult.size(), isCollectionResource(), isPrimitiveArray());

					}
				}

			}
		}

		return Result.EMPTY_RESULT;
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		final List<? extends GraphObject> results = typedIdResource.doGet(null, false, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null).getResults();
		final SearchNodeCommand searchNode        = Services.command(securityContext, SearchNodeCommand.class);

		if (results != null) {

			// fetch static relationship definition
			final PropertyKey key = findPropertyKey(typedIdResource, typeResource);
			if (key != null && key instanceof AbstractRelationProperty) {

				final AbstractRelationProperty staticRel = (AbstractRelationProperty)key;
				final AbstractNode startNode             = typedIdResource.getTypesafeNode();

				if (startNode != null) {

					Class startNodeType = startNode.getClass();
					
					//if (EntityContext.isReadOnlyProperty(startNodeType, EntityContext.getPropertyKeyForName(startNodeType, typeResource.getRawType()))) {
					if (EntityContext.getPropertyKeyForJSONName(startNodeType, typeResource.getRawType()).isReadOnlyProperty()) {

						logger.log(Level.INFO, "Read-only property on {1}: {0}", new Object[] { startNode.getClass(), typeResource.getRawType() });

						return new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

					}

					final DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
					final Iterable<AbstractRelationship> rels = startNode.getRelationships(staticRel.getRelType(), staticRel.getDirection());
					final StructrTransaction transaction      = new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {

							for (final AbstractRelationship rel : rels) {

								final AbstractNode otherNode = rel.getOtherNode(startNode);
								final Class otherNodeType    = otherNode.getClass();
								final String id              = otherNode.getProperty(AbstractNode.uuid);

								// Delete relationship only if not contained in property set
								// check type of other node as well, there can be relationships
								// of the same type to more than one destTypes!
								if (staticRel.getDestType().equals(otherNodeType) &&!propertySet.containsValue(id)) {

									deleteRel.execute(rel);

								} else {

									// Remove id from set because there's already an existing relationship
									propertySet.values().remove(id);
								}

							}

							// Now add new relationships for any new id: This should be the rest of the property set
							for (final Object obj : propertySet.values()) {

								final String uuid                 = (String) obj;
								final List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

								attrs.add(Search.andExactUuid(uuid));

								final Result results = searchNode.execute(attrs);

								if (results.isEmpty()) {

									throw new NotFoundException();

								}

								if (results.size() > 1) {

									throw new SystemException("More than one result found for uuid " + uuid + "!");

								}

								final AbstractNode targetNode = (AbstractNode) results.get(0);

//                                                              String type             = EntityContext.normalizeEntityName(typeResource.getRawType());
								final Class type = staticRel.getDestType();

								if (!type.equals(targetNode.getClass())) {

									throw new FrameworkException(startNode.getClass().getSimpleName(), new TypeToken(AbstractNode.uuid, type.getSimpleName()));

								}

								staticRel.createRelationship(securityContext, startNode, targetNode);

							}

							return null;
						}
					};

					// execute transaction
					Services.command(securityContext, TransactionCommand.class).execute(transaction);

				}

			}
		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		final AbstractNode sourceNode = typedIdResource.getIdResource().getNode();
		final PropertyKey propertyKey = findPropertyKey(typedIdResource, typeResource);

		if (sourceNode != null && propertyKey != null && propertyKey instanceof AbstractRelationProperty) {

			final StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					final AbstractRelationProperty relationshipProperty = (AbstractRelationProperty)propertyKey;
					final Class sourceNodeType                          = sourceNode.getClass();

					if (relationshipProperty.isReadOnlyProperty()) {

						logger.log(Level.INFO, "Read-only property on {0}: {1}", new Object[] { sourceNodeType, typeResource.getRawType() });

						return null;
					}

					// fetch notion
					final Notion notion                  = relationshipProperty.getNotion();
					final PropertyKey primaryPropertyKey = notion.getPrimaryPropertyKey();

					// apply notion if the property set contains the ID property as the only element
					if (primaryPropertyKey != null && propertySet.containsKey(primaryPropertyKey.jsonName()) && propertySet.size() == 1) {

						// the notion that is defined for this relationship can deserialize
						// objects with a single key (uuid for example), and the POSTed
						// property set contains value(s) for this key, so we only need
						// to create relationships
						final Adapter<Object, GraphObject> deserializationStrategy = notion.getAdapterForSetter(securityContext);
						final Object keySource                                     = propertySet.get(primaryPropertyKey.jsonName());

						if (keySource != null) {

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

					} else {

						// the notion can not deserialize objects with a single key, or
						// the POSTed propertySet did not contain a key to deserialize,
						// so we create a new node from the POSTed properties and link
						// the source node to it. (this is the "old" implementation)
						final AbstractNode otherNode = typeResource.createNode(propertySet);

						// FIXME: this prevents post creation transformations from working
						// properly if they rely on an already existing relationship when
						// the transformation runs.

						// TODO: we need to find a way to notify the listener at the end of the
						// transaction, when all entities and relationships are created!
						if (otherNode != null) {

							// FIXME: this creates duplicate relationships when the related
							//        node ID is already present in the property set..



							relationshipProperty.createRelationship(securityContext, sourceNode, otherNode);

							return otherNode;

						}
					}

					return null;
				}
			};

			AbstractNode newNode = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);
			if (newNode != null) {

				RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
				result.addHeader("Location", buildLocationHeader(newNode));

				return result;
			}

		} else {

			// look for methods that have an @Export annotation
			AbstractNode entity = typedIdResource.getTypesafeNode();
			Class entityType    = typedIdResource.getEntityClass();
			String methodName   = typeResource.getRawType();
			boolean success     = false;

			if (entityType != null && methodName != null) {

				for (Method method : EntityContext.getExportedMethodsForType(entityType)) {

					if (methodName.equals(method.getName())) {

						if (method.getAnnotation(Export.class) != null) {

							if (method.getReturnType().equals(Void.TYPE)) {

								try {
									Object[] parameters = extractParameters(propertySet, method.getParameterTypes());
									method.invoke(entity, parameters);
									success = true;

									break;

								} catch (Throwable t) {
								
									logger.log(Level.WARNING, "Unable to call RPC method {0}: {1}", new Object[] { methodName, t.getMessage() } );
								}
								
							} else {

								logger.log(Level.WARNING, "Unable to call RPC method {0}: method has wrong return type (must be void).", methodName);
								
							}
						}
					}
				}
			}

			if (success) {

				return new RestMethodResult(HttpServletResponse.SC_OK);
			}

		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
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
		return typeResource.getEntityClass();
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
	private Object[] extractParameters(Map<String, Object> properties, Class[] parameterTypes) {
		
		List<Object> values     = new ArrayList<Object>(properties.values());
		List<Object> parameters = new ArrayList<Object>();
		int index               = 0;
		
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
			convertedObject = value.toString();

		} else if (value instanceof Number) {

			Number number = (Number)value;
			
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

			logger.log(Level.WARNING, "Unable to deserialize value {0} of type {1}, Class has no static valueOf method.", new Object[] { value, type } );
		}
		
		return convertedObject;
	}
}
