package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.ManyEndpoint;
import org.structr.core.entity.ManyStartpoint;
import org.structr.core.entity.ManyToMany;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public class LocationRelationship extends ManyToMany<NodeInterface, NodeInterface> {

	@Override
	public Class<NodeInterface> getSourceType() {
		return NodeInterface.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.IS_AT;
	}

	@Override
	public Class<NodeInterface> getTargetType() {
		return NodeInterface.class;
	}

	@Override
	public ManyStartpoint<NodeInterface> getSource() {
		return new ManyStartpoint<>(getRelationshipType());
	}

	@Override
	public ManyEndpoint<NodeInterface> getTarget() {
		return new ManyEndpoint<>(getRelationshipType());
	}
}
