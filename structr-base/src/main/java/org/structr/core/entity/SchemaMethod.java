/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractSchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;

public interface SchemaMethod extends NodeInterface {

	String schemaMethodNamePattern    = "[a-z_][a-zA-Z0-9_]*";

	AbstractSchemaNode getSchemaNode();
	Iterable<SchemaMethodParameter> getParameters();
	String getStaticSchemaNodeName();
	String getName();
	String getSource();
	String getSummary();
	String getDescription();
	String getCodeType();
	String getReturnType();
	String getOpenAPIReturnType();
	String getVirtualFileName();
	String getSignature();

	String[] getExceptions();
	String[] getTags();

	void setSource(final String source) throws FrameworkException;

	boolean callSuper();
	boolean overridesExisting();
	boolean doExport();
	boolean includeInOpenAPI();
	boolean isLifecycleMethod();
	boolean isJava();
	boolean wrapJsInMain();

	Class<LifecycleMethod> getMethodType();
	LifecycleMethod asLifecycleMethod();

	boolean isStaticMethod();
	boolean isPrivateMethod();
	boolean returnRawResult();

	String getHttpVerb();

	SchemaMethodParameter getSchemaMethodParameter(final String name);

	default void handleAutomaticCorrectionOfAttributes() throws FrameworkException {

		final Traits schemaMethodTraits     = Traits.of(StructrTraits.SCHEMA_METHOD);
		final NodeInterface schemaNode      = getProperty(schemaMethodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY));
		final boolean isTypeMethod          = (schemaNode != null) || StringUtils.isNotBlank(getProperty(schemaMethodTraits.key(SchemaMethodTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY)));
		final boolean isServiceClassMethod  = (schemaNode != null) && Boolean.TRUE.equals(schemaNode.getProperty(Traits.of(StructrTraits.SCHEMA_NODE).key(AbstractSchemaNodeTraitDefinition.IS_SERVICE_CLASS_PROPERTY)));

		// - lifecycle methods can never be static
		// - lifecycle methods are never callable via REST
		if (isLifecycleMethod()) {
			setProperty(schemaMethodTraits.key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY), false);
			setProperty(schemaMethodTraits.key(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY), true);
		}

		// - service class methods must be static
		// - user-defined functions must also be static
		if (!isTypeMethod || isServiceClassMethod) {
			setProperty(schemaMethodTraits.key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY), true);
		}

		// a method which is not callable via REST should not be present in OpenAPI
		if (isPrivateMethod() == true) {
			setProperty(schemaMethodTraits.key(SchemaMethodTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY), false);
		}
	}

	default void warnAboutShadowingBuiltinFunction() {

		final String name = getName();

		TransactionCommand.simpleBroadcastWarning("User-defined function shadows built-in function", """
			The user-defined function "%s" is shadowing a built-in function (in non-StructrScript contexts).
			You can still use the built-in function in such contexts. In JavaScript, for example via "$._functions.%s"
		""".formatted(name, name), null);
	}
}
