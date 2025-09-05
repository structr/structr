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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class ParseNumberFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_PARSE_NUMBER    = "Usage: ${parse_number(value, locale)}. Example: ${parse_number('12345.6789', 'en')}";
	public static final String ERROR_MESSAGE_PARSE_NUMBER_JS = "Usage: ${{Structr.parseNumber(value, locale)}}. Example: ${{Structr.parseNumber('12345.6789', 'en')}}";

	@Override
	public String getName() {
		return "parse_number";
	}

	@Override
	public String getSignature() {
		return "number [, locale ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || (sources.length == 2 && sources[1] == null) || sources.length > 2) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		try {

			final String numberString = sources[0].toString().replaceAll("[^\\d.,-]", "");

			if (StringUtils.isBlank(numberString)) {
				return "";
			}

			Locale locale = ctx.getLocale();
			try {

				if (sources.length == 2) {

					final String localeString = sources[1].toString();

					if (StringUtils.isNotBlank(localeString)) {

						locale = Locale.forLanguageTag(localeString);
					}
				}

				return NumberFormat.getInstance(locale).parse(numberString);

			} catch (ParseException ex) {

				logException(ex, "{}: Could not parse string \"{}\" to number. Parameters: {}", new Object[] { getReplacement(), caller, getParametersAsString(sources) });
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_PARSE_NUMBER_JS : ERROR_MESSAGE_PARSE_NUMBER);
	}

	@Override
	public String shortDescription() {
		return "Parses the given string using the given (optional) locale";
	}
}
