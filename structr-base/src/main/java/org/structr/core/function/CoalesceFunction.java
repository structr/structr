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


public class CoalesceFunction extends CoreFunction {

	@Override
	public String getName() {
		return "coalesce";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("strings..", "list of strings to coalesce")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("coalesce(node.name, node.title, node.id)", "Returns either the name, the title or the UUID of a node, depending on which one is non-null"),
			Example.javaScript("$.coalesce(node.name, node.title, node.id)", "Returns either the name, the title or the UUID of a node, depending on which one is non-null")
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value1, value2, value3, ...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			final int length = sources.length;

			for (int i = 0; i < length; i++) {

				if (sources[i] != null) {
					return sources[i];
				}
			}

			// no non-null value was supplied
			return null;

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${coalesce(string1, string2...)}. Example: ${coalesce(node.name, node.title, node.id)}"),
			Usage.javaScript("Usage: ${{Structr.coalesce(string1, string2...)}}. Example: ${{Structr.coalesce(node.name, node.title, node.id)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the first non-null value in the list of expressions passed to it. In case all arguments are null, null will be returned.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
