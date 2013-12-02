package org.structr.core.entity;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public class OneEndpoint<T extends NodeInterface> extends AbstractEndpoint implements Target<Relationship, T> {
	
	private Relation<?, T, ?, OneEndpoint<T>> relation = null;
	
	public OneEndpoint(final Relation<?, T, ?, OneEndpoint<T>> relation) {
		this.relation = relation;
	}

	@Override
	public T get(final SecurityContext securityContext, final NodeInterface node) {
		
		final NodeFactory<T> nodeFactory = new NodeFactory<>(securityContext);
		final Relationship rel           = getRawSource(securityContext, node.getNode());
		
		if (rel != null) {
			return nodeFactory.adapt(rel.getEndNode());
		}
		
		return null;
	}

	@Override
	public void set(final SecurityContext securityContext, final NodeInterface sourceNode, final T targetNode) throws FrameworkException {
		
		// let relation check multiplicity
		relation.ensureCardinality(securityContext, sourceNode, targetNode);

		if (targetNode != null) {

			// create new relationship
			StructrApp.getInstance(securityContext).create(sourceNode, targetNode, relation.getClass(), getNotionProperties(securityContext, relation.getClass(), targetNode.getUuid()));
		}
	}

	@Override
	public Relationship getRawSource(final SecurityContext securityContext, final Node dbNode) {
		return getSingle(securityContext, dbNode, relation, Direction.OUTGOING, relation.getTargetType());
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode) {
		return getRawSource(securityContext, dbNode) != null;
	}
}
