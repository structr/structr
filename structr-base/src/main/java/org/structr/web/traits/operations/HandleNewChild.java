package org.structr.web.traits.operations;

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.entity.dom.DOMNode;
import org.w3c.dom.Node;

public abstract class HandleNewChild extends FrameworkMethod<HandleNewChild> {

	public abstract void handleNewChild(final DOMNode node, final DOMNode newChild) throws FrameworkException;
}
