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


import org.structr.api.graph.Node;
import org.structr.api.index.Index;
import org.structr.common.SecurityContext;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 */
public class SearchNodeCommand extends SearchCommand<Node, NodeInterface> {

	@Override
	public NodeFactory getFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page) {
		return new NodeFactory(securityContext, includeHidden, publicOnly, pageSize, page);
	}

	@Override
	public Index<Node> getIndex(final boolean isFulltextSearch) {
		if (isFulltextSearch) {
			return (Index<Node>) arguments.get("fulltextIndex");
		}
		return  (Index<Node>) arguments.get("nodeIndex");
	}

	@Override
	public boolean isRelationshipSearch(){
		return false;
	}
}
