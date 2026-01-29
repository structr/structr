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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;

public class TrimFunction extends CoreFunction {

	@Override
	public String getName() {
		return "trim";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("str");
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

						cleanList.add(obj.toString().trim());
					}
				}

				return cleanList;
			}

			if (StringUtils.isBlank(sources[0].toString())) {
				return "";
			}

			return sources[0].toString().trim();

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
			Usage.javaScript("Usage: ${{ $.trim(string) }}. Example: ${{trim($.this.text)}}"),
			Usage.structrScript("Usage: ${trim(string)}. Example: ${trim(this.text)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Removes whitespace at the edges of the given string.";
	}

	@Override
	public String getLongDescription() {
		return """
		Removes any leading or trailing whitespace from the given object. If the object is a string, a trimmed version 
		will be returned. If it is a collection, a collection of trimmed strings will be returned.""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("""
						${trim('         A text with lots of whitespace        ')}
						> 'A text with lots of whitespace'"""),
				Example.javaScript("""
						${{ $.trim(
							$.merge('     A text with lots of whitespace    ', '     Another text with lots of whitespace     ')
							)
						}}
						>['A text with lots of whitespace', 'Another text with lots of whitespace']""")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("object", "object to trim")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"A space is defined as any character whose codepoint is less than or equal to `U+0020` (the space character)."
		);
	}


}
