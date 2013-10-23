package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 *
 * @author Christian Morgner
 */
public class LocationRelationship extends AbstractRelationship<AbstractNode, AbstractNode> {

	@Override
	public Class<AbstractNode> getSourceType() {
		return AbstractNode.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.IS_AT;
	}

	@Override
	public Class<AbstractNode> getDestinationType() {
		return AbstractNode.class;
	}
}
