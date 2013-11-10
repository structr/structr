package org.structr.core.graph;

import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */


public interface ModificationEvent {

	public boolean isNode();
	
	public boolean isDeleted();
	public boolean isModified();
	public boolean isCreated();
	
	public GraphObject getGraphObject();
	public RelationshipType getRelationshipType();
	public String getUuid();

	public Set<PropertyKey> getModifiedProperties();
	public Set<PropertyKey> getRemovedProperties();
	
	public Map<String, Object> getData(final SecurityContext securityContext) throws FrameworkException;
}
