package org.structr.core.traits.operations.nodeinterface;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

import java.util.Map;

public abstract class VisitForUsage extends FrameworkMethod<VisitForUsage> {

	public abstract void visitForUsage(final NodeInterface obj, final Map<String, Object> data);
}
