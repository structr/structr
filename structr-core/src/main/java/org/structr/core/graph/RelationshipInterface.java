package org.structr.core.graph;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;

/**
 *
 * @author Christian Morgner
 */
public interface RelationshipInterface extends GraphObject {
	
	public void init(final SecurityContext securityContext, final Relationship dbRel);
	
	public void onRelationshipInstantiation();

	public NodeInterface getSourceNode();
	public NodeInterface getTargetNode();
	public NodeInterface getOtherNode(final NodeInterface thisNode);
	public RelationshipType getRelType();
	
	public Relationship getRelationship();

	public PropertyMap getProperties() throws FrameworkException;

	public String getSourceNodeId();
	public void setSourceNodeId(final String startNodeId) throws FrameworkException;

	public String getTargetNodeId();
	public void setTargetNodeId(final String targetIdNode) throws FrameworkException;

	
	public int cascadeDelete();
}
