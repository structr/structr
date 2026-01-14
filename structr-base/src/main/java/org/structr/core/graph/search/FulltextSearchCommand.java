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

import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class FulltextSearchCommand extends NodeServiceCommand {

	public Map<NodeInterface, Double> getNodes(final String indexName, final String searchString, final int pageSize, final int page) throws FrameworkException {

		try {

			final NodeFactory factory = new NodeFactory(securityContext, true, false, pageSize, page);
			final Index<Node> index = (Index<Node>) arguments.get("nodeIndex");
			final Map<Node, Double> result = index.fulltextQuery(indexName, searchString);
			final Map<NodeInterface, Double> mappedResult = new LinkedHashMap<>();

			for (final Map.Entry<Node, Double> entry : result.entrySet()) {

				final NodeInterface node = factory.instantiateWithType(entry.getKey(), null, false);
				if (node != null) {

					mappedResult.put(node, entry.getValue());
				}
			}

			return mappedResult;

		} catch (Throwable t) {
			throw new FrameworkException(422, t.getMessage());
		}
	}

	public Map<RelationshipInterface, Double> getRelationships(final String indexName, final String searchString, final int pageSize, final int page) throws FrameworkException {

		try {

			final RelationshipFactory factory                     = new RelationshipFactory(securityContext, true, false, pageSize, page);
			final Index<Relationship> index                       = (Index<Relationship>) arguments.get("relationshipIndex");
			final Map<Relationship, Double> result                = index.fulltextQuery(indexName, searchString);
			final Map<RelationshipInterface, Double> mappedResult = new LinkedHashMap<>();

			for (final Map.Entry<Relationship, Double> entry : result.entrySet()) {

				final RelationshipInterface node = factory.instantiateWithType(entry.getKey(), null, false);
				if (node != null) {

					mappedResult.put(node, entry.getValue());
				}
			}

			return mappedResult;

		} catch (Throwable t) {
			throw new FrameworkException(422, t.getMessage());
		}
	}
}
