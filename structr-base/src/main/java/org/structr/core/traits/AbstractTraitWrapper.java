package org.structr.core.traits;

import org.structr.core.graph.NodeInterface;

public abstract class AbstractTraitWrapper {

	protected final NodeInterface nodeInterface;
	protected final Traits traits;

	public AbstractTraitWrapper(final Traits traits, final NodeInterface nodeInterface) {

		this.nodeInterface = nodeInterface;
		this.traits        = traits;
	}

	public String getUuid() {
		return nodeInterface.getUuid();
	}

	public String getName() {
		return nodeInterface.getName();
	}

	public String getType() {
		return nodeInterface.getType();
	}

	public NodeInterface getWrappedNode() {
		return nodeInterface;
	}
}
