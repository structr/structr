package org.structr.core.entity;

import org.neo4j.graphdb.Direction;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
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
	public void ensureCardinality(final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {
		
		final App app                          = StructrApp.getInstance(securityContext);
		final Class<? extends OneToMany> clazz = this.getClass();
		final Class<S> sourceType              = getSourceType();


		// check existing relationships
		final Relation<?, T, ?, ?> incomingRel = targetNode.getIncomingRelationship(clazz);
		if (incomingRel != null && incomingRel.getSourceType().isAssignableFrom(sourceType)) {

			try {

				app.beginTx();
				app.delete(incomingRel);
				app.commitTx();

			} finally {

				app.finishTx();
			}
		}
	}
	
}