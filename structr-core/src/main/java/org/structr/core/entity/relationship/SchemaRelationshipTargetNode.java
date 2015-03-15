package org.structr.core.entity.relationship;

import org.structr.core.entity.OneToOne;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;

/**
 *
 * @author Christian Morgner
 */
public class SchemaRelationshipTargetNode extends OneToOne<SchemaRelationshipNode, SchemaNode> {

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
}
