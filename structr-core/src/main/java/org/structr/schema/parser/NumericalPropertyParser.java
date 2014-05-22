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
import org.structr.schema.Schema;

/**
 *
 * @author Christian Morgner
 */
public abstract class NumericalPropertyParser extends PropertyParser {

	public NumericalPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String dbName, final String rawSource, final String defaultValue) {
		super(errorBuffer, className, propertyName, dbName, rawSource, defaultValue);
	}

	public abstract Number parseNumber(final ErrorBuffer errorBuffer, final String source, final String which);

	@Override
	public String getPropertyParameters() {
		return "";
	}

	@Override
	public void extractTypeValidation(final Schema entity, final String expression) throws FrameworkException {

		if (StringUtils.isNotBlank(expression)) {

			if (expression.contains(",")) {

				if ((expression.startsWith("[") || expression.startsWith("]")) && (expression.endsWith("[") || expression.endsWith("]"))) {

					final String range      = expression.substring(1, expression.length()-1);
					final String[] parts    = range.split("[, ]+");
					boolean error           = false;

					if (parts.length == 2) {

						Number lowerBound = parseNumber(errorBuffer, parts[0], "lower");
						Number upperBound = parseNumber(errorBuffer, parts[1], "upper");

						if (lowerBound == null) {
							error = true;
						}

						if (upperBound == null) {
							error = true;
						}

					} else {

						errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_range_expression", "Range must have exactly two bounds."));
						error = true;
					}

					if (error) {
						return;
					}

					final StringBuilder buf = new StringBuilder();

					buf.append("ValidationHelper.check").append(getValueType()).append("InRangeError(this, ");
					buf.append(className).append(".").append(propertyName).append("Property");
					buf.append(", \"").append(expression);
					buf.append("\", errorBuffer)");

					globalValidators.add(buf.toString());

				} else {

					errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_range_expression", "Range expression must start and end with [ or ], e.g. [" + expression + "]."));
				}

			} else {

				errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_range_expression", "Range expression must contain two values separated by a comma."));
			}
		}
	}
}
