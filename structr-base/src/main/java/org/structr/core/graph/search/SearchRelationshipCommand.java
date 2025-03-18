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
package org.structr.core.graph.search;

import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.common.SecurityContext;
import org.structr.core.graph.Factory;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.traits.Traits;

/**
 * Search for relationships by their attributes.
 */
public class SearchRelationshipCommand extends SearchCommand<Relationship, RelationshipInterface> {

	@Override
	public Factory<Relationship, RelationshipInterface> getFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page) {
		return new RelationshipFactory(securityContext, includeHidden, publicOnly, pageSize, page);
	}

	@Override
	public Index<Relationship> getIndex() {
		return  (Index<Relationship>) arguments.get("relationshipIndex");
	}

	@Override
	public boolean isRelationshipSearch() {
		return true;
	}
}
