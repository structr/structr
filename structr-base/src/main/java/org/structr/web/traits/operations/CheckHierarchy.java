package org.structr.web.traits.operations;

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.entity.dom.DOMNode;

public abstract class CheckHierarchy extends FrameworkMethod<CheckHierarchy> {

	public abstract void checkHierarchy(final DOMNode thisNode, final DOMNode otherNode) throws FrameworkException;
}
