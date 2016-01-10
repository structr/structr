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
public class SubtFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SUBT = "Usage: ${subt(value1, value2)}. Example: ${subt(5, 2)}";

	@Override
	public String getName() {
		return "subt()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			try {

				Double result = Double.parseDouble(sources[0].toString());

				for (int i = 1; i < sources.length; i++) {

					result -= Double.parseDouble(sources[i].toString());

				}

				return result;

			} catch (Throwable t) {

				return t.getMessage();

			}
		}

		return "";

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SUBT;
	}

	@Override
	public String shortDescription() {
		return "Substracts the second argument from the first argument";
	}

}
