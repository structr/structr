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



package org.structr.common;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipExpander;

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
		Map<Integer, Relationship> sortedRelationshipMap = new TreeMap<Integer, Relationship>();

		for (Relationship rel : node.getRelationships(RelType.CONTAINS, direction)) {

			try {

				Integer position = null;

				if (rel.hasProperty(resourceId)) {

					Object prop = rel.getProperty(resourceId);

					if (prop instanceof Integer) {

						position = (Integer) prop;

					} else if (prop instanceof String) {

						position = Integer.parseInt((String) prop);

					} else {

						throw new java.lang.IllegalArgumentException("Expected Integer or String");

					}

					sortedRelationshipMap.put(position, rel);

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
