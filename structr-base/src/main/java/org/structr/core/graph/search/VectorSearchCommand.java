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
package org.structr.core.graph.search;

import org.structr.api.UnknownClientException;
import org.structr.api.graph.Node;
import org.structr.api.index.Index;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nearest-neighbour search in a vector index, see PropertyKey.vectorIndexed(). Like the fulltext search,
 * the hits are instantiated with the security context of the command, so nodes the caller may not read
 * are left out: the result can have fewer than k entries.
 */
public class VectorSearchCommand extends NodeServiceCommand {

	public Map<NodeInterface, Double> getNodes(final String indexName, final int k, final double[] vector) throws FrameworkException {

		try {

			final NodeFactory factory                     = new NodeFactory(securityContext, true, false, Integer.MAX_VALUE, 1);
			final Index<Node> index                       = (Index<Node>) arguments.get("nodeIndex");
			final Map<Node, Double> result                = index.vectorQuery(indexName, k, vector);
			final Map<NodeInterface, Double> mappedResult = new LinkedHashMap<>();

			for (final Map.Entry<Node, Double> entry : result.entrySet()) {

				final NodeInterface node = factory.instantiateWithType(entry.getKey(), null, false);
				if (node != null) {

					mappedResult.put(node, entry.getValue());
				}
			}

			return mappedResult;

		} catch (UnknownClientException uclex) {

			throw new FrameworkException(422, uclex.getMessage());
		}
	}
}
