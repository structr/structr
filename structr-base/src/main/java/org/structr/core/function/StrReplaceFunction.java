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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.util.regex.PatternSyntaxException;

public class StrReplaceFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_STR_REPLACE = "Usage: ${str_replace(subject, search, replacement)}. Example: ${str_replace(\"Hello Wrlod!\", \"Wrlod\", \"World\")}";

	@Override
	public String getName() {
		return "str_replace";
	}

	@Override
	public String getSignature() {
		return "str, substring, replacement";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			return sources[0].toString().replaceAll(sources[1].toString(), sources[2].toString());

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		} catch (PatternSyntaxException ex) {

			logParameterError(caller, sources, "Error in RegEx: " + ex.getMessage(),ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_STR_REPLACE;
	}

	@Override
	public String shortDescription() {
		return "Replaces each substring of the subject that matches the given regular expression with the given replacement.";
	}
}
