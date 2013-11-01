package org.structr.core.entity;

import org.structr.common.error.DuplicateRelationshipToken;
import org.structr.common.error.FrameworkException;
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
		return new ManyStartpoint<>(this);
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
		
		// prevent duplicates from being created
		final Class<? extends ManyToMany> clazz = this.getClass();

		// check existing relationships
		for (final AbstractRelationship rel : sourceNode.getOutgoingRelationships()) {
		
			if (rel.getTargetNode().equals(targetNode)) {
				
				throw new FrameworkException(clazz.getSimpleName(), new DuplicateRelationshipToken("This relationship already exists"));
			}
		}
		
	}
}
