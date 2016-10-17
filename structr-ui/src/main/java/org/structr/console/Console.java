/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.console.command.ConsoleCommand;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.function.Functions;
import org.structr.core.script.StructrScriptable;
import org.structr.schema.action.ActionContext;
import org.structr.util.Writable;

/**
 *
 */
public class Console {

	public enum ConsoleMode {
		cypher, javascript, structrscript, adminshell
	};

	private ConsoleMode mode             = ConsoleMode.javascript;
	private StructrScriptable scriptable = null;
	private ActionContext actionContext  = null;
	private ScriptableObject scope       = null;
	private Writable writable            = null;

	public Console(final SecurityContext securityContext, final Map<String, Object> parameters) {
		this.actionContext = new ActionContext(securityContext, parameters);
	}

	public String run(final String line) throws FrameworkException {

		if (line.startsWith("Console.setMode('javascript')") || line.startsWith("Console.setMode(\"javascript\")")) {

			mode = ConsoleMode.javascript;
			return "\r\nMode set to 'JavaScript'.\r\n";

		} else if (line.startsWith("Console.setMode('cypher')") || line.startsWith("Console.setMode(\"cypher\")")) {

			mode = ConsoleMode.cypher;
			return "\r\nMode set to 'Cypher'.\r\n";

		} else if (line.startsWith("Console.setMode('structr')") || line.startsWith("Console.setMode(\"structr\")")) {

			mode = ConsoleMode.structrscript;
			return "\r\nMode set to 'StructrScript'.\r\n";

		} else if (line.startsWith("Console.setMode('shell')") || line.startsWith("Console.setMode(\"shell\")")) {

			mode = ConsoleMode.adminshell;
			return "\r\nMode set to 'AdminShell'. Type 'help' to get a list of commands.\r\n";

		} else {

			switch (mode) {

				case cypher:
					return runCypher(line);

				case javascript:
					return runJavascript(line);

				case structrscript:
					return runStructrScript(line);

				case adminshell:
					return runAdminShell(line);
			}
		}

		return null;
	}

	public SecurityContext getSecurityContext() {
		return actionContext.getSecurityContext();
	}

	public void setWritable(final Writable writable) {
		this.writable = writable;
	}

	// ----- private methods -----
	private String runCypher(final String line) throws FrameworkException {

		final App app                  = StructrApp.getInstance(actionContext.getSecurityContext());
		final long t0                  = System.currentTimeMillis();
		final List<GraphObject> result = app.cypher(line, Collections.emptyMap());
		final long t1                  = System.currentTimeMillis();
		final int size                 = result.size();
		final StringBuilder buf        = new StringBuilder();

		buf.append("Query returned ");
		buf.append(size);
		buf.append(" objects in ");
		buf.append((t1-t0));
		buf.append(" ms.");

		buf.append("\r\n");
		buf.append("\r\n");

		if (size <= 10) {

			buf.append(Functions.get("to_json").apply(actionContext, null, new Object[] { result } ));

		} else {

			buf.append("Too many results (> 10), please use LIMIT to reduce the result count of your Cypher query.");
		}

		return buf.toString();
	}

	private String runStructrScript(final String line) throws FrameworkException {

		final Object result = Functions.evaluate(actionContext, null, line);
		if (result != null) {

			return result.toString();
		}

		return "null";
	}

	private String runJavascript(final String line) throws FrameworkException {

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

	private String runAdminShell(final String line) throws FrameworkException {

		final List<String> parts = splitAnClean(line);
		if (!parts.isEmpty()) {

			final ConsoleCommand cmd = ConsoleCommand.getCommand(parts.get(0));
			if (cmd != null) {

				return cmd.run(actionContext.getSecurityContext(), parts, writable);
			}
		}

		return "Unknown command '" + line + "'";
	}

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

	private List<String> splitAnClean(final String src) {

		final List<String> parts = new ArrayList<>();

		for (final String part : src.split("[ ]+")) {

			final String trimmed = part.trim();

			if (StringUtils.isNotBlank(trimmed)) {

				parts.add(trimmed);
			}
		}

		return parts;
	}
}
