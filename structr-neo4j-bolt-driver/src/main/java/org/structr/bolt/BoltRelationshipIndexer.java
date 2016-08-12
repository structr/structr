/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.index.IndexManager;
import org.structr.bolt.index.CypherRelationshipIndex;

/**
 *
 * @author Christian Morgner
 */
public class BoltRelationshipIndexer implements IndexManager<Relationship> {

	private BoltDatabaseService db = null;
	private Index<Relationship> fulltext   = null;
	private Index<Relationship> exact      = null;
	private Index<Relationship> spatial    = null;

	public BoltRelationshipIndexer(final BoltDatabaseService db) {
		this.db = db;
	}

	@Override
	public Index<Relationship> fulltext() {

		if (fulltext == null) {
			fulltext = new CypherRelationshipIndex(db);
		}

		return fulltext;
	}

	@Override
	public Index<Relationship> exact() {

		if (exact == null) {
			exact = new CypherRelationshipIndex(db);
		}

		return exact;
	}

	@Override
	public Index<Relationship> spatial() {

		if (spatial == null) {
			spatial = new CypherRelationshipIndex(db);
		}

		return spatial;
	}

}
