package org.structr.web.traits.operations;

import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetHtmlAttributes extends FrameworkMethod<GetHtmlAttributes> {

	public abstract Iterable<PropertyKey> getHtmlAttributes(final NodeInterface node);
}
