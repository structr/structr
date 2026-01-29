/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class AbbrFunction extends CoreFunction {

	@Override
	public String getName() {
		return "abbr";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources == null || sources.length < 2 || sources[1] == null) {

				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return usage(ctx.isJavaScriptContext());
			}

			if (sources[0] == null) {

				return "";
			}

			final String abbreviationText = ((sources.length == 3 && sources[2] != null) ? sources[2].toString() : "…");

			int maxLength = Double.valueOf(sources[1].toString()).intValue();

			if (sources[0].toString().length() > maxLength) {

				return StringUtils.substringBeforeLast(StringUtils.substring(sources[0].toString(), 0, maxLength), " ").concat(abbreviationText);

			} else {

				return sources[0];
			}

		} catch (final NumberFormatException nfe) {

			logException(nfe, "{}: NumberFormatException in \"{}\". Can not parse \"{}\" as Integer. Returning original string. Parameters: {}", new Object[] { getDisplayName(), caller, sources[1], getParametersAsString(sources) });

			return sources[0];

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("string, maxLength[, abbr = '…']");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("string", "string to abbreviate"),
			Parameter.optional("maxLength", "maximum length of the returned string (including the ellipsis at the end)"),
			Parameter.optional("abbr", "last character(s) of the returned string after abbreviation")
		);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${abbr(string, maxLength[, abbr = '…'])}. Example: ${abbr(this.title, 20)}"),
			Usage.javaScript("Usage: ${{Structr.abbr(string, maxLength[, abbr = '…'])}}. Example: ${{Structr.abbr(this.title, 20)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Abbreviates the given string at the last space character before the maximum length is reached.";
	}

	@Override
	public String getLongDescription() {
		return "The remaining characters are replaced with the ellipsis character (…) or the given `abbr` parameter.";
	}
}
