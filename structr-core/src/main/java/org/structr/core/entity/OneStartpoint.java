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
public class OneStartpoint<S extends NodeInterface> extends Endpoint implements Source<Relationship, S> {

	public OneStartpoint(final RelationshipType relType) {
		super(relType);
	}
	
	@Override
	public S get(final SecurityContext securityContext, final NodeInterface node) {
		
		final NodeFactory<S> nodeFactory = new NodeFactory<>(securityContext);
		final Relationship rel           = getRaw(node.getNode());
		
		if (rel != null) {
			return nodeFactory.adapt(rel.getStartNode());
		}
		
		return null;
	}

	@Override
	public void set(final SecurityContext securityContext, final NodeInterface node, final S value) throws FrameworkException {
	}

	@Override
	public Relationship getRaw(final Node dbNode) {
		return dbNode.getSingleRelationship(relType, Direction.INCOMING);
	}
}
