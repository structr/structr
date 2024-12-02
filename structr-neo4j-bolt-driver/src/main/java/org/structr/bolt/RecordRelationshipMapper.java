/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.bolt;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Relationship;

import java.util.function.Function;

/**
 *
 */
class RecordRelationshipMapper implements Function<Record, Relationship> {

	private BoltDatabaseService db = null;

	public RecordRelationshipMapper(final BoltDatabaseService db) {
		this.db = db;
	}

	@Override
	public Relationship apply(final Record record) {

		final SessionTransaction tx = db.getCurrentTransaction();

		// target node present?
		final Value t = record.get("t");
		if (!t.isNull()) {

			tx.getNodeWrapper(t.asNode());
		}

		// source node present?
		final Value s = record.get("s");
		if (!s.isNull()) {

			tx.getNodeWrapper(s.asNode());
		}

		// "other" node present (direction unknown)?
		final Value o = record.get("o");
		if (!o.isNull()) {

			tx.getNodeWrapper(o.asNode());
		}

		return record.get(0).asRelationship();
	}
}
