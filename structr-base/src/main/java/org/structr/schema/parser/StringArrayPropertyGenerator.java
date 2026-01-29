/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.Property;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class StringArrayPropertyGenerator extends PropertyGenerator<String[]> {

	public StringArrayPropertyGenerator(final ErrorBuffer errorBuffer, final String entity, final PropertyDefinition params) {
		super(errorBuffer, entity, params);
	}

	@Override
	public String getValueType() {
		return String[].class.getSimpleName();
	}

	@Override
	public Type getPropertyType() {
		return Type.StringArray;
	}

	@Override
	protected Property newInstance() throws FrameworkException {

		final String expression = source.getFormat();
		if ("[]".equals(expression)) {
			reportError(new InvalidPropertySchemaToken(StructrTraits.SCHEMA_NODE, source.getPropertyName(), expression, "invalid_validation_expression", "Empty validation expression."));
			return null;
		}

		return new ArrayProperty(source.getPropertyName(), String.class);
	}

	@Override
	public String[] getDefaultValue() {
		return null;
	}
}
