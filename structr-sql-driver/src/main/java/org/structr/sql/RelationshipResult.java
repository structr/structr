/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.api.graph.RelationshipType;

/**
 */
class RelationshipResult {

	private Map<String, Object> data = new LinkedHashMap<>();
	private SQLIdentity id           = null;
	private RelationshipType relType = null;
	private SQLIdentity sourceNode   = null;
	private SQLIdentity targetNode   = null;

	public RelationshipResult(final SQLIdentity identity, final SQLIdentity sourceNode, final SQLIdentity targetNode, final RelationshipType relType) {
		this(identity, sourceNode, targetNode, relType, null);
	}

	public RelationshipResult(final SQLIdentity identity, final SQLIdentity sourceNode, final SQLIdentity targetNode, final RelationshipType relType, final Map<String, Object> data) {

		if (data != null) {
			this.data.putAll(data);
		}

		this.id         = identity;
		this.relType    = relType;
		this.sourceNode = sourceNode;
		this.targetNode = targetNode;
	}

	SQLIdentity id() {
		return id;
	}

	Map<String, Object> data() {
		return data;
	}

	void visit(final ResultSet result) throws SQLException {

		// The type column contains the column index
		// of the actual value in this property row.
		final String name  = result.getString("name");
		final int type     = result.getInt("type");

		if (type >= 0){

			final Object value = result.getObject(type);

			if (name != null && value != null) {

				data.put(name, value);
			}
		}
	}

	RelationshipType getRelType() {
		return relType;
	}

	SQLIdentity getSourceNode() {
		return sourceNode;
	}

	SQLIdentity getTargetNode() {
		return targetNode;
	}
}
