package org.structr.core.entity.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaNode;

/**
 *
 * @author Axel Morgner
 */
public class TargetSchemaNodeResourceAccess extends OneToMany<SchemaNode, ResourceAccess> {

	@Override
	public Class<SchemaNode> getSourceType() {
		return SchemaNode.class;
	}

	@Override
	public String name() {
		return "TARGET_NODE_RESOURCE_ACCESS";
	}

	@Override
	public Class<ResourceAccess> getTargetType() {
		return ResourceAccess.class;
	}
}
