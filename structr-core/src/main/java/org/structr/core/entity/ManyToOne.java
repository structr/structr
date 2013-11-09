package org.structr.core.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
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
	public void ensureCardinality(final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {
		
		final App app                          = StructrApp.getInstance(securityContext);
		final Class<? extends ManyToOne> clazz = this.getClass();
		final Class<S> sourceType              = getSourceType();


		// check existing relationships
		final Relation<?, T, ?, ?> outgoingRel = sourceNode.getOutgoingRelationship(clazz);
		if (outgoingRel != null && outgoingRel.getTargetNode().getClass().equals(sourceType)) {

			try {

				app.beginTx();
				app.delete(outgoingRel);
				app.commitTx();

			} finally {

				app.finishTx();
			}
		}
	}
	
}