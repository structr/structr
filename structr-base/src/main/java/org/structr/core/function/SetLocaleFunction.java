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

import java.util.Locale;

public class SetLocaleFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_SET_LOCALE    = "Usage: ${set_locale(locale)}. Example: ${set_locale('de_DE')}";
	public static final String ERROR_MESSAGE_SET_LOCALE_JS = "Usage: ${{Structr.setLocale(locale)}}. Example: ${{Structr.setLocale('de_DE');}}";

	@Override
	public String getName() {
		return "set_locale";
	}

	@Override
	public String getSignature() {
		return "locale";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length != 1 || sources[0] == null) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			ctx.setLocale(Locale.forLanguageTag(sources[0].toString().replaceAll("_", "-")));

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SET_LOCALE_JS : ERROR_MESSAGE_SET_LOCALE);
	}

	@Override
	public String shortDescription() {
		return "Sets the locale in the current context to the given value.";
	}
}
