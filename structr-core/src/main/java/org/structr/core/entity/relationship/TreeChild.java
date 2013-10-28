package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.OneToMany;

/**
 *
 * @author Christian Morgner
 */
public class TreeChild extends OneToMany<LinkedTreeNode, LinkedTreeNode> {

	@Override
	public Class<LinkedTreeNode> getSourceType() {
		return LinkedTreeNode.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.CONTAINS;
	}

	@Override
	public Class<LinkedTreeNode> getTargetType() {
		return LinkedTreeNode.class;
	}
}
