/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import java.util.Date;

public class ToDateFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_TO_DATE    = "Usage: ${to_date(value)}. Example: ${to_date(1473201885000)}";
	public static final String ERROR_MESSAGE_TO_DATE_JS = "Usage: ${{Structr.toDate(value)}}. Example: ${{Structr.toDate(1473201885000)}}";

	@Override
	public String getName() {
		return "to_date";
	}

	@Override
	public String getSignature() {
		return "number";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		// if source is a number, try to convert from millis
		if (sources != null && sources.length == 1 && sources[0] != null && sources[0] instanceof Number) {

			try {
				Long timestamp = 0L;

				if (sources[0] instanceof Double) {

					timestamp = Math.round((Double) sources[0]);

				} else if (sources[0] instanceof Integer || sources[0] instanceof Long) {

					timestamp = (Long) sources[0];

				} else {
					throw new UnsupportedOperationException();
				}

				return new Date(timestamp);

			} catch (Throwable t) {
				// fail silently
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_TO_DATE_JS : ERROR_MESSAGE_TO_DATE);
	}

	@Override
	public String shortDescription() {
		return "Converts the given number to a date";
	}

}
