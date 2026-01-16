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
import org.structr.core.GraphObject;
import org.structr.core.script.Scripting;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class EvaluateScriptFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "evaluateScript";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("entity, source");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		if(sources != null && sources.length == 2 && sources[1] != null && sources[1] instanceof String && sources[0] != null && sources[0] instanceof GraphObject) {

			String script = "${" + sources[1].toString().trim() + "}";
			GraphObject entity = (GraphObject)sources[0];

			return Scripting.replaceVariables(ctx, entity, script, "evaluateScript()");
		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${evaluateScript(entity, script)}"),
			Usage.javaScript("Usage: ${{ $.evaluateScript(entity, script); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Evaluates a serverside script string in the context of the given entity.";
	}

	@Override
	public String getLongDescription() {
		return "You can use this function to evaluate a dynamic script in the context of a Structr application. Please note that there are many different way to exploit this function to gain privileged access to your application and the underlying server. It is almost never a good idea to use this function.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("entity", "`this` entity in the script"),
			Parameter.mandatory("script", "script source")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${evaluateScript(me, 'print(this.name)')}", "Print the name of the current user")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"This function poses a **very severe** security risk if you are using it with user-provided content!",
			"The function runs in an auto-script context, i.e. you don't need to put ${ ... } around the script.",
			"If you want to run a JavaScript snippet, put curly braces around the script: { ... }."
		);
	}
}
