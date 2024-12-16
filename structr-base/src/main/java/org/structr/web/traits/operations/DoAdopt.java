package org.structr.web.traits.operations;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.entity.dom.Page;
import org.w3c.dom.DOMException;

public abstract class DoAdopt extends FrameworkMethod<DoAdopt> {

	public abstract void doAdopt(final NodeInterface node, final Page page) throws DOMException;
}
