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
package org.structr.core.function;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ParseDateFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_PARSE_DATE    = "Usage: ${parse_date(value, pattern)}. Example: ${parse_date(\"2014-01-01\", \"yyyy-MM-dd\")}";
	public static final String ERROR_MESSAGE_PARSE_DATE_JS = "Usage: ${{Structr.parseDate(value, pattern)}}. Example: ${{Structr.parseDate(\"2014-01-01\", \"yyyy-MM-dd\")}}";

	@Override
	public String getName() {
		return "parse_date";
	}

	@Override
	public String getSignature() {
		return "str, pattern";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length != 2) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final String dateString = sources[0].toString();

			if (StringUtils.isBlank(dateString)) {
				return "";
			}

			final String pattern = sources[1].toString();

			try {
				// parse with format from IS
				return new SimpleDateFormat(pattern).parse(dateString);

			} catch (ParseException ex) {

				logger.debug("{}: Could not parse string \"{}\" with pattern {} in element \"{}\". Parameters: {}", new Object[] { getReplacement(), dateString, pattern, caller, getParametersAsString(sources) });

			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_PARSE_DATE_JS : ERROR_MESSAGE_PARSE_DATE);
	}

	@Override
	public String shortDescription() {
		return "Parses the given date string using the given format string";
	}
}
