package org.structr.core.traits;

import org.structr.core.graph.NodeInterface;

@FunctionalInterface
public interface TraitFactory {

	NodeTrait newInstance(final Traits traits, final NodeInterface node);
}
