package org.structr.core.entity;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public class OtherNodeTypeFilter implements Predicate<Relationship> {

	private Predicate<GraphObject> nodePredicate = null;
	private NodeFactory nodeFactory              = null;
	private Node thisNode                        = null;
	private Class desiredType                    = null;

	public OtherNodeTypeFilter(final SecurityContext securityContext, final Node thisNode, final Class desiredType) {
		this(securityContext, thisNode, desiredType, null);
	}
	
	public OtherNodeTypeFilter(final SecurityContext securityContext, final Node thisNode, final Class desiredType, final Predicate<GraphObject> nodePredicate) {

		this.nodePredicate = nodePredicate;
		this.nodeFactory   = new NodeFactory(SecurityContext.getSuperUserInstance());
		this.desiredType   = desiredType;
		this.thisNode      = thisNode;
	}

	@Override
	public boolean accept(final Relationship item) {

		try {
			final NodeInterface otherNode = nodeFactory.instantiate(item.getOtherNode(thisNode));

			// check predicate if exists
			if (nodePredicate == null || nodePredicate.accept(otherNode)) {
				
				final Class otherNodeType = otherNode.getClass();

				return desiredType.isAssignableFrom(otherNodeType) || otherNodeType.isAssignableFrom(desiredType);
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return false;
	}
}
