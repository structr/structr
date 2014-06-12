/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema.parser;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.EnumProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class EnumPropertyParser extends PropertyParser {

	private String enumTypeName = "";
	private String enumType     = "";

	public EnumPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String dbName, final String rawSource, final String defaultValue) {
		super(errorBuffer, className, propertyName, dbName, rawSource, defaultValue);
	}

	@Override
	public String getPropertyType() {
		return EnumProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return enumTypeName;
	}

	@Override
	public String getPropertyParameters() {
		return enumType;
	}

	@Override
	public Type getKey() {
		return Type.Enum;
	}

	@Override
	public void extractTypeValidation(final Schema entity, String expression) throws FrameworkException {

		final String[] enumTypes = expression.split("[, ]+");
		if (StringUtils.isNotBlank(expression) && enumTypes.length > 0) {

			enumTypeName = StringUtils.capitalize(propertyName);

			// create enum type
			enumType = ", " + enumTypeName + ".class";

			// build enum type definition

			final StringBuilder buf = new StringBuilder();

			buf.append("\n\tpublic enum ").append(enumTypeName).append(" {\n\t\t");
			for (int i=0; i<enumTypes.length; i++) {

				final String element = enumTypes[i];

				if (element.matches("[a-zA-Z_]+")) {

					buf.append(element);

					// comma separation
					if (i < enumTypes.length-1) {
						buf.append(", ");
					}

				} else {

					errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_property_definition", "Invalid enum type name, must match [a-zA-Z_]+."));

				}
			}
			buf.append("\n\t};");

			enumDefinitions.add(buf.toString());

		} else {

			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_property_definition", "No enum types found, please specify a list of types, e.g. (red, green, blue)"));
		}
	}

	@Override
	public String getDefaultValueSource() {
		return enumTypeName.concat(".").concat(defaultValue);
	}
}
