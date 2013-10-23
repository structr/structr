package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.PropertyAccess;
import org.structr.core.entity.ResourceAccess;

/**
 *
 * @author Christian Morgner
 */
public class Access extends AbstractRelationship<ResourceAccess, PropertyAccess> {

	@Override
	public Class<ResourceAccess> getSourceType() {
		return ResourceAccess.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.PROPERTY_ACCESS;
	}

	@Override
	public Class<PropertyAccess> getDestinationType() {
		return PropertyAccess.class;
	}

}
