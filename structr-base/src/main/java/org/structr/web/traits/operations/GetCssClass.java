package org.structr.web.traits.operations;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetCssClass extends FrameworkMethod<GetCssClass> {

	public abstract String getCssClass(final NodeInterface node);
}
