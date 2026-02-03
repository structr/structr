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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;

import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;
import java.util.List;

public class SplitRegexFunction extends CoreFunction {

	@Override
	public String getName() {
		return "splitRegex";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("str [, regex ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String toSplit = sources[0].toString();
			String splitExpr = "[,;\\s]+";

			if (sources.length >= 2) {
				splitExpr = sources[1].toString();
			}

			return Arrays.asList(toSplit.split(splitExpr));

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
			Usage.javaScript("Usage: ${{ $.splitRegex(str[, regex]) }}."),
			Usage.structrScript("Usage: ${splitRegex(str[, regex])}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Splits the given string by given regex.";
	}

	@Override
	public String getLongDescription() {
		return """
		Uses the given separator to split the given string into a collection of strings. This is the opposite of `join()`.
		The default separator is a regular expression which splits the string at any of the following characters: `,;(whitespace)`
		The optional second parameter is used as regex. To use a literal string as separator, see `split()`.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${splitRegex('one,two,three,four')}"),
				Example.structrScript("${splitRegex('one;two;three;four')}"),
				Example.structrScript("${splitRegex('one two three four')}"),
				Example.structrScript("${splitRegex('one::two::three::four', ':+')}"),
				Example.structrScript("${splitRegex('one.two.three.four', '\\\\.')}"),
				Example.structrScript("${splitRegex('one:two&three%four', ':|&|%')}"),
				Example.javaScript("${{ $.splitRegex('one:two&three%four', ':|&|%') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("string", "string to split"),
				Parameter.optional("separator", "separator regex")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.String;
	}
}
