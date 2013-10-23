package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.LinkedTreeNode;

/**
 *
 * @author Christian Morgner
 */
public class TreeChild extends AbstractRelationship<LinkedTreeNode, LinkedTreeNode> {

	@Override
	public Class<LinkedTreeNode> getSourceType() {
		return LinkedTreeNode.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.CONTAINS;
	}

	@Override
	public Class<LinkedTreeNode> getDestinationType() {
		return LinkedTreeNode.class;
	}

	@Override
	public Class<? extends AbstractRelationship<LinkedTreeNode, LinkedTreeNode>> reverse() {
		return TreeChild.class;
	}
}
