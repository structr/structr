package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.OneToMany;

/**
 *
 * @author Christian Morgner
 */
public class Parentship extends OneToMany<AbstractNode, AbstractNode> {

	@Override
	public Class<AbstractNode> getSourceType() {
		return AbstractNode.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.CONTAINS;
	}

	@Override
	public Class<AbstractNode> getTargetType() {
		return AbstractNode.class;
	}

	@Override
	public Class<? extends AbstractRelationship<AbstractNode, AbstractNode>> reverse() {
		return Parentship.class;
	}

}
