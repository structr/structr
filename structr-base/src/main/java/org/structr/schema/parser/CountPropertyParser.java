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
package org.structr.schema.parser;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.ElementCounter;
import org.structr.schema.SchemaHelper.Type;

import java.util.Map;

/**
 *
 *
 */
public class CountPropertyParser extends PropertySourceGenerator {

	private String auxType = "";

	public CountPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getPropertyType() {
		return ElementCounter.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Integer.class.getName();
	}

	@Override
	public String getUnqualifiedValueType() {
		return "Integer";
	}

	@Override
	public String getPropertyParameters() {
		return auxType;
	}

	@Override
	public Type getKey() {
		return Type.Count;
	}

	@Override
	public void parseFormatString(final Map<String, NodeInterface> schemaNodes, final NodeInterface entity, String expression) throws FrameworkException {

		if (expression == null || expression.isEmpty()) {
			throw new FrameworkException(422, "Invalid count property expression for property ‛" + source.getPropertyName() + "‛", new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), source.getPropertyName(), expression, "invalid_property_reference", "Empty property reference."));
		}

		auxType = ", " + expression + "Property";
	}
}
