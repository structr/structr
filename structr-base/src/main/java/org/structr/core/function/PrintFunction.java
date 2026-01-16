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
import org.structr.schema.action.ActionContext;

import java.util.List;

/**
 *
 */
public class PrintFunction extends CoreFunction {

	@Override
	public String getName() {
		return "print";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("objects...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			ctx.print(sources, caller);

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
				Usage.structrScript("Usage: ${print(objects...)}."),
				Usage.javaScript("Usage: ${{ $.print(objects...)}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Prints the given strings or objects to the output buffer.";
	}

	@Override
	public String getLongDescription() {
		return "Prints the string representation of all of the given objects into the page rendering buffer. This method is often used in conjunction with `each()` to create rendering output for a collection of entities etc. in scripting context.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${print('Hello, world!')}"),
				Example.structrScript("${print(this.name, 'test')}"),
				Example.javaScript("${{ $.print('Hello, world!') }}"),
				Example.javaScript("${{ $.print($.get('this').name, 'test') }}")
				);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("objects", "Objects that will be printed into the page rendering buffer")
		);
	}
}
