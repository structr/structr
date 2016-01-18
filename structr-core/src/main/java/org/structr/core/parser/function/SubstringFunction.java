/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SubstringFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SUBSTRING = "Usage: ${substring(string, start, length)}. Example: ${substring(this.name, 19, 3)}";

	@Override
	public String getName() {
		return "substring()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			final String source = sources[0].toString();
			final int sourceLength = source.length();
			final int start = parseInt(sources[1]);
			final int length = sources.length >= 3 ? parseInt(sources[2]) : sourceLength - start;
			final int end = start + length;

			if (start >= 0 && start < sourceLength && end >= 0 && end <= sourceLength && start <= end) {

				return source.substring(start, end);
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SUBSTRING;
	}

	@Override
	public String shortDescription() {
		return "Returns the substring of the given string";
	}

}
