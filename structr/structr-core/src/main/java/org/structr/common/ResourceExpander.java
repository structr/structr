/*
 *  Copyright (C) 2010-2012 Axel Morgner
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



package org.structr.common;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipExpander;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class ResourceExpander implements RelationshipExpander {

	private static final Logger logger = Logger.getLogger(ResourceExpander.class.getName());

	//~--- fields ---------------------------------------------------------

	private Direction direction = Direction.OUTGOING;
	private String resourceId   = null;

	//~--- constructors ---------------------------------------------------

	public ResourceExpander(final String resourceId) {
		this.resourceId = resourceId;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public Iterable<Relationship> expand(Node node) {

		/**
		 * Expand outgoing relationships of type CONTAINS and check for
		 * resourceId property. If property exists, let TreeMap do the
		 * sorting for us and return sorted values from map.
		 */
		Map<Long, Relationship> sortedRelationshipMap = new TreeMap<Long, Relationship>();

		for (final Relationship rel : node.getRelationships(RelType.CONTAINS, direction)) {

			try {

				long position;
				Object prop = null;
				final String key;

				// "*" is a wildcard for "matches any resource id"
				// TOOD: use pattern matching here?
				if (rel.hasProperty("*")) {

					prop = rel.getProperty("*");
					key  = "*";

				} else if (rel.hasProperty(resourceId)) {

					prop = rel.getProperty(resourceId);
					key  = resourceId;

				} else {

					key = null;

				}

				if ((key != null) && (prop != null)) {

					if (prop instanceof Long) {

						position = (Long) prop;

					} else if (prop instanceof Integer) {

						position = ((Integer) prop).longValue();

					} else if (prop instanceof String) {

						position = Long.parseLong((String) prop);

					} else {

						throw new java.lang.IllegalArgumentException("Expected Long, Integer or String");

					}

					long originalPos = position;

					// find free slot
					while (sortedRelationshipMap.containsKey(position)) {

						position++;

					}

					sortedRelationshipMap.put(position, rel);

					if (originalPos != position) {

						final long newPos = position;

						Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

							@Override
							public Object execute() throws FrameworkException {

								rel.setProperty(key, newPos);

								return null;
							}

						});

					}

					logger.log(Level.FINEST, "Node {0}: Put relationship {1} into map at position {2}", new Object[] { node, rel, position });

				}

			} catch (Throwable t) {

				// fail fast, no check
				logger.log(Level.SEVERE, "While reading property " + resourceId, t);
			}

		}

		return sortedRelationshipMap.values();
	}

	@Override
	public RelationshipExpander reversed() {

		ResourceExpander reversed = new ResourceExpander(resourceId);

		reversed.setDirection(Direction.INCOMING);

		return reversed;
	}

	//~--- set methods ----------------------------------------------------

	public void setDirection(Direction direction) {
		this.direction = direction;
	}
}
