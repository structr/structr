package org.structr.core.traits.wrappers;

import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;

public class SchemaMethodParameterTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements SchemaMethodParameter {

	public SchemaMethodParameterTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public NodeInterface getSchemaNode() {
		return wrappedObject.getProperty(traits.key("schemaNode"));
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key("description"));
	}

	@Override
	public String getParameterType() {
		return wrappedObject.getProperty(traits.key("parameterType"));
	}

	@Override
	public int getIndex() {
		return wrappedObject.getProperty(traits.key("index"));
	}

	@Override
	public String getExampleValue() {
		return wrappedObject.getProperty(traits.key("exampleValue"));
	}
}
