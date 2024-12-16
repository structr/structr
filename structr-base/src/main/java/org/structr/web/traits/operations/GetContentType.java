package org.structr.web.traits.operations;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetContentType extends FrameworkMethod<GetContentType> {

	public abstract String getContentType(final NodeInterface node);
}
