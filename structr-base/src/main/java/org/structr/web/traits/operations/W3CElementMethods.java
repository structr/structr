package org.structr.web.traits.operations;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;
import org.w3c.dom.DOMException;

public abstract class W3CElementMethods extends FrameworkMethod<W3CElementMethods> {

	public abstract String getNodeName(final NodeInterface node);
	public abstract String getNodeValue(final NodeInterface node) throws DOMException;
	public abstract void setNodeValue(final NodeInterface node, final String nodeValue) throws DOMException;
	public abstract short getNodeType(final NodeInterface node);
}
