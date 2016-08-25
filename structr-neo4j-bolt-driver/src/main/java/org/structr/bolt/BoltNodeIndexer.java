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
	private Index<Node> index      = null;

	public BoltNodeIndexer(final BoltDatabaseService db) {
		this.db = db;
	}

	@Override
	public Index<Node> fulltext() {
		return getIndex();
	}

	@Override
	public Index<Node> exact() {
		return getIndex();
	}

	@Override
	public Index<Node> spatial() {
		return getIndex();
	}

	// ----- private methods -----
	private Index<Node> getIndex() {

		if (index == null) {

			index = new CypherNodeIndex(db);

			db.execute("CREATE INDEX ON :AbstractNode(type)");
			db.execute("CREATE INDEX ON :AbstractNode(name)");
			db.execute("CREATE INDEX ON :AbstractFile(name)");
			db.execute("CREATE INDEX ON :Page(name)");
		}

		return index;
	}
}
