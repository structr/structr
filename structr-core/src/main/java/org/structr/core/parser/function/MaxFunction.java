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
public class MaxFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_MAX = "Usage: ${max(value1, value2)}. Example: ${max(this.children, 10)}";

	@Override
	public String getName() {
		return "max()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		Object result = "";
		String errorMsg = "ERROR! Usage: ${max(val1, val2)}. Example: ${max(5,10)}";

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			try {
				result = Math.max(Double.parseDouble(sources[0].toString()), Double.parseDouble(sources[1].toString()));

			} catch (Throwable t) {
				result = errorMsg;
			}

		} else {

			result = "";
		}

		return result;

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_MAX;
	}

	@Override
	public String shortDescription() {
		return "Returns the larger value of the given arguments";
	}

}
