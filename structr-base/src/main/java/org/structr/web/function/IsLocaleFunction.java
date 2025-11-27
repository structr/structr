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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Locale;

public class IsLocaleFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "is_locale";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("locales...");
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${is_locale(locales...)}"),
			Usage.javaScript("Usage: ${{Structr.isLocale(locales...}}. Example ${{Structr.isLocale('de_DE', 'de_AT', 'de_CH')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns true if the current user locale is equal to the given argument.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("locales", "list of strings that represent different locales to check")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${is_locale('en_GB', 'en_US')}", "Check whether the current locale is an English variant")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"See the `locale` keyword to learn how the locale of the current context is determined."
		);
	}
}
