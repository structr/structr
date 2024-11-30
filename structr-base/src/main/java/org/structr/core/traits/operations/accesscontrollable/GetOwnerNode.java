package org.structr.core.traits.operations.accesscontrollable;

import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.OverwritableOperation;

public abstract class GetOwnerNode extends OverwritableOperation {

	public abstract Principal getOwnerNode(final NodeInterface nodeInterface);
}
