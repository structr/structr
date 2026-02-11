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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.script.Scripting;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class LogFunction extends CoreFunction {

	private static final Logger logger = LoggerFactory.getLogger(LogFunction.class.getName());

	@Override
	public String getName() {
		return "log";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("objects...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (sources == null) {
				throw new IllegalArgumentException();
			}

			final StringBuilder buf = new StringBuilder();
			for (final Object obj : sources) {

				if (obj != null) {
					buf.append(Scripting.formatForLogging(obj));
				}
			}

			logger.info(buf.toString());
			return "";

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${log(objects...)}. Example ${log('Hello World!', 'test', 123)}"),
			Usage.javaScript("Usage: ${{ $.log(objects...); }}. Example ${{ $.log('Hello World!', 'test', 123)); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Logs the given objects to the logfile.";
	}

	@Override
	public String getLongDescription() {
		return "This function takes one or more arguments and logs the string representation of all of them to the Structr logfile. Please note that the individual objects are logged in a single line, one after another, without a separator.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("objects...", "object or list of objects to log")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${log('user is ', $.me)}", "Logs a string with the current user ID")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Single nodes are printed as `NodeType(name, uuid)`, unless they are in a collection that is being logged.",
			"If you want a JSON representation in the log file, you can use `toJson(node, view)`",
			"If you use `JSON.stringify()`, the default view `public` will be used"
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.InputOutput;
	}

	@Override
	public List<Link> getLinkedConcepts() {

		final List<Link> linkedConcepts = super.getLinkedConcepts();

		linkedConcepts.add(Link.to("ispartof", ConceptReference.of(ConceptType.Topic, "Logging")));

		return linkedConcepts;
	}
}
