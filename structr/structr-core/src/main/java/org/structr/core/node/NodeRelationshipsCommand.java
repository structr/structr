/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------

/**
 * Returns a List of Relationships for the given node.
 *
 * @param one or more AbstractNode instances to collect the properties from.
 * @return a list of relationships for the given nodes
 *
 * @author amorgner
 */
public class NodeRelationshipsCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(NodeRelationshipsCommand.class.getName());

	//~--- methods --------------------------------------------------------

	/**
	 * First argument is the AbstractNode to get relationships for.
	 * Second argument is relationship type {@see RelationshipType} (can be null)
	 * Third argument is direction {@see Direction}
	 *
	 * @param parameters
	 * @return list with relationships that match relationship type and direction
	 */
	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

		// Avoid to return null
		List<StructrRelationship> result = new LinkedList<StructrRelationship>();

		if (parameters.length == 3) {

			Object arg0              = parameters[0];
			Object arg1              = parameters[1];
			Object arg2              = parameters[2];
			AbstractNode sNode       = (AbstractNode) arg0;
			RelationshipType relType = (RelationshipType) arg1;
			Direction dir            = (Direction) arg2;
			Node node                = null;
			long id                  = -1;

			try {

				id = sNode.getId();

				if (id > -1) {
					node = graphDb.getNodeById(sNode.getId());
				} else {
					return result;
				}

			} catch (Exception e) {

				logger.log(Level.WARNING, "Not found: " + id, e.getMessage());

				return result;
			}

			Iterable<Relationship> rels;

			if (arg1 != null) {
				rels = node.getRelationships(relType, dir);
			} else {
				rels = node.getRelationships(dir);
			}

			try {

				for (Relationship r : rels) {
					result.add(new StructrRelationship(securityContext, r));
				}

			} catch (RuntimeException e) {

				logger.log(Level.WARNING, "Exception occured: ", e.getMessage());

				/**
				 * ********* FIXME 
				 *
				 * Here an exception occurs:
				 *
				 * org.neo4j.kernel.impl.nioneo.store.InvalidRecordException: Node[5] is neither firstNode[37781] nor secondNode[37782] for Relationship[188125]
				 * at org.neo4j.kernel.impl.nioneo.xa.ReadTransaction.getMoreRelationships(ReadTransaction.java:131)
				 * at org.neo4j.kernel.impl.nioneo.xa.NioNeoDbPersistenceSource$ReadOnlyResourceConnection.getMoreRelationships(NioNeoDbPersistenceSource.java:280)
				 * at org.neo4j.kernel.impl.persistence.PersistenceManager.getMoreRelationships(PersistenceManager.java:100)
				 * at org.neo4j.kernel.impl.core.NodeManager.getMoreRelationships(NodeManager.java:585)
				 * at org.neo4j.kernel.impl.core.NodeImpl.getMoreRelationships(NodeImpl.java:358)
				 * at org.neo4j.kernel.impl.core.IntArrayIterator.hasNext(IntArrayIterator.java:115)
				 *
				 *
				 */
			}
		}

		return result;
	}
}
