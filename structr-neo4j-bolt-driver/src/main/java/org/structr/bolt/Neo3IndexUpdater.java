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
package org.structr.bolt;

import org.structr.api.index.ExistingIndexInfo;
import org.structr.api.index.NewIndexConfig;

import java.util.Map;

public class Neo3IndexUpdater extends IndexUpdater {

	public Neo3IndexUpdater(final BoltDatabaseService db, final boolean supportsRelationshipIndexes) {

		super(db, supportsRelationshipIndexes);
	}

	@Override
	protected String getIndexIdentifier(final NewIndexConfig config) {
		return "";
	}

	@Override
	protected String getIndexIdentifier(final ExistingIndexInfo config) {
		return "";
	}

	@Override
	protected String getIndexInfoQuery() {
		return "CALL db.indexes() YIELD description, state, type WHERE type = 'node_label_property' RETURN {description: description, state: state} ORDER BY description";
	}

	@Override
	protected ExistingIndexInfo convertIndexInfoRow(final Map<String, Object> indexInfoRow) {
		return null;
	}

	@Override
	protected String getCreateIndexStatement(NewIndexConfig newIndexConfig) {
		return "";
	}

	@Override
	protected String getDropIndexStatement(ExistingIndexInfo existingIndexInfo) {
		return "";
	}
}
