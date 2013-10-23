package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Principal;

/**
 *
 * @author Christian Morgner
 */
public class Ownership extends AbstractRelationship<AbstractNode, Principal> {

	@Override
	public Class<Principal> getDestinationType() {
		return Principal.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.OWNS;
	}

	@Override
	public Class<AbstractNode> getSourceType() {
		return AbstractNode.class;
	}
}
