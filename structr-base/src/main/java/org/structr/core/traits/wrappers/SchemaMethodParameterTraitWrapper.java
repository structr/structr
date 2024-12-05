package org.structr.core.traits.wrappers;

import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;

public class SchemaMethodParameterTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements SchemaMethodParameter {

	public SchemaMethodParameterTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getParameterType() {
		return wrappedObject.getProperty(traits.key("parameterType"));
	}
}
