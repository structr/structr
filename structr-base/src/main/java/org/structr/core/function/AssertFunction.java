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

import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class AssertFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "assert";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("condition, statusCode, message");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources == null) {
				throw new IllegalArgumentException();
			}

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			final boolean condition = toBoolean(sources, 0);
			final int statusCode    = toInteger(sources, 1);
			final String message    = toString(sources, 2);

			if (!condition) {

				throw new AssertException(message, statusCode);
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${assert(condition, statusCode, message)}. Example: ${assert(empty(str), 422, 'str must be empty!')}"),
			Usage.javaScript("Usage: ${{Structr.assert(condition, statusCode, message);}}. Example: ${{Structr.assert(empty(str), 422, 'str must be empty!');}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Aborts the current request if the given condition evaluates to false.";
	}

	@Override
	public String getLongDescription() {
		return """
		This function allows you to check a precondition and abort the execution flow immediately if the condition is not satisfied, sending a customized error code and error message to the caller, along with all the error tokens that have accumulated in the error buffer.
		
		If you want to collect errors (e.g in a validation function), use `error()` which allows you to store error tokens in the context without aborting the execution flow.
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("condition", "condition to evaluate"),
			Parameter.mandatory("statusCode", "statusCode to send **if the condition evaluates to `false`**"),
			Parameter.mandatory("message", "error message to send **if the condition evaluates to `false`**")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.javaScript("$.assert($.me.name == 'admin', 422, 'Only admin users are allowed to access this resource.')", "Make sure only admin users can continue here")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"See also `getErrors()`, `clearError()`, `clearErrors()` and `error()`.",
			"Only works in schema methods, not in page rendering."
		);
	}

	// ----- private methods -----
	private boolean toBoolean(final Object[] args, final int index) {

		if (index >= args.length) {
			return false;
		}

		final Object value = args[index];

		if (value != null && value instanceof Boolean) {

			return ((Boolean)value);
		}

		return false;
	}

	private int toInteger(final Object[] args, final int index) {

		if (index >= args.length) {
			return 422;
		}

		final Object value = args[index];

		if (value != null && value instanceof Number) {

			return ((Number)value).intValue();
		}

		return 422;
	}

	private String toString(final Object[] args, final int index) {

		if (index >= args.length) {
			return "Unspecified assertion error";
		}

		final Object value = args[index];

		if (value != null) {

			return value.toString();
		}

		return "Unspecified assertion error";
	}
}
