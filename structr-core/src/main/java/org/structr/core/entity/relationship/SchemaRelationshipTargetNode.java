package org.structr.core.entity.relationship;

import org.structr.core.entity.ManyToOne;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;

/**
 *
 * @author Christian Morgner
 */
public class SchemaRelationshipTargetNode extends ManyToOne<SchemaRelationshipNode, SchemaNode> {

	@Override
	public Class<SchemaRelationshipNode> getSourceType() {
		return SchemaRelationshipNode.class;
	}

	@Override
	public Class<SchemaNode> getTargetType() {
		return SchemaNode.class;
	}

	@Override
	public String name() {
		return "IS_RELATED_TO";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.TARGET_TO_SOURCE;
	}
}
