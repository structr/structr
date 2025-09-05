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
package org.structr.web.function;

import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.util.Locale;

public class IsLocaleFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_IS_LOCALE    = "Usage: ${is_locale(locales...)}";
	public static final String ERROR_MESSAGE_IS_LOCALE_JS = "Usage: ${{Structr.isLocale(locales...}}. Example ${{Structr.isLocale('de_DE', 'de_AT', 'de_CH')}}";

	@Override
	public String getName() {
		return "is_locale";
	}

	@Override
	public String getSignature() {
		return "locales...";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final Locale locale = ctx.getLocale();
		if (locale != null) {

			final String localeString = locale.toLanguageTag();

			if (sources != null && sources.length > 0) {
				final int len = sources.length;
				for (int i = 0; i < len; i++) {

					if (localeString.equals(sources[0].toString().replaceAll("_", "-"))) {
						return true;
					}
				}

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());
			}
		}

		return false;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_IS_LOCALE_JS : ERROR_MESSAGE_IS_LOCALE);
	}

	@Override
	public String shortDescription() {
		return "Returns true if the current user locale is equal to the given argument";
	}
}
