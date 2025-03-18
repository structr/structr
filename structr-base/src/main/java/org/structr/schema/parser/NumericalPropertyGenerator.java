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
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;

/**
 *
 *
 */
public abstract class NumericalPropertyGenerator<T> extends PropertyGenerator<T> {

	private Number lowerBound      = null;
	private Number upperBound      = null;
	private boolean lowerExclusive = false;
	private boolean upperExclusive = false;
	protected boolean error        = false;

	public NumericalPropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {

		super(errorBuffer, className, params);

		initialize();
	}

	public abstract Number parseNumber(final ErrorBuffer errorBuffer, final String propertyName, final String source, final String which);

	private void initialize() {

		final String rangeFormatErrorMessage = "Range expression must describe a (possibly open-ended) interval, e.g. [10,99] or ]9,100[ for all two-digit integers";
		final String name                    = source.getPropertyName();
		final String expression              = source.getFormat();

		if (StringUtils.isNotBlank(expression)) {

			if ((expression.startsWith("[") || expression.startsWith("]")) && (expression.endsWith("[") || expression.endsWith("]"))) {

				final String range = expression.substring(1, expression.length() - 1);
				final String[] parts = range.split(",+");

				if (parts.length == 2) {

					lowerBound = parseNumber(getErrorBuffer(), name, parts[0].trim(), "lower");
					upperBound = parseNumber(getErrorBuffer(), name, parts[1].trim(), "upper");

					if (lowerBound == null || upperBound == null) {
						error = true;
					}

					lowerExclusive = expression.startsWith("]");
					upperExclusive = expression.endsWith("[");

				} else {

					error = true;
				}

			} else {

				error = true;
			}
		}

		if (error) {

			reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), source.getPropertyName(), expression, "invalid_range_expression", rangeFormatErrorMessage));
		}
	}

	public Number getLowerBound() {
		return lowerBound;
	}

	public Number getUpperBound() {
		return upperBound;
	}

	public boolean isLowerExclusive() {
		return lowerExclusive;
	}

	public boolean isUpperExclusive() {
		return upperExclusive;
	}
}
