package org.structr.core.entity;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public class OneEndpoint<T extends NodeInterface> extends Endpoint implements Target<Relationship, T> {
	
	public OneEndpoint(final RelationshipType relType) {
		super(relType);
	}

	@Override
	public T get(final SecurityContext securityContext, final NodeInterface node) {
		
		final NodeFactory<T> nodeFactory = new NodeFactory<>(securityContext);
		final Relationship rel           = getRaw(node.getNode());
		
		if (rel != null) {
			return nodeFactory.adapt(rel.getEndNode());
		}
		
		return null;
	}

	@Override
	public void set(final SecurityContext securityContext, final NodeInterface node, final T value) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Relationship getRaw(Node dbNode) {
		return dbNode.getSingleRelationship(relType, Direction.OUTGOING);
	}
}
