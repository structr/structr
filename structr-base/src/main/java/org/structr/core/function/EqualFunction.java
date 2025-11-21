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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class EqualFunction extends CoreFunction {

	@Override
	public String getName() {
		return "equal";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value1, value2");
	}

	@Override
	public List<String> aliases() {
		return Arrays.asList("eq");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		if (sources.length < 2) {

			return true;
		}

		if (sources[0] == null && sources[1] == null) {
			return true;
		}

		if (sources[0] == null || sources[1] == null) {
			return false;
		}

		return valueEquals(sources[0], sources[1]);
	}


	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${$.equal(value1, value2)}. Example: ${$.equal($.this.children.size, 0)}"),
			Usage.structrScript("Usage: ${equal(value1, value2)}. Example: ${equal(this.children.size, 0)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a boolean value that indicates whether the values are equal.";
	}

	@Override
	public String getLongDescription() {
		return """
		This function is very lenient; you can use it to compare dates and strings, strings and numbers, etc., based on the actual values of the converted objects.

		If the two values are of different types, Structr tries to determine the desired comparison type and convert the values before comparing.
		""";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function can also be called using just `eq()` as an alias."
		);
	}
}
