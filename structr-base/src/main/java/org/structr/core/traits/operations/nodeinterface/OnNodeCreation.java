package org.structr.core.traits.operations.nodeinterface;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.ComposableOperation;

public interface OnNodeCreation extends ComposableOperation {

	void onNodeCreation(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException;
}
