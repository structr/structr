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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;

import static org.structr.core.function.Functions.cleanString;

public class CleanFunction extends CoreFunction {

	@Override
	public String getName() {
		return "clean";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof Iterable) {

				final List<String> cleanList = new LinkedList<>();

				for (final Object obj : (Iterable)sources[0]) {

					if (StringUtils.isBlank(obj.toString())) {

						cleanList.add("");

					} else {

						cleanList.add(cleanString(obj));
					}
				}

				return cleanList;
			}

			if (StringUtils.isBlank(sources[0].toString())) {
				return "";
			}

			return cleanString(sources[0]);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("string");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{$.clean(string)}}. Example: ${{$.clean($.this.stringWithNonWordChars)}}"),
			Usage.structrScript("Usage: ${clean(string)}. Example: ${clean(this.stringWithNonWordChars)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Cleans the given string.";
	}

	@Override
	public String getLongDescription() {
		return """
		This function can be used to convert complex strings or collections of strings (e.g. user names, article titles, etc.) into simple strings that can be used in URLs etc.
		
		| Characters | Action |
		| --- | --- | --- |
		| Whitespace | Replace with `-` (consecutive whitespaces are replaced with a single `-`) |
		|  `–'+/|\\` | Replace with `-` |
		| Uppercase letters | Replace with corresponding lowercase letter |
		| `<>.?(){}[]!,` | Remove |
		""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("stringOrList", "string or list of strings to clean")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${clean('This   is Än   example')}", "Results in \"this-is-an-example\""),
			Example.structrScript(
			"""
			${clean(merge('This   is Än   example', 'This   is   Änother   example'))}
			=> ['this-is-an-example', 'this-is-another-example']
			""", "Clean a list of strings"),
			Example.javaScript(
			"""
			${{ $.clean(['This   is Än   example', 'This   is   Änother   example'])}}
			=> ['this-is-an-example', 'this-is-another-example']
			""", "Clean a list of strings")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"Strings are normalized in the NFD form (see. http://www.unicode.org/reports/tr15/tr15-23.html) before the replacements are applied."
		);
	}
}
