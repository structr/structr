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
package org.structr.common.error;

/**
 * Indicates that a property value was not in the expected set of values.
 *
 *
 */
public class ValueToken extends SemanticErrorToken {

	public ValueToken(final String type, final String propertyKey, final Object[] values) {

		super(type, propertyKey, "must_be_one_of");

		withDetail(ValueToken.getContent(values));
	}

	private static String getContent(final Object[] values) {

		final StringBuilder buf = new StringBuilder();
		final int len           = values.length;

		for (int i=0; i<len; i++) {

			if (values[i] != null) {
				buf.append(values[i].toString());
			}

			if (i < len-1) {
				buf.append(", ");
			}
		}

		return buf.toString();
	}
}
