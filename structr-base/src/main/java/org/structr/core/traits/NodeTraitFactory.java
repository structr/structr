package org.structr.core.traits;

import org.structr.core.graph.NodeInterface;

@FunctionalInterface
public interface NodeTraitFactory {

	NodeTrait newInstance(final Traits traits, final NodeInterface node);
}
