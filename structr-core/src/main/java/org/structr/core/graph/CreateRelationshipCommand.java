/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Transformation;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

//~--- classes ----------------------------------------------------------------

/**
 * Creates a relationship between two NodeInterface instances. The execute
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

	//~--- methods --------------------------------------------------------

	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R execute(final A fromNode, final B toNode, final Class<R> relType) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, null);
	}

	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R execute(final A fromNode, final B toNode, final Class<R> relType, final PropertyMap properties) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, properties);
	}

	private synchronized <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R createRelationship(final A fromNode, final B toNode, final Class<R> relType, final PropertyMap properties) throws FrameworkException {

		final RelationshipFactory<R> factory = new RelationshipFactory(securityContext);
		final R template                     = instantiate(relType);
		final Node startNode                 = fromNode.getNode();
		final Node endNode                   = toNode.getNode();
		final Relationship rel               = startNode.createRelationshipTo(endNode, template);
		final R newRel                       = factory.instantiate(rel);
		final Date now                       = new Date();

		// logger.log(Level.INFO, "CREATING relationship {0}-[{1}]->{2}", new Object[] {  fromNode.getType(), newRel.getRelType(), toNode.getType() } );

		if (newRel != null) {

			newRel.unlockReadOnlyPropertiesOnce();
			newRel.setProperty(GraphObject.type, relType.getSimpleName());

			// set UUID
			newRel.unlockReadOnlyPropertiesOnce();
			newRel.setProperty(GraphObject.id, getNextUuid());

			// set created date
			newRel.unlockReadOnlyPropertiesOnce();
			newRel.setProperty(AbstractRelationship.createdDate, now);

			// set last modified date
			newRel.unlockReadOnlyPropertiesOnce();
			newRel.setProperty(AbstractRelationship.lastModifiedDate, now);

			// Try to get the cascading delete flag from the domain specific relationship type
			newRel.unlockReadOnlyPropertiesOnce();
			newRel.setProperty(AbstractRelationship.cascadeDelete, factory.instantiate(rel).getCascadingDeleteFlag());

			// notify transaction handler
			TransactionCommand.relationshipCreated(newRel);

			if (properties != null) {

				for (Entry<PropertyKey, Object> entry : properties.entrySet()) {

					PropertyKey key = entry.getKey();

					// on creation, writing of read-only properties should be possible
					if (key.isReadOnly() || key.isWriteOnce()) {
						newRel.unlockReadOnlyPropertiesOnce();
					}

					newRel.setProperty(entry.getKey(), entry.getValue());
				}

			}

			// notify relationship of its creation
			newRel.onRelationshipCreation();

			// iterate post creation transformations
			for (Transformation<GraphObject> transformation : StructrApp.getConfiguration().getEntityCreationTransformations(newRel.getClass())) {

				transformation.apply(securityContext, newRel);

			}

		}

		return newRel;
	}

	private <T extends Relation> T instantiate(final Class<T> type) {

		try {

			return type.newInstance();

		} catch(Throwable t) {
			t.printStackTrace();
		}

		return null;
	}
}
