/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.rest.resource;

import org.structr.common.CaseHelper;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TypeToken;
import org.structr.core.*;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelationship;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.notion.Notion;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.SystemException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.ListUtils;

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

	//~--- constructors ---------------------------------------------------

	public StaticRelationshipResource(SecurityContext securityContext, TypedIdResource typedIdResource, TypeResource typeResource) {

		this.securityContext = securityContext;
		this.typedIdResource = typedIdResource;
		this.typeResource    = typeResource;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {

		// fetch results from typedIdResource (should be a single node)
		List<GraphObject> results = typedIdResource.doGet();

		if (results != null) {

			// ok, source node exists, fetch it
			AbstractNode sourceNode = typedIdResource.getTypesafeNode();

			if (sourceNode != null) {

				// fetch static relationship definition
				DirectedRelationship staticRel = findDirectedRelationship(typedIdResource, typeResource);

				if (staticRel != null) {

					List<AbstractNode> relatedNodes = staticRel.getRelatedNodes(securityContext, sourceNode);

					// check for search keys in request first..
					List<SearchAttribute> dummyList = new LinkedList<SearchAttribute>();
					if(typeResource.hasSearchableAttributes(dummyList)) {

						// use result list of doGet from typeResource and intersect with relatedNodes list.
						List<GraphObject> typeNodes    = typeResource.doGet();

						List intersection = ListUtils.intersection(relatedNodes, typeNodes);
						if (!intersection.isEmpty()) {

							return intersection;

						}
					}

					// return non-empty list
					if(!relatedNodes.isEmpty()) {
						return relatedNodes;
					}

					// do not return empty collection here, try getProperty
					// return Collections.emptyList();

				}

				// URL: /type1/uuid/type2
				// we need to use the raw type of the second type constraint
				// as the property key for getProperty
				// look for a property converter for the given type and key
				Class type = sourceNode.getClass();

				// String key                  = typeResource.getRawType();
				String key                  = CaseHelper.toLowerCamelCase(typeResource.getRawType());
				PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, type, key);

				if (converter != null) {

					Value conversionValue = EntityContext.getPropertyConversionParameter(type, key);

					converter.setCurrentNode(sourceNode);
					converter.setRawMode(true);    // disable notion

					Object value = converter.convertForGetter(null, conversionValue);

					// create error message in advance to avoid having to construct it twice in different locations
					StringBuilder msgBuilder = new StringBuilder();

					msgBuilder.append("Property result on type ");
					msgBuilder.append(sourceNode.getClass().getSimpleName());
					msgBuilder.append(", key ");
					msgBuilder.append(key);
					msgBuilder.append(" is not an Iterable<GraphObject>!");

					String msg = msgBuilder.toString();

					if (value != null) {

						// check for non-empty list of GraphObjects
						if ((value instanceof List) &&!((List) value).isEmpty() && ((List) value).get(0) instanceof GraphObject) {

							return (List<GraphObject>) value;

						} else if (value instanceof Iterable) {

							// check type of value (must be an Iterable of GraphObjects in order to proceed here)
							List<GraphObject> propertyListResult = new LinkedList<GraphObject>();
							Iterable sourceIterable              = (Iterable) value;

							for (Object o : sourceIterable) {

								if (o instanceof GraphObject) {

									propertyListResult.add((GraphObject) o);

								} else {

									throw new SystemException(msg);

								}

							}

							return propertyListResult;
						}
					} else {

						logger.log(Level.SEVERE, msg);

						throw new SystemException(msg);

					}

				}
			}
		}

		return Collections.emptyList();
	}
//
//	@Override
//	public RestMethodResult doDelete() throws FrameworkException {
//
//		List<GraphObject> results = typedIdResource.doGet();
//
//		if (results != null) {
//
//			// fetch static relationship definition
//			DirectedRelationship staticRel = findDirectedRelationship(typedIdResource, typeResource);
//
//			if (staticRel != null) {
//
//				AbstractNode startNode = typedIdResource.getTypesafeNode();
//
//				if (startNode != null) {
//
//					if (EntityContext.isReadOnlyProperty(startNode.getClass(), typeResource.getRawType())) {
//
//						logger.log(Level.INFO, "Read-only property on {1}: {0}", new Object[] { startNode.getClass(), typeResource.getRawType() });
//
//						return new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);
//
//					}
//
//					final List<StructrRelationship> rels = startNode.getRelationships(staticRel.getRelType(), staticRel.getDirection());
//					StructrTransaction transaction       = new StructrTransaction() {
//
//						@Override
//						public Object execute() throws FrameworkException {
//
//							for (StructrRelationship rel : rels) {
//
//								rel.delete(securityContext);
//
//							}
//
//							return null;
//						}
//					};
//
//					// execute transaction
//					Services.command(securityContext, TransactionCommand.class).execute(transaction);
//
//				}
//
//			}
//		}
//
//		return new RestMethodResult(HttpServletResponse.SC_OK);
//	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		List<GraphObject> results = typedIdResource.doGet();
		final Command searchNode        = Services.command(securityContext, SearchNodeCommand.class);

		if (results != null) {

			// fetch static relationship definition
			final DirectedRelationship staticRel = findDirectedRelationship(typedIdResource, typeResource);

			if (staticRel != null) {

				final AbstractNode startNode = typedIdResource.getTypesafeNode();

				if (startNode != null) {

					if (EntityContext.isReadOnlyProperty(startNode.getClass(), typeResource.getRawType())) {

						logger.log(Level.INFO, "Read-only property on {1}: {0}", new Object[] { startNode.getClass(), typeResource.getRawType() });

						return new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

					}

					final List<StructrRelationship> rels = startNode.getRelationships(staticRel.getRelType(), staticRel.getDirection());
					StructrTransaction transaction       = new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {

							for (StructrRelationship rel : rels) {

								AbstractNode otherNode = rel.getOtherNode(startNode);
								String id              = otherNode.getStringProperty(AbstractNode.Key.uuid.name());

								// Delete relationship only if not contained in property set
								if (!propertySet.containsValue(id)) {

									rel.delete(securityContext);

								} else {

									// Remove id from set because there's already an existing relationship
									propertySet.remove(id);
								}

							}

							// Now add new relationships for any new id: This should be the rest of the property set
							for (Object obj : propertySet.values()) {

								String uuid = (String) obj;

								List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

								attrs.add(Search.andExactUuid(uuid));

								List<AbstractNode> results = (List<AbstractNode>) searchNode.execute(null, false, false, attrs);

								if (results.isEmpty()) {

									throw new NotFoundException();

								}

								if (results.size() > 1) {

									throw new SystemException("More than one result found for uuid " + uuid + "!");

								}

								AbstractNode targetNode = (AbstractNode) results.get(0);
								String type             = EntityContext.normalizeEntityName(typeResource.getRawType());

								if (!type.equals(targetNode.getType())) {

									throw new FrameworkException(startNode.getType(), new TypeToken(uuid, type));

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

		// create transaction closure
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				AbstractNode sourceNode  = typedIdResource.getIdResource().getNode();
				DirectedRelationship rel = EntityContext.getDirectedRelationship(sourceNode.getClass(), typeResource.getRawType());

				if ((sourceNode != null) && (rel != null)) {

					if (EntityContext.isReadOnlyProperty(sourceNode.getClass(), typeResource.getRawType())) {

						logger.log(Level.INFO, "Read-only property on {0}: {1}", new Object[] { sourceNode.getClass(), typeResource.getRawType() });

						return null;

					}

					// fetch notion
					Notion notion                  = rel.getNotion();
					PropertyKey primaryPropertyKey = notion.getPrimaryPropertyKey();

					if ((primaryPropertyKey != null) && propertySet.containsKey(primaryPropertyKey.name())) {

						// the notion that is defined for this relationship can deserialize
						// objects with a single key (uuid for example), and the POSTed
						// property set contains value(s) for this key, so we only need
						// to create relationships
						Adapter<Object, GraphObject> deserializationStrategy = notion.getAdapterForSetter(securityContext);
						Object keySource                                     = propertySet.get(primaryPropertyKey.name());

						if (keySource != null) {

							GraphObject otherNode = null;

							if (keySource instanceof Collection) {

								Collection collection = (Collection) keySource;

								for (Object key : collection) {

									otherNode = deserializationStrategy.adapt(key);

									if (otherNode != null) {

										rel.createRelationship(securityContext, sourceNode, otherNode);

									}

								}

							} else {

								// create a single relationship
								otherNode = deserializationStrategy.adapt(keySource);

								if (otherNode != null) {

									rel.createRelationship(securityContext, sourceNode, otherNode);

								}
							}

							return otherNode;

						} else {

							logger.log(Level.INFO, "Key {0} not found in {1}", new Object[] { primaryPropertyKey.name(), propertySet.toString() });

						}

						return null;
					} else {

						// the notion can not deserialize objects with a single key, or
						// the POSTed propertySet did not contain a key to deserialize,
						// so we create a new node from the POSTed properties and link
						// the source node to it. (this is the "old" implementation)
						AbstractNode otherNode = typeResource.createNode(propertySet);

						if (otherNode != null) {

							rel.createRelationship(securityContext, sourceNode, otherNode);

							return otherNode;

						}
					}

				}

				throw new IllegalPathException();
			}
		};

		// execute transaction: create new node
		AbstractNode newNode = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);
		RestMethodResult result;

		if (newNode != null) {

			result = new RestMethodResult(HttpServletResponse.SC_CREATED);

			result.addHeader("Location", buildLocationHeader(newNode));

		} else {

			result = new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

		}

		return result;
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
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof TypeResource) {

			throw new IllegalPathException();

		}

		return super.tryCombineWith(next);
	}

	//~--- get methods ----------------------------------------------------

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
}
