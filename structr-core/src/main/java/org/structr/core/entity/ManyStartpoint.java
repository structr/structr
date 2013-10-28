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
public class ManyStartpoint<S extends NodeInterface> extends Endpoint implements Source<Iterable<Relationship>, Iterable<S>> {

	public ManyStartpoint(final RelationshipType relType) {
		super(relType);
	}
	
	@Override
	public Iterable<S> get(final SecurityContext securityContext, final NodeInterface node) {
		
		final NodeFactory<S> nodeFactory  = new NodeFactory<>(securityContext);
		final Iterable<Relationship> rels = getRaw(node.getNode());
		
		if (rels != null) {
			
			return Iterables.map(nodeFactory, Iterables.map(new Function<Relationship, Node>() {

				@Override
				public Node apply(Relationship from) {
					return from.getStartNode();
				}
				
			}, rels));
		}
		
		return null;
	}

	@Override
	public void set(final SecurityContext securityContext, final NodeInterface node, final Iterable<S> value) throws FrameworkException {

//		if (obj instanceof AbstractNode) {
//			
//			Set<S> toBeDeleted = new LinkedHashSet<S>(getProperty(securityContext, obj, true));
//			Set<S> toBeCreated = new LinkedHashSet<S>();
//			AbstractNode sourceNode = (AbstractNode)obj;
//
//			if (collection != null) {
//				toBeCreated.addAll(collection);
//			}
//			
//			// create intersection of both sets
//			Set<S> intersection = new LinkedHashSet<S>(toBeCreated);
//			intersection.retainAll(toBeDeleted);
//			
//			// intersection needs no change
//			toBeCreated.removeAll(intersection);
//			toBeDeleted.removeAll(intersection);
//			
//			// remove existing relationships
//			for (S targetNode : toBeDeleted) {
//
//				removeRelationship(securityContext, sourceNode, (AbstractNode)targetNode);
//			}
//			
//			// create new relationships
//			for (S targetNode : toBeCreated) {
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
		return dbNode.getRelationships(relType, Direction.INCOMING);
	}
}
