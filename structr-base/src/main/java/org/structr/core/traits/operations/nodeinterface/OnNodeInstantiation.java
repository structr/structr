package org.structr.core.traits.operations.nodeinterface;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.LifecycleMethod;

public interface OnNodeInstantiation extends LifecycleMethod {

	void onNodeInstantiation(final NodeInterface nodeInterface, final boolean isCreation);
}
