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

import java.util.Date;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.DateProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class DatePropertyParser extends PropertyParser {

	private String auxType = ", \"" + ISO8601DateProperty.PATTERN + "\"";

	public DatePropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String dbName, final String rawSource, final String defaultValue) {
		super(errorBuffer, className, propertyName, dbName, rawSource, defaultValue);
	}

	@Override
	public String getPropertyType() {
		return DateProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Date.class.getName();
	}

	@Override
	public String getPropertyParameters() {
		return auxType;
	}

	@Override
	public Type getKey() {
		return Type.Date;
	}

	@Override
	public void extractTypeValidation(final Schema entity, String expression) throws FrameworkException {

		if (expression.length() == 0) {
			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_date_pattern", "Empty date pattern."));
			return;
		}

		auxType = ", \"" + expression +"\"";
	}
}
