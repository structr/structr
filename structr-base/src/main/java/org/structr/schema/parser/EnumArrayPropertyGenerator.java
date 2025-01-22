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
import org.structr.core.property.EnumArrayProperty;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class EnumArrayPropertyGenerator extends PropertyGenerator<String[]> {

	public EnumArrayPropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getValueType() {
		return String.class.getName();
	}

	@Override
	public Type getPropertyType() {
		return Type.EnumArray;
	}

	@Override
	public Property newInstance() throws FrameworkException {

		final String expression = source.getFormat();
		final String name       = source.getPropertyName();

		if (StringUtils.isNotBlank(expression)) {

			return new EnumArrayProperty(name, EnumProperty.trimAndFilterEmptyStrings(expression.split("[, ]+")));

		} else if (source.getFqcn() != null) {

			// not supported!
			throw new FrameworkException(422, "Enum type definitions based on fully-qualified class names are not supported any more.");

		} else {

			reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), this.source.getPropertyName(), expression, "invalid_property_definition", "No enum types found, please specify a list of types, e.g. (red, green, blue)"));
		}

		return null;
	}

	@Override
	public String[] getDefaultValue() {

		final String val = source.getDefaultValue();

		if (StringUtils.isNotBlank(val)) {

			return val.split("[, ]+");
		}

		return null;
	}
}
