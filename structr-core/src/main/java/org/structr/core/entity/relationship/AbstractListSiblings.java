package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.LinkedListNode;
import org.structr.core.entity.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractListSiblings<S extends LinkedListNode, T extends LinkedListNode> extends OneToOne<S, T> {

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.CONTAINS_NEXT_CHILD;
	}
}
