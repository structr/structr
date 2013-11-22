package org.structr.core.graph;

import java.util.Map;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;

/**
 *
 * @author Christian Morgner
 */


public interface ModificationEvent {

	public boolean isNode();
	
	public int getStatus();
	
	public boolean isDeleted();
	public boolean isModified();
	public boolean isCreated();
	
	public GraphObject getGraphObject();
	public RelationshipType getRelationshipType();
	public String getUuid();

	public PropertyMap getModifiedProperties();
	public PropertyMap getRemovedProperties();
	
	public Map<String, Object> getData(final SecurityContext securityContext) throws FrameworkException;
}
