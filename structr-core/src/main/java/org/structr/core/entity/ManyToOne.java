package org.structr.core.entity;

import org.structr.common.error.CardinalityToken;
import org.structr.common.error.FrameworkException;
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
		return new ManyStartpoint<>(this);
	}

	@Override
	public OneEndpoint<T> getTarget() {
		return new OneEndpoint<>(this);
	}
	
	@Override
	public int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

	@Override
	public void checkMultiplicity(final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {

		/**
		 * OneToMany means that the target node may only have
		 * one incoming relationship of a given 1:n type
		 */
		
		final Class<? extends ManyToOne> clazz = this.getClass();
		final Class<S> sourceType              = getSourceType();
		final Class<T> targetType              = getTargetType();

		// check existing relationships
		final Relation<?, T, ?, ?> outgoing = targetNode.getOutgoingRelationship(clazz);

		boolean error = false;

		// check for error
		error |= (outgoing != null && outgoing.getTargetNode().getClass().equals(targetType));

		if (error) {

			final String msg = targetType.getSimpleName() + " can only have exactly one " + sourceType.getSimpleName();
			
			throw new FrameworkException(clazz.getSimpleName(), new CardinalityToken(msg));
		}
	}
}
