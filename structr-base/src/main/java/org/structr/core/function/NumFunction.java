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

public class NumFunction extends CoreFunction {

	@Override
	public String getName() {
		return "num";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("object");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			try {

				return getDoubleOrNull(sources[0]);

			} catch (Throwable t) {

				logException(caller, t, sources);
				return null;
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${num(object)}. Example: ${num(this.numericalStringValue)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Tries the convert given object into a floating-point number with double precision.";
	}

	@Override
	public String getLongDescription() {
		return """
		Date values are also supported and are converted to the number of milliseconds since January 1, 1970, 00:00:00 GMT.
		
		Other date strings are also supported in the following formats:
		- "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
		- "yyyy-MM-dd'T'HH:mm:ssXXX"
		- "yyyy-MM-dd'T'HH:mm:ssZ"
		- "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("object", "input object to convert to a long integer, can be string, date or floating-point number")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${num(now)}", "Return the milliseconds since epoch of the current date"),
			Example.structrScript("${num('35.8')}", "Convert a string into a floating-point number")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"See also `long()`."
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
