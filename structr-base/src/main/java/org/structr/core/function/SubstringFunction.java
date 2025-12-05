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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class SubstringFunction extends CoreFunction {

	@Override
	public String getName() {
		return "substring";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("str, start [, length ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 3);

			final String source = sources[0].toString();
			final int sourceLength = source.length();
			final int beginIndex = parseInt(sources[1]);
			final int length = sources.length == 3 ? parseInt(sources[2]) : sourceLength - beginIndex;
			final int endIndex = Math.min(beginIndex + length, sourceLength);

			if (beginIndex >= 0 && beginIndex < sourceLength && endIndex >= beginIndex && endIndex <= sourceLength) {

				return source.substring(beginIndex, endIndex);
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
			Usage.javaScript("Usage: ${{ $.substring(string, start [, length ]) }}."),
			Usage.structrScript("Usage: ${substring(string, start [, length ])}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the substring of the given string.";
	}

	@Override
	public String getLongDescription() {
		return """
		Returns a portion with the given length of the given string, starting from the given start index. 
		If no length parameter is given or the length would exceed the string length (calculated from the start index), the rest of the string is returned.
		""";
	}


	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("""
						${substring('This is my test', 2)}
						> is is my test
						${substring('This is my test', 8, 2)}
						> my
						${substring('This is my test', 8, 100)}
						> my test
						"""),
				Example.javaScript("${{ $.substring('This is my test', 2) }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("string", "URL to connect to"),
				Parameter.mandatory("start", "URL to connect to"),
				Parameter.optional("length", "length of string from start")
				);
	}
}
