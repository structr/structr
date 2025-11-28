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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class CeilFunction extends CoreFunction {

	@Override
	public String getName() {
		return "ceil";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			return (int)Math.ceil(Double.parseDouble(sources[0].toString()));

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
			Usage.javaScript("Usage: ${{$.ceil(value)}}. Example: ${{$.ceil(32.4)}}"),
			Usage.structrScript("Usage: ${ceil(value)}. Example: ${ceil(32.4)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the given value, rounded up to the nearest integer.";
	}

	@Override
	public String getLongDescription() {
		return "This method tries to convert its arguments into numerical values, i.e. you can use strings as arguments.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("value", "input value (can also be a string with a number in it)")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${ ceil(5.8) }", "Returns 6")
		);
	}
}
