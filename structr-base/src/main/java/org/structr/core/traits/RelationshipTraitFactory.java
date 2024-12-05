package org.structr.core.traits;

import org.structr.core.graph.RelationshipInterface;

@FunctionalInterface
public interface RelationshipTraitFactory {

	RelationshipTrait newInstance(final Traits traits, final RelationshipInterface relationship);
}
