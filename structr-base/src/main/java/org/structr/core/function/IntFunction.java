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


public class IntFunction extends CoreFunction {

	@Override
	public String getName() {
		return "int";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof Number) {
				return ((Number)sources[0]).intValue();
			}

			final Double dbl = getDoubleOrNull(sources[0]);

			if (dbl != null) {

				return dbl.intValue();

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return null;
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} catch (Throwable t) {

			logException(caller, t, sources);
			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${int(parameter)}. Example: ${int(this.numericalStringValue)} or ${int(5.8)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Tries to convert the given object into an integer value.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("input", "input value to convert to an integer, can be string or floating-point number")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${int(5.8)}", "Convert a floating-point value into an integer"),
			Example.structrScript("${int('35.8')}", "Convert a string into an integer")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
