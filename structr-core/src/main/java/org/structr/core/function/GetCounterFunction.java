/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GetCounterFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GET_COUNTER = "Usage: ${get_counter(level)}. Example: ${get_counter(1)}";

	@Override
	public String getName() {
		return "get_counter()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (!arrayHasLengthAndAllElementsNotNull(sources, 1)) {
				
				return 0;
			}

			try {

				return ctx.getCounter(parseInt(sources[0]));

			} catch (NumberFormatException nfe) {

				logException(nfe, "{}: NumberFormatException parsing counter level \"{}\" in element \"{}\". Parameters: {}", new Object[] { getName(), sources[0].toString(), caller, getParametersAsString(sources) });

			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}

		return 0;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_GET_COUNTER;
	}

	@Override
	public String shortDescription() {
		return "Returns the value of the counter with the given index";
	}

}
