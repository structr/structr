/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.bolt.mapper;

import java.util.List;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.wrapper.NodeWrapper;
import org.structr.bolt.wrapper.RelationshipWrapper;

/**
 * A mapper that converts a stream of Records to a stream of Nodes,
 * with the ability to pre-fetch additional data from the Record.
 */
public class PrefetchingNodeMapper {

	private List<Path> paths = null;
	private Node node       = null;

	public PrefetchingNodeMapper(final Record record) {

		this.node = record.get(0).asNode();

		// if the record contains additional results, we can use them for prefetching
		if (record.size() == 2) {

			this.paths = (List)record.get(1).asList();
		}
	}

	public Node getNode() {
		return this.node;
	}

	public void prefetch(final BoltDatabaseService db, final NodeWrapper node) {

		if (paths != null) {

			for (final Path path : paths) {

				// pre-load all the nodes in this path
				for (final org.neo4j.driver.v1.types.Node n : path.nodes()) {

					// load other nodes only
					if (node != null && n.id() != node.getId()) {

						NodeWrapper.newInstance(db, n);
					}
				}

				// pre-load all the relationships in this path
				for (final org.neo4j.driver.v1.types.Relationship r : path.relationships()) {

					final RelationshipWrapper rel = RelationshipWrapper.newInstance(db, r);

					if (node != null) {

						node.addToCache(rel);
					}
				}
			}
		}
	}
}
