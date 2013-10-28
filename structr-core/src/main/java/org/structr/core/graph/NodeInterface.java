package org.structr.core.graph;

import org.neo4j.graphdb.Node;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.ManyEndpoint;
import org.structr.core.entity.ManyStartpoint;
import org.structr.core.entity.OneEndpoint;
import org.structr.core.entity.OneStartpoint;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Source;
import org.structr.core.entity.Target;

/**
 *
 * @author Christian Morgner
 */
public interface NodeInterface extends GraphObject, Comparable<NodeInterface>, AccessControllable {
	
	public void init(final SecurityContext securityContext, final Node dbNode);
	public void setSecurityContext(final SecurityContext securityContext);
	
	public void onNodeCreation();
	public void onNodeInstantiation();
	public void onNodeDeletion();
	
	public Node getNode();
	
	public String getName();
	
	public boolean isValid(ErrorBuffer errorBuffer);
	public boolean isDeleted();
	
	public <R extends AbstractRelationship> Iterable<R> getRelationships();
	public <R extends AbstractRelationship> Iterable<R> getIncomingRelationships();
	public <R extends AbstractRelationship> Iterable<R> getOutgoingRelationships();

	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(final Class<R> type);
	
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(final Class<R> type);
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(final Class<R> type);
	
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(final Class<R> type);
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(final Class<R> type);
}
