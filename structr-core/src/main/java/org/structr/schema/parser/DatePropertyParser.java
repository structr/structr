/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
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

	private String pattern = ISO8601DateProperty.PATTERN;
	private String auxType = ", \"" + pattern + "\"";

	public DatePropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final PropertyParameters params) {
		super(errorBuffer, className, propertyName, params);
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
	public void parseFormatString(final Schema entity, String expression) throws FrameworkException {

		if (expression != null) {

			if (expression.isEmpty()) {
				errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_date_pattern", "Empty date pattern."));
				return;
			}

			pattern = expression;
			auxType = ", \"" + expression +"\"";
		}
	}

	@Override
	public String getDefaultValueSource() {
		return "DatePropertyParser.parse(\"" + pattern + "\", \"" + defaultValue + "\")";
	}

	/**
	 * Static method to catch parse exception
	 *
	 * @param pattern
	 * @param value
	 * @return
	 */
	public static Date parse(final String pattern, final String value) {
		try {
			return new SimpleDateFormat(pattern).parse(value);
		} catch (ParseException ex) {
			Logger.getLogger(DatePropertyParser.class.getName()).log(Level.SEVERE, "Unable to parse " + value + " with pattern " + pattern, ex);
		}
		return null;
	}
}
