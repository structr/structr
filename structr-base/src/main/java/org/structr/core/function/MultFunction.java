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

import org.structr.common.error.FrameworkException;
import org.structr.docs.Language;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MultFunction extends CoreFunction {

	@Override
	public String getName() {
		return "mult";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("values...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		double result = 1.0d;

		if (sources != null) {

			for (Object i : sources) {

				// Multiply with null results in null
				if (i == null) {

					return null;
				}

				try {

					result *= Double.parseDouble(i.toString());

				} catch (Throwable t) {

					logException(caller, t, sources);

					return t.getMessage();
				}
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return result;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mult(value1, value2)}. Example: ${mult(5, 2)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the product of all given arguments.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("values...", "one or more values to multiply")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function tries to convert its parameter objects into numerical values, i.e. you can use strings as arguments."
		);
	}
}
