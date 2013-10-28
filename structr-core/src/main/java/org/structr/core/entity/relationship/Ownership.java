package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Principal;

/**
 *
 * @author Christian Morgner
 */
public class Ownership extends OneToMany<Principal, AbstractNode> {

	@Override
	public Class<Principal> getSourceType() {
		return Principal.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.OWNS;
	}

	@Override
	public Class<AbstractNode> getTargetType() {
		return AbstractNode.class;
	}
}
