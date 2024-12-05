package org.structr.core.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.schema.action.ActionEntry;

import java.util.Map;

public class SchemaMethodTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements SchemaMethod {

	public SchemaMethodTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Iterable<SchemaMethodParameter> getParameters() {
		return null;
	}

	@Override
	public ActionEntry getActionEntry(Map<String, SchemaNode> schemaNodes, AbstractSchemaNode schemaEntity) throws FrameworkException {
		return null;
	}

	@Override
	public boolean isStaticMethod() {
		return false;
	}

	@Override
	public boolean isPrivateMethod() {
		return false;
	}

	@Override
	public boolean returnRawResult() {
		return false;
	}

	@Override
	public SchemaMethodTraitDefinition.HttpVerb getHttpVerb() {
		return null;
	}

	@Override
	public boolean isJava() {
		return false;
	}

	@Override
	public boolean isLifecycleMethod() {
		return false;
	}
}
