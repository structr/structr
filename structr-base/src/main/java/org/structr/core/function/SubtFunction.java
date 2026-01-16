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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class SubtFunction extends CoreFunction {

	@Override
	public String getName() {
		return "subt";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value1, value2");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			try {

				double result = Double.parseDouble(sources[0].toString());

				for (int i = 1; i < sources.length; i++) {

					result -= Double.parseDouble(sources[i].toString());
				}

				return result;

			} catch (Throwable t) {

				logException(caller, t, sources);
				return t.getMessage();
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${subt(value1, value2)}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Subtracts the second argument from the first argument.";
	}

	@Override
	public String getLongDescription() {
		return "This function tries to convert its parameter objects into numerical values, i.e. you can use strings as arguments.";
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${subt(5, 2)}"),
				Example.structrScript("${subt('5', '2')}"),
				Example.javaScript("${{ $.subt(5, 2)  }}"),
				Example.javaScript("${{ $.subt('5', '2')  }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("val1", "minuend"),
				Parameter.mandatory("val2", "subtrahend")
				);
	}
}
