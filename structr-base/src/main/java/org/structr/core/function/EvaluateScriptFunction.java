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

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.Scripting;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class EvaluateScriptFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "evaluate_script";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("entity, source");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		if(sources != null && sources.length == 2 && sources[1] != null && sources[1] instanceof String && sources[0] != null && sources[0] instanceof GraphObject) {

			String script = "${" + sources[1].toString().trim() + "}";
			GraphObject entity = (GraphObject)sources[0];

			return Scripting.replaceVariables(ctx, entity, script, "evaluate_script()");
		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${evaluate_script(entity, script)}"),
			Usage.javaScript("Usage: ${$.evaluate_script(entity, script)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Evaluates a serverside script string in the context of the given entity.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
