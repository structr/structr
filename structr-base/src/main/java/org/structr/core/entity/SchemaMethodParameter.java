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

import org.structr.core.entity.relationship.SchemaMethodParameters;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;

/**
 * The typed parameter of a schema method.
 */
public class SchemaMethodParameter extends SchemaReloadingNode {

	public static final Property<SchemaMethod> schemaMethod = new StartNode<>("schemaMethod", SchemaMethodParameters.class);
	public static final Property<String> parameterType      = new StringProperty("parameterType");
	public static final Property<Integer> index             = new IntProperty("index").defaultValue(0);
	public static final Property<String> description        = new StringProperty("description");
	public static final Property<String> exampleValue       = new StringProperty("exampleValue");

	public String getParameterType() {
		return getProperty(parameterType);
	}

	@Override
	public boolean reloadSchemaOnCreate() {

		final SchemaMethod method = getProperty(schemaMethod);
		if (method != null && method.isJava()) {

			return true;
		}

		// only documentation, no reload
		return false;
	}

	@Override
	public boolean reloadSchemaOnModify(final ModificationQueue modificationQueue) {

		final SchemaMethod method = getProperty(schemaMethod);
		if (method != null && method.isJava()) {

			return true;
		}

		// only documentation, no reload
		return false;
	}

	@Override
	public boolean reloadSchemaOnDelete() {

		final SchemaMethod method = getProperty(schemaMethod);
		if (method != null && method.isJava()) {

			return true;
		}

		// only documentation, no reload
		return false;
	}
}
