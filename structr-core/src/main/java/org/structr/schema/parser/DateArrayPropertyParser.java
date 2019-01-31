/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.Date;
import java.util.Map;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.ArrayProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class DateArrayPropertyParser extends PropertySourceGenerator {

	public DateArrayPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getPropertyType() {
		return ArrayProperty.class.getSimpleName().concat("<Date>");
	}

	@Override
	public String getValueType() {
		return Date[].class.getSimpleName();
	}

	@Override
	public String getUnqualifiedValueType() {
		return "Date[]";
	}

	@Override
	public String getPropertyParameters() {
		return ", Date.class";
	}

	@Override
	public Type getKey() {
		return Type.DateArray;
	}

	@Override
	public void parseFormatString(final Map<String, SchemaNode> schemaNodes, final Schema entity, final String expression) throws FrameworkException {

		if ("[]".equals(expression)) {
			reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), source.getPropertyName(), expression, "invalid_validation_expression", "Empty validation expression."));
			return;
		}
	}

	@Override
	public String getDefaultValue() {
		return "\"".concat(getSourceDefaultValue()).concat("\"");
	}
}
