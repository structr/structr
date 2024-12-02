package org.structr.core.traits.operations.accesscontrollable;

import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetOwnerNode extends FrameworkMethod {

	public abstract Principal getOwnerNode(final NodeInterface nodeInterface);
}
