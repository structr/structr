package org.structr.core.entity.relationship;

import org.structr.core.entity.ManyToMany;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

/**
 *
 * @author Christian Morgner
 */
public class NodeHasLocation extends ManyToMany<NodeInterface, NodeInterface> {

	@Override
	public Class<NodeInterface> getSourceType() {
		return NodeInterface.class;
	}

	@Override
	public String name() {
		return "IS_AT";
	}

	@Override
	public Class<NodeInterface> getTargetType() {
		return NodeInterface.class;
	}

	@Override
	public SourceId getSourceIdProperty() {
		return null;
	}

	@Override
	public TargetId getTargetIdProperty() {
		return null;
	}
}
