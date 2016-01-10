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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class AbbrFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ABBR = "Usage: ${abbr(longString, maxLength)}. Example: ${abbr(this.title, 20)}";

	@Override
	public String getName() {
		return "abbr()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			try {
				int maxLength = Double.valueOf(sources[1].toString()).intValue();

				if (sources[0].toString().length() > maxLength) {

					return StringUtils.substringBeforeLast(StringUtils.substring(sources[0].toString(), 0, maxLength), " ").concat("…");

				} else {

					return sources[0];
				}

			} catch (NumberFormatException nfe) {

				return nfe.getMessage();

			}

		}

		return "";

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ABBR;
	}

	@Override
	public String shortDescription() {
		return "Abbreviates the given string";
	}

}
