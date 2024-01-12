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
package org.structr.core.entity;

import org.structr.api.Predicate;
import org.structr.api.graph.Node;
import org.structr.common.SecurityContext;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 *
 */
public class OtherNodeTypeRelationFilter implements Predicate<Relation> {

	private NodeFactory nodeFactory = null;
	private Node thisNode           = null;
	private Class desiredType       = null;

	public OtherNodeTypeRelationFilter(final SecurityContext securityContext, final Node thisNode, final Class desiredType) {

		this.nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
		this.desiredType = desiredType;
		this.thisNode    = thisNode;
	}

	@Override
	public boolean accept(final Relation rel) {

		final NodeInterface otherNode = nodeFactory.instantiate(rel.getRelationship().getOtherNode(thisNode));
		final Class otherNodeType     = otherNode.getClass();

		return desiredType.isAssignableFrom(otherNodeType) || otherNodeType.isAssignableFrom(desiredType);
	}
}
