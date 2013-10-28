package org.structr.core.entity;

import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public abstract class ManyToMany<S extends NodeInterface, T extends NodeInterface> extends AbstractRelationship<S, T> implements Relation<S, T, ManyStartpoint<S>, ManyEndpoint<T>> {

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public ManyStartpoint<S> getSource() {
		return new ManyStartpoint<>(getRelationshipType());
	}

	@Override
	public ManyEndpoint<T> getTarget() {
		return new ManyEndpoint<>(getRelationshipType());
	}
}
