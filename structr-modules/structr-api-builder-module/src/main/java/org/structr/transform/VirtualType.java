package org.structr.transform;

import org.structr.common.ResultTransformer;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.NodeTrait;

public interface VirtualType extends NodeTrait, ResultTransformer {

	String getSourceType();
	Integer getPosition();
	String getFilterExpression();
	Iterable<NodeInterface> getVirtualProperties();
}
