package org.structr.common;

import org.structr.core.entity.SchemaRelationshipNode.Direction;
import org.structr.core.entity.SchemaRelationshipNode.Propagation;

/**
 * Marker interface to signal the configurable support of security
 * permission propagation through domain relationships.
 *
 * @author Christian Morgner
 */
public interface PermissionPropagation {

	public Direction getPropagationDirection();
	public Propagation getReadPropagation();
	public Propagation getWritePropagation();
	public Propagation getDeletePropagation();
	public Propagation getAccessControlPropagation();

	public String getDeltaProperties();
}
