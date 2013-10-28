package org.structr.core.entity;

import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public abstract class OneToMany<S extends NodeInterface, T extends NodeInterface> extends AbstractRelationship<S, T> implements Relation<S, T, OneStartpoint<S>, ManyEndpoint<T>> {

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public OneStartpoint<S> getSource() {
		return new OneStartpoint<>(getRelationshipType());
	}

	@Override
	public ManyEndpoint<T> getTarget() {
		return new ManyEndpoint<>(getRelationshipType());
	}
}
