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

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

/**
 * print() with a trailing newline.
 */
public class PrintlnFunction extends CoreFunction {

	@Override
	public String getName() {

		return "println";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("objects...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			ctx.print(sources, caller);
			ctx.print(new Object[] { "\n" }, caller);

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${println(objects...)}."), Usage.javaScript("Usage: ${{ $.println(objects...)}}."));
	}

	@Override
	public String getShortDescription() {

		return "Prints the given strings or objects to the output buffer, followed by a newline.";
	}

	@Override
	public String getLongDescription() {

		return "Behaves exactly like `print()`, but appends a newline character after the given objects. Called without arguments it writes just the newline." + "\n\nIn a `Content` element whose content type is `text/plain` (or not set at all), Structr replaces newlines with `<br>` when the page is rendered, so a newline written here appears as a line break. In `text/html` content it stays a plain newline, which is only whitespace in HTML. Inside a `textarea` no replacement happens, so the newline is preserved as typed.";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.structrScript("${println('Hello, world!')}"),
				Example.structrScript("${each(find('Project'), println(data.name))}", "Writes one project name per line"),
				Example.javaScript("${{ $.println('Hello, world!') }}")
				);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("objects", "Objects that will be printed into the page rendering buffer"));
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Rendering;
	}
}
