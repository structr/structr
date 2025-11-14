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
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Locale;

public class SetLocaleFunction extends CoreFunction {

	@Override
	public String getName() {
		return "set_locale";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("locale");
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${set_locale(locale)}. Example: ${set_locale('de_DE')}"),
			Usage.javaScript("Usage: ${{ $.setLocale(locale); }}. Example: ${{ $.setLocale('de_DE'); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sets the locale for the current request.";
	}

	@Override
	public String getLongDescription() {
		return """
        This function gives granular control of the current locale and directly influences the result of date parsing and formatting functions as well as the results of calls to localize().

        For page rendering and REST requests, the builtin request parameter `_locale` can be used to set the locale for the whole request.
        """;
	}

	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${ (set_locale('de_DE'), date_format(now, 'E')) }", "Get name of current weekday in german.")
		);
	}
}
