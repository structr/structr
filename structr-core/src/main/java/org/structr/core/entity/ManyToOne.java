package org.structr.core.entity;

import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public abstract class ManyToOne<S extends NodeInterface, T extends NodeInterface> extends AbstractRelationship<S, T> implements Relation<S, T, ManyStartpoint<S>, OneEndpoint<T>> {

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public ManyStartpoint<S> getSource() {
		return new ManyStartpoint<>(getRelationshipType());
	}

	@Override
	public OneEndpoint<T> getTarget() {
		return new OneEndpoint<>(getRelationshipType());
	}
}
