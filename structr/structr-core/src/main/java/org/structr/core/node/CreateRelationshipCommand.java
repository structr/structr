/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.node;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Transformation;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Creates a relationship between two AbstractNode instances. The execute
 * method of this command takes the following parameters.
 *
 * @param startNode the start node
 * @param endNode the end node
 * @param combinedType the name of relationship combinedType to create
 *
 * @return the new relationship
 *
 * @author cmorgner
 */
public class CreateRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(CreateRelationshipCommand.class.getName());

	//~--- fields ---------------------------------------------------------

	private RelationshipFactory relationshipFactory;

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

		relationshipFactory = (RelationshipFactory) arguments.get("relationshipFactory");

		if ((graphDb != null) && (parameters.length == 3)) {

			Object arg0              = parameters[0];    // start node
			Object arg1              = parameters[1];    // end node
			Object arg2              = parameters[2];    // relationship combinedType
			RelationshipType relType = null;

			if (arg2 instanceof String) {

				relType = getRelationshipTypeFor((String) arg2);

			} else if (arg2 instanceof RelationshipType) {

				relType = (RelationshipType) arg2;

			} else {

				throw new UnsupportedArgumentError("Wrong argument type(s).");

			}

			if ((arg0 instanceof AbstractNode) && (arg1 instanceof AbstractNode)) {

				AbstractNode startNode = (AbstractNode) arg0;
				AbstractNode endNode   = (AbstractNode) arg1;

				return createRelationship(startNode, endNode, relType, null);

			} else {

				throw new UnsupportedArgumentError("Wrong argument type(s).");

			}

		} else if ((graphDb != null) && (parameters.length == 5)) {

			Object arg0 = parameters[0];                 // start node
			Object arg1 = parameters[1];                 // end node
			Object arg2 = parameters[2];                 // relationship type or combinedType
			Object arg3 = parameters[3];                 // properties
			Object arg4 = parameters[4];                 // check duplicates

			// parameters
			Map<String, Object> properties = null;
			RelationshipType relType       = null;

			if (arg2 instanceof String) {

				relType = getRelationshipTypeFor((String) arg2);

			} else if (arg2 instanceof RelationshipType) {

				relType = (RelationshipType) arg2;

			} else {

				throw new UnsupportedArgumentError("Wrong argument type(s).");

			}

			if (arg3 instanceof Map) {

				properties = (Map<String, Object>) arg3;

			}

			boolean checkDuplicates = false;

			if (arg4 instanceof Boolean) {

				checkDuplicates = ((Boolean) arg4) == true;

			}

			if ((arg0 instanceof AbstractNode) && (arg1 instanceof AbstractNode)) {

				AbstractNode startNode = (AbstractNode) arg0;
				AbstractNode endNode   = (AbstractNode) arg1;

				if (checkDuplicates) {

					List<AbstractRelationship> incomingRels = endNode.getIncomingRelationships();

					for (AbstractRelationship rel : incomingRels) {

                                                // Duplicate check:
                                                // First, check if relType and start node are equal
						if (rel.getRelType().equals(relType) && rel.getStartNode().equals(startNode)) {

                                                        // At least one property of new rel has to be different to the tested existing node
							Map<String, Object> relProps = rel.getProperties();
							boolean propsEqual           = true;

							for (Entry entry : properties.entrySet()) {

								String key = (String) entry.getKey();
								Object val = entry.getValue();

								if (!relProps.containsKey(key) || !relProps.get(key).equals(val)) {

									propsEqual = false;

									break;

								}

							}

							if (propsEqual) {

								logger.log(Level.WARNING, "Creation of duplicate relationship was blocked");

								return null;

							}

						}

					}

				}

				return createRelationship(startNode, endNode, relType, properties);

			} else {

				throw new UnsupportedArgumentError("Wrong argument type(s).");

			}

		}

		return null;
	}

	private synchronized AbstractRelationship createRelationship(final AbstractNode fromNode, final AbstractNode toNode, final RelationshipType relType, final Map<String, Object> properties)
		throws FrameworkException {

		return (AbstractRelationship) Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Node startNode              = fromNode.getNode();
				Node endNode                = toNode.getNode();
				Relationship rel            = startNode.createRelationshipTo(endNode, relType);
				AbstractRelationship newRel = relationshipFactory.createRelationship(securityContext, rel);

				newRel.setProperty(AbstractRelationship.HiddenKey.createdDate.name(), new Date());

				if (newRel != null) {

					if ((properties != null) &&!properties.isEmpty()) {

						newRel.setProperties(properties);

					}

					// notify relationship of its creation
					newRel.onRelationshipInstantiation();

					// iterate post creation transformations
					for (Transformation<GraphObject> transformation : EntityContext.getEntityCreationTransformations(newRel.getClass())) {

						transformation.apply(securityContext, newRel);

					}

				}

				return newRel;
			}

		});
	}

	//~--- get methods ----------------------------------------------------

	private RelationshipType getRelationshipTypeFor(final String relTypeString) {

		RelationshipType relType = null;

		try {
			relType = RelType.valueOf(relTypeString);
		} catch (Exception ignore) {}

		if (relType == null) {

			relType = DynamicRelationshipType.withName(relTypeString);

		}

		return relType;
	}
}
