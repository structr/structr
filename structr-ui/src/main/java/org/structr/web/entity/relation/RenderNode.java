package org.structr.web.entity.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToMany;
import org.structr.core.graph.NodeInterface;
import org.structr.web.common.RelType;
import org.structr.web.entity.dom.DOMElement;

/**
 *
 * @author Christian Morgner
 */
public class RenderNode extends OneToMany<DOMElement, NodeInterface> {

	@Override
	public Class<DOMElement> getSourceType() {
		return DOMElement.class;
	}

	@Override
	public Class<NodeInterface> getTargetType() {
		return NodeInterface.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.RENDER_NODE;
	}
}
