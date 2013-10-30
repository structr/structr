package org.structr.core.entity;

import org.structr.common.error.CardinalityToken;
import org.structr.common.error.FrameworkException;
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
		return new OneStartpoint<>(this);
	}

	@Override
	public ManyEndpoint<T> getTarget() {
		return new ManyEndpoint<>(this);
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
		
		final Class<? extends OneToMany> clazz = this.getClass();
		final Class<S> sourceType              = getSourceType();
		final Class<T> targetType              = getTargetType();

		// check existing relationships
		final Relation<?, T, ?, ?> incomingRel = targetNode.getIncomingRelationship(clazz);

		boolean error = false;

		// check for error
		error |= (incomingRel != null && incomingRel.getSourceNode().getClass().equals(sourceType));

		if (error) {

			final String msg = targetType.getSimpleName() + " can only have exactly one " + sourceType.getSimpleName();
			
			throw new FrameworkException(clazz.getSimpleName(), new CardinalityToken(msg));
		}
	}
}
