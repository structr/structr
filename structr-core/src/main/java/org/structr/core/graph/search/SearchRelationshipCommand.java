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
package org.structr.core.graph.search;

import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.structr.common.SecurityContext;
import org.structr.core.graph.Factory;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;

/**
 * Search for relationships by their attributes.
 *
 *
 */
public class SearchRelationshipCommand<T extends RelationshipInterface> extends SearchCommand<Relationship, T> {

	@Override
	public Factory<Relationship, T> getFactory(SecurityContext securityContext, boolean includeDeletedAndHidden, boolean publicOnly, int pageSize, int page, String offsetId) {
		return new RelationshipFactory(securityContext, includeDeletedAndHidden, publicOnly, pageSize, page, offsetId);
	}

	@Override
	public Index<Relationship> getFulltextIndex() {
		return  (Index<Relationship>) arguments.get(NodeService.RelationshipIndex.rel_fulltext.name());
	}

	@Override
	public Index<Relationship> getKeywordIndex() {
		return  (Index<Relationship>) arguments.get(NodeService.RelationshipIndex.rel_keyword.name());
	}

	@Override
	public LayerNodeIndex getSpatialIndex() {
		return null;
	}

	@Override
	public boolean isRelationshipSearch() {
		return true;
	}
}
