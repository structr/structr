package org.structr.core.entity.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;

/**
 *
 * @author Christian Morgner
 */
public class SchemaRelationshipSourceNode extends OneToMany<SchemaNode, SchemaRelationshipNode> {

	@Override
	public Class<SchemaNode> getSourceType() {
		return SchemaNode.class;
	}

	@Override
	public Class<SchemaRelationshipNode> getTargetType() {
		return SchemaRelationshipNode.class;
	}

	@Override
	public String name() {
		return "IS_RELATED_TO";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
