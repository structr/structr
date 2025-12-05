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

import org.structr.api.util.Iterables;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class NthFunction extends CoreFunction {

	@Override
	public String getName() {
		return "nth";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("collection, index");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final int pos = Double.valueOf(sources[1].toString()).intValue();

			if (sources[0] instanceof List l && !l.isEmpty()) {

				final int size = l.size();

				if (pos >= size) {

					return null;

				}

				return l.get(Math.min(Math.max(0, pos), size - 1));
			}

			if (sources[0] instanceof Iterable i) {

				return Iterables.nth(i, pos);
			}

			if (sources[0].getClass().isArray()) {

				final Object[] arr = (Object[])sources[0];
				if (pos <= arr.length) {

					return arr[pos];
				}
			}

			return null;

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
			Usage.javaScript("Usage: ${{$.nth(collection, index)}}. Example: ${{$.nth($.this.children, 2)}}"),
			Usage.structrScript("Usage: ${nth(collection, index)}. Example: ${nth(this.children, 2)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the element with the given index of the given collection.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("collection", "collection to return element of"),
			Parameter.mandatory("index", "index of the object to return (0-based)")
		);
	}
	@Override
	public String getLongDescription() {
		return "`first()` and `last()`.";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${nth(find('User'), 1)}", "Return the second of the existing users")
		);
	}
}
