/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.common.error.FrameworkException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Returns a List of relationships for the given node.
 *
 *
 */
public class NodeRelationshipsCommand extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(NodeRelationshipsCommand.class.getName());

	/**
	 * Fetch relationships for the given source node.
	 *
	 * @param sourceNode
	 * @param relType can be null
	 * @param dir
	 *
	 * @return a list of relationships
	 * @throws FrameworkException
	 */
	public List<RelationshipInterface> execute(NodeInterface sourceNode, RelationshipType relType, Direction dir) throws FrameworkException {

		RelationshipFactory factory        = new RelationshipFactory(securityContext);
		List<RelationshipInterface> result = new LinkedList<>();
		Node node                          = sourceNode.getNode();

		Iterable<Relationship> rels;

		if (node == null) {

			return Collections.EMPTY_LIST;

		}

		if (relType != null) {

			rels = node.getRelationships(dir, relType);

		} else {

			rels = node.getRelationships(dir);
		}

		try {

			for (Relationship r : rels) {

				result.add(factory.instantiate(r));
			}

		} catch (RuntimeException e) {

			// The relationships already collected are returned; iterating them is a database read that can
			// fail halfway through, and a partial answer is more useful here than none.
			logger.warn("Unable to read relationships of node {}: {}", sourceNode.getUuid(), e.getMessage());
		}

		return result;
	}
}
