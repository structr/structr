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

import org.structr.common.error.FrameworkException;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class OrFunction extends CoreFunction {

	@Override
	public String getName() {
		return "or";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("bool1, bool2, ...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		boolean result = false;

		if (sources != null) {

			if (sources.length < 2) {
				return usage(ctx.isJavaScriptContext());
			}

			for (Object i : sources) {

				if (i != null) {

					try {

						result |= "true".equals(i.toString()) || Boolean.TRUE.equals(i);

					} catch (Throwable t) {

						return t.getMessage();
					}

				} else {

					// null is false
					result |= false;
				}
			}
		}

		return result;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${or(b1, b2, ...)}. Example: ${or('true', 'true')}")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("expressions...", "list of expressions to evaluate")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the logical OR result of the given boolean expressions.";
	}

	@Override
	public String getLongDescription() {
		return "This function takes two or more arguments and ORs them together, returning `true` if any of the expressions evaluates to true, and `false` otherwise.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${or(true, false)}", "true && false = true")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (the || operator)."
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
