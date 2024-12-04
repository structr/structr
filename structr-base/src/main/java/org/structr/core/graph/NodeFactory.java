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
package org.structr.core.graph;


import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractNode;

/**
 * A factory for Structr nodes.
 */
public class NodeFactory extends Factory<Node, NodeInterface> {

	public NodeFactory(final SecurityContext securityContext) {
		super(securityContext);
	}

	public NodeFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly) {
		super(securityContext, includeHidden, publicOnly);
	}

	public NodeFactory(final SecurityContext securityContext, final int pageSize, final int page) {
		super(securityContext, pageSize, page);
	}

	public NodeFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page) {
		super(securityContext, includeHidden, publicOnly, pageSize, page);
	}

	@Override
	public NodeInterface instantiateWithType(final Node node, final String nodeClass, final Identity pathSegmentId, boolean isCreation) {

		// cannot instantiate node without type
		if (nodeClass == null) {
			return null;
		}

		final AbstractNode newNode = new AbstractNode(securityContext, node, nodeClass, TransactionCommand.getCurrentTransactionId());

		newNode.setRawPathSegmentId(pathSegmentId);
		newNode.onNodeInstantiation(isCreation);

		// check access
		if (isCreation || securityContext.isReadable(newNode, includeHidden, publicOnly)) {

			return newNode;
		}

		return null;
	}

	// ----- protected methods -----
	@Override
	protected String determineActualType(final Node node) {

		// check deletion first
		if (TransactionCommand.isDeleted(node)) {
			return null;
		}

		if (node.hasProperty("type")) {

			final Object obj =  node.getProperty("type");
			if (obj != null) {

				return obj.toString();
			}
		}

		return null;
	}
}
