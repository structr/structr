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
import org.structr.core.GraphObject;
import org.structr.core.script.Scripting;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.List;

public class ReplaceFunction extends CoreFunction {

	@Override
	public String getName() {
		return "replace";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("template, entity");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final String template = sources[0].toString();
			GraphObject node = null;

			Object convertedInputNode = Function.toGraphObject(sources[1], 3);

			if (convertedInputNode instanceof GraphObject) {
				node = (GraphObject) convertedInputNode;
			}

			if (convertedInputNode instanceof List) {

				final List list = (List)convertedInputNode;
				if (list.size() == 1 && list.get(0) instanceof GraphObject) {

					node = (GraphObject)list.get(0);
				}
			}

			if (node != null) {

				// recursive replacement call, be careful here
				return Scripting.replaceVariables(ctx, node, template, "replace()");
			}

			return "";

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
			Usage.javaScript("Usage: ${{$.replace(template, source)}}."),
			Usage.structrScript("Usage: ${replace(template, source)}. Example: ${replace(\"${this.id}\", this)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Replaces script expressions in the given template with values from the given entity.";
	}

	@Override
	public String getLongDescription() {
		return "This function can be used to evaluate template expressions in database objects, for example to create customized e-mails etc.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${replace('Welcome, ${this.name}!', me)}"),
				Example.javaScript("""
						${{ $.replace('Welcome, ${this.name}!', $.me) }}
						> Welcome, admin!
						""")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"Allowing user input to be evaluated in a template expression poses a security risk. You have no control over what the user can do!"
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("template", "template for replacement"),
				Parameter.optional("entity", "target object")
				);
	}

}
