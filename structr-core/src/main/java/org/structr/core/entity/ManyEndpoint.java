package org.structr.core.entity;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public class ManyEndpoint<T extends NodeInterface> extends Endpoint implements Target<Iterable<Relationship>, Iterable<T>> {
	
	public ManyEndpoint(final RelationshipType relType) {
		super(relType);
	}

	@Override
	public Iterable<T> get(SecurityContext securityContext, NodeInterface node) {
		
		final NodeFactory<T> nodeFactory  = new NodeFactory<>(securityContext);
		final Iterable<Relationship> rels = getRaw(node.getNode());
		
		if (rels != null) {
			
			return Iterables.map(nodeFactory, Iterables.map(new Function<Relationship, Node>() {

				@Override
				public Node apply(Relationship from) {
					return from.getEndNode();
				}
				
			}, rels));
		}
		
		return null;
	}

	@Override
	public void set(SecurityContext securityContext, NodeInterface node, Iterable<T> value) throws FrameworkException {

//		if (obj instanceof AbstractNode) {
//			
//			Set<T> toBeDeleted = new LinkedHashSet<T>(getProperty(securityContext, obj, true));
//			Set<T> toBeCreated = new LinkedHashSet<T>();
//			AbstractNode sourceNode = (AbstractNode)obj;
//
//			if (collection != null) {
//				toBeCreated.addAll(collection);
//			}
//			
//			// create intersection of both sets
//			Set<T> intersection = new LinkedHashSet<T>(toBeCreated);
//			intersection.retainAll(toBeDeleted);
//			
//			// intersection needs no change
//			toBeCreated.removeAll(intersection);
//			toBeDeleted.removeAll(intersection);
//			
//			// remove existing relationships
//			for (T targetNode : toBeDeleted) {
//
//				removeRelationship(securityContext, sourceNode, (AbstractNode)targetNode);
//			}
//			
//			// create new relationships
//			for (T targetNode : toBeCreated) {
//
//				createRelationship(securityContext, sourceNode, (AbstractNode)targetNode);
//			}
//			
//		} else {
//
//			logger.log(Level.WARNING, "Property {0} is registered on illegal type {1}", new Object[] { this, obj.getClass() } );
//		}
	}

	@Override
	public Iterable<Relationship> getRaw(final Node dbNode) {
		return dbNode.getRelationships(relType, Direction.OUTGOING);
	}
}
