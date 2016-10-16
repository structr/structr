/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.console;

import java.util.Map;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.script.StructrScriptable;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class Console {

	private ActionContext actionContext  = null;
	private StructrScriptable scriptable = null;
	private ScriptableObject scope       = null;

	public Console(final SecurityContext securityContext, final Map<String, Object> parameters) {
		this.actionContext = new ActionContext(securityContext, parameters);
	}

	public String run(final String line) throws FrameworkException {

		final Context scriptingContext = Context.enter();

		init(scriptingContext);

		try {
			Object extractedValue = scriptingContext.evaluateString(scope, line, "interactive script, line ", 1, null);

			if (scriptable.hasException()) {
				throw scriptable.getException();
			}

			// prioritize written output over result returned from method
			final String output = actionContext.getOutput();
			if (output != null && !output.isEmpty()) {
				extractedValue = output;
			}

			if (extractedValue == null) {
				return "null";
			}

			return extractedValue.toString();

		} catch (final FrameworkException fex) {

			// just throw the FrameworkException so we dont lose the information contained
			throw fex;

		} catch (final Throwable t) {

			throw new FrameworkException(422, t.getMessage());

		} finally {

			Context.exit();
		}
	}

	public SecurityContext getSecurityContext() {
		return actionContext.getSecurityContext();
	}

	// ----- private methods -----
	private void init(final Context scriptingContext) {

		// Set version to JavaScript1.2 so that we get object-literal style
		// printing instead of "[object Object]"
		scriptingContext.setLanguageVersion(Context.VERSION_1_2);

		// Initialize the standard objects (Object, Function, etc.)
		// This must be done before scripts can be executed.
		if (this.scope == null) {
			this.scope = scriptingContext.initStandardObjects();
		}

		// set optimization level to interpreter mode to avoid
		// class loading / PermGen space bug in Rhino
		//scriptingContext.setOptimizationLevel(-1);

		if (this.scriptable == null) {

			this.scriptable = new StructrScriptable(actionContext, null, scriptingContext);
			this.scriptable.setParentScope(scope);

			// register Structr scriptable
			scope.put("Structr", scope, scriptable);
		}

		// clear output buffer
		actionContext.clear();
	}
}
