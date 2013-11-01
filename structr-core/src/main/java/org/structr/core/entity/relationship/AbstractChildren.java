package org.structr.core.entity.relationship;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.OneToMany;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractChildren<S extends LinkedTreeNode, T extends LinkedTreeNode> extends OneToMany<S, T> {

	public static final Property<Integer> position = new IntProperty("position").indexed();

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.CONTAINS;
	}
}
