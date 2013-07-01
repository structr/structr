/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.graph.search;

import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.Factory;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipFactory;

/**
 * Search for relationships by their attributes.
 *
 * @author Axel Morgner
 */
public class SearchRelationshipCommand<T extends AbstractRelationship> extends SearchCommand<Relationship, T> {

	@Override
	public Factory<Relationship, T> getFactory(SecurityContext securityContext, boolean includeDeletedAndHidden, boolean publicOnly, int pageSize, int page, String offsetId) {
		return new RelationshipFactory(securityContext);
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
}
