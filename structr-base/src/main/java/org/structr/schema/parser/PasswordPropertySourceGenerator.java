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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PasswordProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper.Type;

import java.util.Map;

/**
 *
 *
 */
public class PasswordPropertySourceGenerator extends PropertySourceGenerator {

	public PasswordPropertySourceGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getPropertyType() {
		return PasswordProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return String.class.getName();
	}

	@Override
	public String getUnqualifiedValueType() {
		return "String";
	}

	@Override
	public String getPropertyParameters() {
		return "";
	}

	@Override
	public Type getKey() {
		return Type.String;
	}

	@Override
	public void parseFormatString(final Map<String, NodeInterface> schemaNodes, final NodeInterface entity, String expression) throws FrameworkException {

		if ("[]".equals(expression)) {
			reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), source.getPropertyName(), expression, "invalid_validation_expression", "Empty validation expression."));
			return;
		}

		if (StringUtils.isNotBlank(expression) && !("multi-line".equals(expression))) {

			addGlobalValidator(new Validator("isValidStringMatchingRegex", className, source.getPropertyName(), expression));
		}
	}

	@Override
	public String getDefaultValue() {
		return "\"".concat(getSourceDefaultValue()).concat("\"");
	}
}
