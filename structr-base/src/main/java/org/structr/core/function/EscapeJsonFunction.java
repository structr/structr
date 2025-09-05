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

import org.apache.commons.lang3.StringEscapeUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class EscapeJsonFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_ESCAPE_JSON = "Usage: ${escape_json(string)}. Example: ${escape_json(this.name)}";
	public static final String ERROR_MESSAGE_ESCAPE_JSON_JS = "Usage: ${{ Structr.escape_json(string) }}. Example: ${{ Structr.escape_json(this.name); }}";

	@Override
	public String getName() {
		return "escape_json";
	}

	@Override
	public String getSignature() {
		return "string";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			return StringEscapeUtils.escapeJson(sources[0].toString());

		} catch (ArgumentNullException ane) {

			// silently ignore null strings
			return null;

		} catch (ArgumentCountException ace) {

			logParameterError(caller, sources, ace.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ESCAPE_JSON_JS : ERROR_MESSAGE_ESCAPE_JSON);
	}

	@Override
	public String shortDescription() {
		return "Escapes the given string for use within JSON";
	}
}
