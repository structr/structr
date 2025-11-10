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

import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class AssertFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE    = "Usage: ${assert(condition, statusCode, message)}. Example: ${assert(empty(str), 422, 'str must be empty!')}";
	public static final String ERROR_MESSAGE_JS = "Usage: ${{Structr.assert(condition, statusCode, message);}}. Example: ${{Structr.assert(empty(str), 422, 'str must be empty!');}}";

	@Override
	public String getName() {
		return "assert";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("condition, statusCode, message");
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
	public String usage(boolean inJavaScriptContext) {

		if (inJavaScriptContext) {

			return ERROR_MESSAGE_JS;
		}
		return ERROR_MESSAGE;
	}

	@Override
	public String getShortDescription() {
		return "Aborts the current request if the given condition evaluates to false.";
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
