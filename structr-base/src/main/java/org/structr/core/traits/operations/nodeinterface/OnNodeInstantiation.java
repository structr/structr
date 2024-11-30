package org.structr.core.traits.operations.nodeinterface;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.ComposableOperation;

public interface OnNodeInstantiation extends ComposableOperation {

	void onNodeInstantiation(final NodeInterface nodeInterface, final boolean isCreation);
}
