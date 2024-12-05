package org.structr.core.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.NodeTrait;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.schema.action.ActionEntry;

import java.util.Map;

public interface SchemaMethod extends NodeTrait {

	Iterable<NodeInterface> getParameters();

	ActionEntry getActionEntry(Map<String, SchemaNode> schemaNodes, AbstractSchemaNode schemaEntity) throws FrameworkException;

	boolean isStaticMethod();
	boolean isPrivateMethod();
	boolean returnRawResult();

	SchemaMethodTraitDefinition.HttpVerb getHttpVerb();

	boolean isJava();
	boolean isLifecycleMethod();

	/*
	default void handleAutomaticCorrectionOfAttributes(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		final boolean isLifeCycleMethod = isLifecycleMethod();
		final boolean isTypeMethod = (getProperty(SchemaMethod.schemaNode) != null || StringUtils.isNotBlank(getProperty(SchemaMethodTraitDefinition.staticSchemaNodeName)));

		// - lifecycle methods can never be static
		// - user-defined functions can also not be static (? or should always be static?)
		if (!isTypeMethod || isLifeCycleMethod) {
			setProperty(SchemaMethod.isStatic, false);
		}

		// lifecycle methods are NEVER callable via REST
		if (isLifeCycleMethod) {
			setProperty(SchemaMethod.isPrivate, true);
		}

		// a method which is not callable via REST should not be present in OpenAPI
		if (getProperty(SchemaMethod.isPrivate) == true) {
			setProperty(SchemaMethod.includeInOpenAPI, false);
		}
	}
	*/

}
