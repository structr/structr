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
import org.structr.common.trace.CallFrame;
import org.structr.common.trace.CallTrace;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CallStackFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "callStack";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final List<Map<String, Object>> frames = new ArrayList<>();
		final CallFrame current                = CallTrace.current();

		if (current != null) {

			for (final CallFrame frame : current.chain()) {

				frames.add(frame.toMap());
			}
		}

		return frames;
	}

	@Override
	public String getShortDescription() {
		return "Returns the chain of calls that led to the running code, innermost first.";
	}

	@Override
	public String getLongDescription() {
		return """
				The first element is the running method or script, the second the one that called it, and so on up to the
				request, job or other entry point that started the chain. Calls into another transaction with
				`doInNewTransaction()` and jobs started with `schedule()` keep the chain of the code that started them.

				Each element is an object with the following structure.

				| Key      | Type    | Description                                                                                     |
				|----------|---------|-------------------------------------------------------------------------------------------------|
				| kind     | String  | `METHOD`, `LIFECYCLE`, `TEMPLATE`, `PROPERTY`, `HTTP`, `WEBSOCKET`, `CRON`, `JOB`, `NEW_TRANSACTION`, `FLOW`, `BPMN`, `SCRIPT`, ... |
				| name     | String  | name of the method, or what the code is (`content`, `getInputStream`, ...)                      |
				| type     | String  | type that declares the method, or `null`                                                         |
				| id       | String  | uuid of the node holding the code, or `null`                                                     |
				| entity   | String  | uuid of the object the method runs on, or `null`                                                 |
				| isStatic | boolean | `true` for a static method                                                                       |
				""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
				{
					let caller = $.callStack()[1];
					if (caller && caller.kind === 'HTTP') {
						// called directly by an HTTP request, not from another method
					}
				}
				""", "Find out who called the running method")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Schema;
	}
}
