package org.structr.core.graph;

import org.neo4j.graphdb.Node;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.core.Incoming;
import org.structr.core.Outgoing;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;

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
	
	public boolean isDeleted();
	
	public <T extends AbstractRelationship> Iterable<T> getRelationships(final Class<T> type);
	public <T extends AbstractRelationship> Iterable<T> getRelationships();
	
	public <T extends Incoming> Iterable<T> getReverseRelationships(final Class<T> type);
	public <T extends Outgoing & Incoming> Iterable<T> getAllRelationships(final Class<T> type);
	
	public <T extends Incoming> Iterable<T> getIncomingRelationships(final Class<T> type);
	public <T extends Incoming> Iterable<T> getIncomingRelationships();
	
	public <T extends Outgoing> Iterable<T> getOutgoingRelationships(final Class<T> type);
	public <T extends Outgoing> Iterable<T> getOutgoingRelationships();
}
