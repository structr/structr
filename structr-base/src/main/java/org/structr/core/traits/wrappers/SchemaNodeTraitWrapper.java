package org.structr.core.traits.wrappers;

import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;

public class SchemaNodeTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements SchemaNode {

	public SchemaNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Iterable<NodeInterface> getSchemaGrants() {
		return wrappedObject.getProperty(traits.key("schemaGrants"));
	}
}
