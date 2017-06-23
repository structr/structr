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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SliceFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SLICE = "Usage: ${slice(collection, start, end)}. Example: ${slice(this.children, 0, 10)}";

	@Override
	public String getName() {
		return "slice()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

			if (sources[0] instanceof Collection || isArray(sources[0])) {

				final int start = Double.valueOf(sources[1].toString()).intValue();
				final int end   = Double.valueOf(sources[2].toString()).intValue();
				List list       = null;

				// handle list argument
				if (sources[0] instanceof List) {

					list = (List)sources[0];

				// handle array argument
				} else if (isArray(sources[0])) {

					list = toList((Object[])sources[0]);

				// handle collection argument
				} else {

					list = new LinkedList((Collection)sources[0]);
				}

				boolean valid   = true;

				if (start < 0)             { valid = false; logger.warn("Error in slice(): start index must be > 0."); }
				if (start > list.size())   { valid = false; logger.warn("Error in slice(): start index is out of range."); }
				if (end < 0)               { valid = false; logger.warn("Error in slice(): end index must be > 0."); }
				if (end > list.size() + 1) { valid = false; logger.warn("Error in slice(): end index is out of range."); }
				if (start > end)           { valid = false; logger.warn("Error in slice(): start index must be <= end index."); }

				if (valid) {

					return list.subList(start, end);
				}

			} else {

				logger.warn("Error in slice(): first argument is not a collection: {}.", sources[0].getClass().getSimpleName());

			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return null;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SLICE;
	}

	@Override
	public String shortDescription() {
		return "Selects elements from the given collection,, starting at the given start argument, and ends at, but does not include, the given end argument.";
	}

}
