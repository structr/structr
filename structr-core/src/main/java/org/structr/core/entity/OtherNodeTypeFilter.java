/**
 * Copyright (C) 2010-2015 Structr GmbH
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
package org.structr.core.entity;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;

/**
 *
 *
 */
public class OtherNodeTypeFilter implements Predicate<Relationship> {

	private Predicate<GraphObject> nodePredicate = null;
	private NodeFactory nodeFactory              = null;
	private Node thisNode                        = null;
	private Class desiredType                    = null;

	public OtherNodeTypeFilter(final SecurityContext securityContext, final Node thisNode, final Class desiredType) {
		this(securityContext, thisNode, desiredType, null);
	}

	public OtherNodeTypeFilter(final SecurityContext securityContext, final Node thisNode, final Class desiredType, final Predicate<GraphObject> nodePredicate) {

		this.nodePredicate = nodePredicate;
		this.nodeFactory   = new NodeFactory(securityContext);
		this.desiredType   = desiredType;
		this.thisNode      = thisNode;
	}

	@Override
	public boolean accept(final Relationship item) {

		final NodeInterface otherNode = nodeFactory.instantiate(item.getOtherNode(thisNode), item);

		// check predicate if exists
		if (otherNode != null && (nodePredicate == null || nodePredicate.accept(otherNode))) {

			final Class otherNodeType = otherNode.getClass();

			//final boolean desiredTypeIsAssignableFromOtherNodeType = desiredType.isAssignableFrom(otherNodeType);
			final boolean desiredTypeIsAssignableFromOtherNodeType = SearchCommand.getAllSubtypesAsStringSet(desiredType.getSimpleName()).contains(otherNodeType.getSimpleName());

			return desiredTypeIsAssignableFromOtherNodeType;
		}

		return false;
	}
}
