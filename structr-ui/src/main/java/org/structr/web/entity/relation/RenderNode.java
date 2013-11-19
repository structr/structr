package org.structr.web.entity.relation;

import org.structr.core.entity.OneToMany;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
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
	public String name() {
		return "RENDER_NODE";
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return null;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return null;
	}
}
