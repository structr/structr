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
package org.structr.embedded;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Node;

import java.util.Map;
import java.util.function.Function;

/**
 *
 */
class RecordRelationshipMapper implements Function<Map<String, Object>, Relationship> {

	private EmbeddedDatabaseService db = null;

	public RecordRelationshipMapper(final EmbeddedDatabaseService db) {
		this.db = db;
	}

	@Override
	public Relationship apply(final Map<String, Object> record) {

		final EmbeddedTransaction tx = db.getCurrentTransaction();

		// target node present?
		final Object t = record.get("t");
		if (t instanceof Node node) {

			tx.getNodeWrapper(node);
		}

		// source node present?
		final Object s = record.get("s");
		if (s instanceof Node node) {

			tx.getNodeWrapper(node);
		}

		// "other" node present (direction unknown)?
		final Object o = record.get("o");
		if (o instanceof Node node) {

			tx.getNodeWrapper(node);
		}

		System.out.println("RecordRelationshipMapper: returning value with key 'n', not first value, so this might be null! Keys: " + record.keySet());
		return (Relationship) record.get("n");
	}
}
