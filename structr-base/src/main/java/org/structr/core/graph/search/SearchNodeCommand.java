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
 * Search for nodes by their attributes.
 * <p>
 * The execute method takes four parameters:
 * <p>
 * <ol>
 * <li>top node: search only below this node
 *     <p>if null, search everywhere (top node = root node)
 * <li>boolean include deleted and hidden: if true, return deleted and hidden nodes as well
 * <li>boolean public only: if true, return only public nodes
 * <li>List&lt;TextualSearchAttribute> search attributes: key/value pairs with search operator
 *    <p>if no TextualSearchAttribute is given, return any node matching the other
 *       search criteria
 * </ol>
 */
public class SearchNodeCommand extends SearchCommand<Node, NodeInterface> {

	@Override
	public NodeFactory getFactory(SecurityContext securityContext, boolean includeHidden, boolean publicOnly, int pageSize, int page) {
		return new NodeFactory(securityContext, includeHidden, publicOnly, pageSize, page);
	}

	@Override
	public Index<Node> getIndex() {
		return  (Index<Node>) arguments.get("nodeIndex");
	}

	@Override
	public boolean isRelationshipSearch(){
		return false;
	}
}
