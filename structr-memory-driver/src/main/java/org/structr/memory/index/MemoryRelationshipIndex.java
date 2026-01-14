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
package org.structr.memory.index;

import org.structr.api.graph.Relationship;
import org.structr.api.util.Iterables;
import org.structr.memory.MemoryDatabaseService;
import org.structr.memory.index.filter.MemoryLabelFilter;

import java.util.Set;

/**
 *
 */
public class MemoryRelationshipIndex extends AbstractMemoryIndex<Relationship> {

	public MemoryRelationshipIndex(final MemoryDatabaseService db) {
		super(db);
	}

	@Override
	public Iterable<Relationship> getResult(final MemoryQuery query) {

		final Set<String> labels = query.getTypeLabels();

		if (labels.isEmpty()) {

			return Iterables.filter(query, query.sort(db.getAllRelationships()));

		} else {

			return Iterables.filter(query, query.sort(db.getFilteredRelationships(new MemoryLabelFilter<>(labels))));
		}
	}
}
