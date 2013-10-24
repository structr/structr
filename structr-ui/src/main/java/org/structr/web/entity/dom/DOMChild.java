package org.structr.web.entity.dom;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.relationship.TreeChild;

/**
 *
 * @author Christian Morgner
 */
public class DOMChild extends TreeChild<DOMNode, DOMNode> {

	@Override
	public Class<DOMNode> getSourceType() {
		return DOMNode.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.CONTAINS;
	}

	@Override
	public Class<DOMNode> getDestinationType() {
		return DOMNode.class;
	}

	@Override
	public Class<? extends AbstractRelationship<DOMNode, DOMNode>> reverse() {
		return DOMChild.class;
	}
}
