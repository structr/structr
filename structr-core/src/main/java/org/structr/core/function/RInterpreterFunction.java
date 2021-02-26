/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

/**
 * Interpretes R code
 */
public class RInterpreterFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_R    = "Usage: ${r(<R code>)}";
	public static final String ERROR_MESSAGE_R_JS = "Usage: ${{Structr.r(<R code>)}}";

	@Override
	public String getName() {
		return "r";
	}

	@Override
	public String getSignature() {
		return "code";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String script = sources[0].toString();

			// create a script engine manager:
			ScriptEngineManager manager = new ScriptEngineManager();

			// create a Renjin engine:
			ScriptEngine engine = manager.getEngineByName("Renjin");

			// check if the engine has loaded correctly:
			if (engine == null) {
				throw new RuntimeException("Renjin Script Engine not found in the classpath.");
			}

			try {

				return engine.eval(script);

			} catch (final ScriptException e) {

				logger.error("Error while executing R script: {}", new Object[] { script, e });
				return null;
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_R_JS : ERROR_MESSAGE_R);
	}

	@Override
	public String shortDescription() {
		return "Executes the given R code using the internal R interpreter";
	}
}
