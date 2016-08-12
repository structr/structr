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

import org.structr.bolt.index.CypherNodeIndex;
import org.structr.api.graph.Node;
import org.structr.api.index.Index;
import org.structr.api.index.IndexManager;

/**
 *
 * @author Christian Morgner
 */
public class BoltNodeIndexer implements IndexManager<Node> {

	private BoltDatabaseService db = null;
	private Index<Node> fulltext   = null;
	private Index<Node> exact      = null;
	private Index<Node> spatial    = null;

	public BoltNodeIndexer(final BoltDatabaseService db) {
		this.db = db;
	}

	@Override
	public Index<Node> fulltext() {

		if (fulltext == null) {
			fulltext = new CypherNodeIndex(db);
		}

		return fulltext;
	}

	@Override
	public Index<Node> exact() {

		if (exact == null) {
			exact = new CypherNodeIndex(db);
		}

		return exact;
	}

	@Override
	public Index<Node> spatial() {

		if (spatial == null) {
			spatial = new CypherNodeIndex(db);
		}

		return spatial;
	}

}
