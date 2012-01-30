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



package org.structr.rest.constraint;

import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.*;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelationship;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.notion.Notion;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
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
import org.structr.common.CaseHelper;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class StaticRelationshipConstraint extends SortableConstraint {

	private static final Logger logger = Logger.getLogger(StaticRelationshipConstraint.class.getName());

	//~--- fields ---------------------------------------------------------

	TypeConstraint typeConstraint       = null;
	TypedIdConstraint typedIdConstraint = null;

	//~--- constructors ---------------------------------------------------

	public StaticRelationshipConstraint(SecurityContext securityContext, TypedIdConstraint typedIdConstraint, TypeConstraint typeConstraint) {

		this.securityContext   = securityContext;
		this.typedIdConstraint = typedIdConstraint;
		this.typeConstraint    = typeConstraint;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {

		// fetch results from typedIdConstraint (should be a single node)
		List<GraphObject> results = typedIdConstraint.doGet();

		if (results != null) {

			// ok, source node exists, fetch it
			AbstractNode sourceNode = typedIdConstraint.getTypesafeNode();

			if (sourceNode != null) {

				// fetch static relationship definition
				DirectedRelationship staticRel = findDirectedRelationship(typedIdConstraint, typeConstraint);

				if (staticRel != null) {

					List<AbstractNode> relatedNodes = staticRel.getRelatedNodes(securityContext, sourceNode);

					if (!relatedNodes.isEmpty()) {

						return relatedNodes;

					}

					// do not return empty collection here, try getProperty
					// return Collections.emptyList();

				}

				// URL: /type1/uuid/type2
				// we need to use the raw type of the second type constraint
				// as the property key for getProperty
				// look for a property converter for the given type and key
				Class type                  = sourceNode.getClass();
				//String key                  = typeConstraint.getRawType();
				String key                  = CaseHelper.toLowerCamelCase(typeConstraint.getRawType());
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

	@Override
	public RestMethodResult doDelete() throws FrameworkException {

		List<GraphObject> results = typedIdConstraint.doGet();

		if (results != null) {

			// fetch static relationship definition
			DirectedRelationship staticRel = findDirectedRelationship(typedIdConstraint, typeConstraint);

			if (staticRel != null) {

				AbstractNode startNode = typedIdConstraint.getTypesafeNode();

				if (startNode != null) {

					if (EntityContext.isReadOnlyProperty(startNode.getClass(), typeConstraint.getRawType())) {

						logger.log(Level.INFO, "Read-only property on {1}: {0}", new Object[] { startNode.getClass(), typeConstraint.getRawType() });

						return new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

					}

					final List<StructrRelationship> rels = startNode.getRelationships(staticRel.getRelType(), staticRel.getDirection());
					StructrTransaction transaction       = new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {

							for (StructrRelationship rel : rels) {

								rel.delete(securityContext);

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
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		// in this case, PUT is just the concatenation of DELETE and POST
		doDelete();

		return doPost(propertySet);
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		// create transaction closure
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				AbstractNode sourceNode  = typedIdConstraint.getIdConstraint().getNode();
				DirectedRelationship rel = EntityContext.getDirectedRelationship(sourceNode.getClass(), typeConstraint.getRawType());

				if ((sourceNode != null) && (rel != null)) {

					if (EntityContext.isReadOnlyProperty(sourceNode.getClass(), typeConstraint.getRawType())) {

						logger.log(Level.INFO, "Read-only property on {0}: {1}", new Object[] { sourceNode.getClass(), typeConstraint.getRawType() });

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
						AbstractNode otherNode = typeConstraint.createNode(propertySet);

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
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws FrameworkException {

		if (next instanceof TypeConstraint) {

			throw new IllegalPathException();

		}

		return super.tryCombineWith(next);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getUriPart() {
		return typedIdConstraint.getUriPart().concat("/").concat(typeConstraint.getUriPart());
	}

	public TypedIdConstraint getTypedIdConstraint() {
		return typedIdConstraint;
	}

	public TypeConstraint getTypeConstraint() {
		return typeConstraint;
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}
}
