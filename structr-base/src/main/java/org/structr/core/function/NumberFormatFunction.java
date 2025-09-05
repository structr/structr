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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberFormatFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_NUMBER_FORMAT    = "Usage: ${number_format(value, locale, pattern)}. Example: ${number_format(12345.6789, 'en', '#,##0.00')}";
	public static final String ERROR_MESSAGE_NUMBER_FORMAT_JS = "Usage: ${{Structr.numberFormat(value, locale, pattern)}}. Example: ${{Structr.numberFormat(12345.6789, 'en', '#,##0.00')}}";

	@Override
	public String getName() {
		return "number_format";
	}

	@Override
	public String getSignature() {
		return "value, locale, format";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length != 3 || sources[1] == null || sources[2] == null) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			final Double val      = Double.parseDouble(sources[0].toString());
			final String langCode = sources[1].toString();
			final String pattern  = sources[2].toString();

			return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.forLanguageTag(langCode.replaceAll("_", "-")))).format(val);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} catch (Throwable t) {

			logException(caller, t, sources);
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return "";
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_NUMBER_FORMAT_JS : ERROR_MESSAGE_NUMBER_FORMAT);
	}

	@Override
	public String shortDescription() {
		return "Formats the given value using the given locale and format string";
	}
}
