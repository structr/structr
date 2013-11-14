package org.structr.core.entity;

import java.util.LinkedHashSet;
import java.util.Set;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class ManyEndpoint<T extends NodeInterface> extends AbstractEndpoint implements Target<Iterable<Relationship>, Iterable<T>> {

	private Relation<?, T, ?, ManyEndpoint<T>> relation = null;
	
	public ManyEndpoint(final Relation<?, T, ?, ManyEndpoint<T>> relation) {
		this.relation = relation;
	}

	@Override
	public Iterable<T> get(SecurityContext securityContext, NodeInterface node) {
		
		final NodeFactory<T> nodeFactory  = new NodeFactory<>(securityContext);
		final Iterable<Relationship> rels = getRawSource(securityContext, node.getNode());
		
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
	public void set(final SecurityContext securityContext, final NodeInterface sourceNode, final Iterable<T> collection) throws FrameworkException {

		final App app            = StructrApp.getInstance(securityContext);
		final Set<T> toBeDeleted = new LinkedHashSet<>(Iterables.toList(get(securityContext, sourceNode)));
		final Set<T> toBeCreated = new LinkedHashSet<>();

		if (collection != null) {
			Iterables.addAll(toBeCreated, collection);
		}

		// create intersection of both sets
		final Set<T> intersection = new LinkedHashSet<>(toBeCreated);
		intersection.retainAll(toBeDeleted);

		// intersection needs no change
		toBeCreated.removeAll(intersection);
		toBeDeleted.removeAll(intersection);
		
		// remove existing relationships
		for (T targetNode : toBeDeleted) {

			for (AbstractRelationship rel : sourceNode.getOutgoingRelationships()) {

				if (rel.getRelType().equals(relation) && rel.getTargetNode().equals(targetNode)) {

					app.delete(rel);
				}

			}
		}

		// create new relationships
		for (T targetNode : toBeCreated) {

			relation.ensureCardinality(sourceNode, targetNode);

			app.create(sourceNode, targetNode, relation.getClass());
		}
	}

	@Override
	public Iterable<Relationship> getRawSource(final SecurityContext securityContext, final Node dbNode) {
		return getMultiple(securityContext, dbNode, relation, Direction.OUTGOING, relation.getTargetType());
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode) {
		return getRawSource(securityContext, dbNode).iterator().hasNext();
	}
}
