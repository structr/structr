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

import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class ClearErrorFunction extends CoreFunction {

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length == 0) {

			return null;
		}

		if (sources.length == 1 && sources[0] instanceof ErrorToken errorToken) {

			ctx.getErrorBuffer().getErrorTokens().remove(errorToken);

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String getName() {
		return "clearError";
	}

	@Override
	public String getShortDescription() {
		return "Clears the given error token from the current context.";
	}

	@Override
	public String getLongDescription() {
		return "This function only supports error tokens returned by the `getErrors()` function as arguments.";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("errorToken");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{$.clearError(errorToken)}}. Example: ${{$.clearError(errorToken)}}"),
			Usage.structrScript("Usage: ${clearError(errorToken)}. Example: ${clearError(errorToken)}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("errorToken", "error token as returned by `getErrors()`")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"See also `getErrors()`, `clearErrors()`, `error()` and `assert()`."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Validation;
	}
}
