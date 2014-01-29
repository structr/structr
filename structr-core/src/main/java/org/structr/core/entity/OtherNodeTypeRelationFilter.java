package org.structr.core.entity;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public class OtherNodeTypeRelationFilter implements Predicate<Relation> {

	private NodeFactory nodeFactory = null;
	private Node thisNode           = null;
	private Class desiredType       = null;

	public OtherNodeTypeRelationFilter(final SecurityContext securityContext, final Node thisNode, final Class desiredType) {

		this.nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
		this.desiredType = desiredType;
		this.thisNode    = thisNode;
	}

	@Override
	public boolean accept(final Relation rel) {

		try {
			final NodeInterface otherNode = nodeFactory.instantiate(rel.getRelationship().getOtherNode(thisNode));
			final Class otherNodeType     = otherNode.getClass();

			return desiredType.isAssignableFrom(otherNodeType) || otherNodeType.isAssignableFrom(desiredType);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return false;
	}
}
