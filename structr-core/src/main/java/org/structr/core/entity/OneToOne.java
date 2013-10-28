package org.structr.core.entity;

import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public abstract class OneToOne<S extends NodeInterface, T extends NodeInterface> extends AbstractRelationship<S, T> implements Relation<S, T, OneStartpoint<S>, OneEndpoint<T>> {

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public OneStartpoint<S> getSource() {
		return new OneStartpoint<>(getRelationshipType());
	}

	@Override
	public OneEndpoint<T> getTarget() {
		return new OneEndpoint<>(getRelationshipType());
	}
}
