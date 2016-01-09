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
package org.structr.core.traversal;

import java.util.Comparator;
import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;
import org.structr.core.predicate.TypePredicate;

/**
 * A node collector that collects related nodes of a given result type with a
 * traversal of the given relationship type and direction and sorts them
 * using the given Comparator.
 *
 *
 */
public class SortedRelatedNodes<T extends AbstractNode> extends AbstractNodeCollector<T> {

	private Class resultType = null;
	private int count = 0;
	
	public SortedRelatedNodes(RelationshipType relType, Direction direction, Class resultType, int maxDepth, int count) {
		this(null, relType, direction, resultType, maxDepth, count);
	}
	
	public SortedRelatedNodes(RelationshipType relType, Direction direction, Class resultType, Notion notion, int maxDepth, int count) {
		this(null, relType, direction, resultType, notion, maxDepth, count);
	}

	public SortedRelatedNodes(Comparator<T> comparator, RelationshipType relType, Direction direction, Class resultType, int maxDepth, int count) {
		this(comparator, relType, direction, resultType, new ObjectNotion(), maxDepth, count);
	}

	public SortedRelatedNodes(Comparator<T> comparator, RelationshipType relType, Direction direction, Class resultType, Notion notion, int maxDepth, int count) {

		super(relType, direction, maxDepth);

		// add type predicate
		if(resultType != null) {

			this.addPredicate(new TypePredicate(resultType.getSimpleName()));
			this.resultType = resultType;
		}

		// set comparator for sorting
		this.setComparator(comparator);

		this.setNotion(notion);
		this.count = count;
	}

	@Override
	public List transformResult(List<T> result) {

		// truncate list to length count
		return result.subList(0, Math.min(result.size(), count));
	}

	@Override
	public void cleanup() {
	}

	@Override
	public Class getResultType() {
		return resultType;
	}
}
