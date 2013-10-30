package org.structr.core.entity;

import org.structr.common.error.CardinalityToken;
import org.structr.common.error.FrameworkException;
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
		return new OneStartpoint<>(this);
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

		final Class<? extends OneToOne> clazz = getClass();
		final Class<S> sourceType             = getSourceType();
		final Class<T> targetType             = getTargetType();

		// check existing relationships
		final Relation<S, ?, ?, ?> outgoingRel = sourceNode.getOutgoingRelationship(clazz);
		final Relation<?, T, ?, ?> incomingRel = targetNode.getIncomingRelationship(clazz);

		boolean error = false;

		// check for error
		error |= (outgoingRel != null && outgoingRel.getTargetNode().getClass().equals(targetType));
		error |= (incomingRel != null && incomingRel.getSourceNode().getClass().equals(sourceType));

		if (error) {

			final String msg = targetType.getSimpleName() + " can only have exactly one " + sourceType.getSimpleName();
			
			throw new FrameworkException(clazz.getSimpleName(), new CardinalityToken(msg));
		}
	}
}
