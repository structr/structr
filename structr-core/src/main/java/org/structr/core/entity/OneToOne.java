package org.structr.core.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
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
	public void ensureCardinality(final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {

		final App app                         = StructrApp.getInstance(securityContext);
		final Class<? extends OneToOne> clazz = getClass();
		final Class<S> sourceType             = getSourceType();
		final Class<T> targetType             = getTargetType();

		// check existing relationships
		final Relation<S, ?, ?, ?> outgoingRel = sourceNode.getOutgoingRelationship(clazz);
		final Relation<?, T, ?, ?> incomingRel = targetNode.getIncomingRelationship(clazz);

		// remove relationship if exists
		if (outgoingRel != null && outgoingRel.getTargetNode().getClass().equals(targetType)) {
			app.delete(outgoingRel);
		}
		
		if (incomingRel != null && incomingRel.getSourceNode().getClass().equals(sourceType)) {
			app.delete(incomingRel);
		}
	}
}
