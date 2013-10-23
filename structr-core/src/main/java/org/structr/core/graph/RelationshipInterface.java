package org.structr.core.graph;

import org.neo4j.graphdb.Direction;
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
	
	public Relationship getRelationship();

	public Class<? extends NodeInterface> getSourceType();
	public RelationshipType getRelationshipType();
	public Class<? extends NodeInterface> getDestinationType();

	public Direction getDirection();
	public Direction getDirectionForType(final Class<? extends NodeInterface> type);
	
	public <T extends NodeInterface> T getStartNode();
	public <T extends NodeInterface> T getEndNode();
	
	public PropertyMap getProperties() throws FrameworkException;
	
	public int cascadeDelete();
}
