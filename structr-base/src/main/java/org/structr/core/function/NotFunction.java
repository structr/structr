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
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class NotFunction extends CoreFunction {


	@Override
	public String getName() {
		return "not";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("bool");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			return !("true".equals(sources[0].toString()) || Boolean.TRUE.equals(sources[0]));

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return true;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${not(bool)}. Example: ${not(me.isAdmin)}")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("expression", "boolean expression to negate")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the logical negation given boolean expression.";
	}

	@Override
	public String getLongDescription() {
		return "This function takes a single arguments and returns the negation of its boolean value.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${not(true)}", "Return false")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (the ! operator)."
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
