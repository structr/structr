/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.LifecycleMethod;

public interface SchemaMethod extends NodeInterface {

	String schemaMethodNamePattern    = "[a-z_][a-zA-Z0-9_]*";

	AbstractSchemaNode getSchemaNode();
	Iterable<SchemaMethodParameter> getParameters();
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

	boolean callSuper();
	boolean overridesExisting();
	boolean doExport();
	boolean includeInOpenAPI();
	boolean isLifecycleMethod();
	boolean isJava();

	Class<LifecycleMethod> getMethodType();
	LifecycleMethod asLifecycleMethod();

	boolean isStaticMethod();
	boolean isPrivateMethod();
	boolean returnRawResult();

	String getHttpVerb();

	SchemaMethodParameter getSchemaMethodParameter(final String name);

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
