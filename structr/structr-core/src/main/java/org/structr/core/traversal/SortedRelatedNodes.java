/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.traversal;

import org.structr.core.predicate.TypePredicate;
import java.util.Comparator;
import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 *
 * @author Christian Morgner
 */
public class SortedRelatedNodes extends AbstractNodeCollector {

	private int count = 0;
	
	public SortedRelatedNodes(RelationshipType relType, Direction direction, Class resultType, int maxDepth, int count) {
		this(null, relType, direction, resultType, maxDepth, count);
	}
	
	public SortedRelatedNodes(Comparator<AbstractNode> comparator, RelationshipType relType, Direction direction, Class resultType, int maxDepth, int count) {
		this(comparator, relType, direction, resultType, new ObjectNotion(), maxDepth, count);
	}

	public SortedRelatedNodes(Comparator<AbstractNode> comparator, RelationshipType relType, Direction direction, Class resultType, Notion notion, int maxDepth, int count) {

		super(relType, direction, maxDepth);

		// add type predicate
		if(resultType != null) {
			this.addPredicate(new TypePredicate(resultType.getSimpleName()));
		}

		// set comparator for sorting
		this.setComparator(comparator);

		this.setNotion(notion);
		this.count = count;
	}

	@Override
	public List transformResult(List<AbstractNode> result) {

		// truncate list to length count
		return result.subList(0, Math.min(result.size(), count));
	}

	@Override
	public void cleanup() {
	}
}
