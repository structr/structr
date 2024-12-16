package org.structr.web.traits.operations;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.entity.dom.DOMNode;

public abstract class UpdateFromNode extends FrameworkMethod<UpdateFromNode> {

	public abstract void updateFromNode(final NodeInterface node, final DOMNode otherNode) throws FrameworkException;
}
