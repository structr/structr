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

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.wrapper.NodeWrapper;

/**
 * A mapper that converts a stream of Records to a stream of Relationships,
 * with the ability to pre-fetch additional data from the Record.
 */
public class PrefetchingRelationshipMapper {

	private Relationship relationship = null;
	private Node sourceNode           = null;
	private Node targetNode           = null;

	public PrefetchingRelationshipMapper(final Record record) {

		this.relationship = record.get(0).asRelationship();

		// target node present?
		final Value t = record.get("t");
		if (!t.isNull()) {

			this.targetNode = t.asNode();
		}

		// source node present?
		final Value s = record.get("s");
		if (!s.isNull()) {

			this.sourceNode = s.asNode();
		}

		// "other" node present (direction unknown)?
		final Value o = record.get("o");
		if (!o.isNull()) {

			final Node otherNode = o.asNode();
			if (otherNode.id() == relationship.startNodeId()) {

				this.sourceNode = otherNode;

			} else {

				this.targetNode = otherNode;
			}
		}
	}

	public Relationship getRelationship() {
		return this.relationship;
	}

	public void prefetch(final BoltDatabaseService db) {

		// prefetch relationship endpoints
		if (sourceNode != null) {

			NodeWrapper.newInstance(db, sourceNode);
		}

		if (targetNode != null) {

			NodeWrapper.newInstance(db, targetNode);
		}
	}
}
