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
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class AbbrFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_ABBR    = "Usage: ${abbr(longString, maxLength[, abbr = '…'])}. Example: ${abbr(this.title, 20)}";
	public static final String ERROR_MESSAGE_ABBR_JS = "Usage: ${{Structr.abbr(longString, maxLength[, abbr = '…'])}}. Example: ${{Structr.abbr(this.title, 20)}}";

	@Override
	public String getName() {
		return "abbr";
	}

	@Override
	public String getSignature() {
		return "str, maxLength[, abbr = '…']";
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

			logException(nfe, "{}: NumberFormatException in \"{}\". Can not parse \"{}\" as Integer. Returning original string. Parameters: {}", new Object[] { getReplacement(), caller, sources[1], getParametersAsString(sources) });

			return sources[0];

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ABBR_JS : ERROR_MESSAGE_ABBR);
	}

	@Override
	public String shortDescription() {
		return "Abbreviates the given string to the given length and appends the abbreviation (default = '…')";
	}
}
