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



package org.structr.core.graph;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Transformation;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

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
public class CreateRelationshipCommand<T extends AbstractRelationship> extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(CreateRelationshipCommand.class.getName());

	//~--- methods --------------------------------------------------------

	public T execute(final AbstractNode fromNode, final AbstractNode toNode, final RelationshipType relType) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, null, false);
	}

	public T execute(final AbstractNode fromNode, final AbstractNode toNode, final String relType) throws FrameworkException {
		return createRelationship(fromNode, toNode, getRelationshipTypeFor(relType), null, false);
	}

	public T execute(final AbstractNode fromNode, final AbstractNode toNode, final RelationshipType relType, boolean checkDuplicates) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, null, checkDuplicates);
	}

	public T execute(final AbstractNode fromNode, final AbstractNode toNode, final String relType, boolean checkDuplicates) throws FrameworkException {
		return createRelationship(fromNode, toNode, getRelationshipTypeFor(relType), null, checkDuplicates);
	}

	public T execute(final AbstractNode fromNode, final AbstractNode toNode, final RelationshipType relType, final PropertyMap properties, boolean checkDuplicates) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, properties, checkDuplicates);
	}

	public T execute(final AbstractNode fromNode, final AbstractNode toNode, final String relType, final PropertyMap properties, boolean checkDuplicates) throws FrameworkException {
		return createRelationship(fromNode, toNode, getRelationshipTypeFor(relType), properties, checkDuplicates);
	}
	
	private synchronized T createRelationship(final AbstractNode fromNode, final AbstractNode toNode, final RelationshipType relType, final PropertyMap properties, final boolean checkDuplicates)
		throws FrameworkException {

		return (T) Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				if (checkDuplicates) {

					List<AbstractRelationship> incomingRels = toNode.getIncomingRelationships();

					for (AbstractRelationship rel : incomingRels) {

                                                // Duplicate check:
                                                // First, check if relType and start node are equal
						if (rel.getRelType().equals(relType) && rel.getStartNode().equals(fromNode)) {

                                                        // At least one property of new rel has to be different to the tested existing node
							PropertyMap relProps = rel.getProperties();
							boolean propsEqual   = true;

							for (Entry<PropertyKey, Object> entry : properties.entrySet()) {

								PropertyKey key = entry.getKey();
								Object val      = entry.getValue();

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


				
				RelationshipFactory<T> relationshipFactory = new RelationshipFactory<T>(securityContext);
				Node startNode                             = fromNode.getNode();
				Node endNode                               = toNode.getNode();
				Relationship rel                           = startNode.createRelationshipTo(endNode, relType);
				T newRel                                   = relationshipFactory.instantiateRelationship(securityContext, rel);

				newRel.setProperty(AbstractRelationship.createdDate, new Date());

				if (newRel != null) {

					if ((properties != null) &&!properties.isEmpty()) {

						for (Entry<PropertyKey, Object> entry : properties.entrySet()) {
							newRel.setProperty(entry.getKey(), entry.getValue());
						}

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
