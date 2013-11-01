package org.structr.web.entity.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToOne;
import org.structr.core.graph.NodeInterface;
import org.structr.web.common.RelType;

/**
 *
 * @author Christian Morgner
 */
public abstract class LinkRelationship<S extends NodeInterface, T extends NodeInterface> extends OneToOne<S, T> {

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.LINK;
	}
}
