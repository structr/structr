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

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;


public class ArgumentTypeException extends IllegalArgumentException {

	public ArgumentTypeException(final String message) {
		super(message);
	}

	public static ArgumentTypeException wrongTypes(final Object[] params, final int minimum, final Class... types) {

		final StringBuilder buf = new StringBuilder("Expected at least (");
		buf.append(join(types, minimum, Class::getSimpleName));
		buf.append("), got (");
		buf.append(join(params, 1000, Object::toString));
		buf.append(")");

		return new ArgumentTypeException(buf.toString());
	}


	private static <T> String join(final T[] objects, final int minimum, Function<T, String> mappingFunction) {

		final List<String> mapped = new LinkedList<>();
		int count                 = 0;

		for (final T o : objects) {

			if (o != null) {

				mapped.add(mappingFunction.apply(o));

			} else {

				mapped.add("null");
			}

			if (count++ > minimum) {
				break;
			}
		}

		return StringUtils.join(mapped, ", ");
	}
}
