package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.LinkedListNode;
import org.structr.core.entity.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public class ListSibling extends OneToOne<LinkedListNode, LinkedListNode> {

	@Override
	public Class<LinkedListNode> getSourceType() {
		return LinkedListNode.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.CONTAINS_NEXT_CHILD;
	}

	@Override
	public Class<LinkedListNode> getTargetType() {
		return LinkedListNode.class;
	}
}
