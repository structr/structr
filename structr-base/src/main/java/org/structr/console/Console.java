/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.SyntaxErrorException;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.console.rest.RestCommand;
import org.structr.console.shell.AdminConsoleCommand;
import org.structr.console.tabcompletion.*;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.function.Functions;
import org.structr.core.graph.NativeQueryCommand;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.script.Snippet;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.util.CommandLineUtils;
import org.structr.util.Writable;

import java.io.IOException;
import java.util.*;


public class Console {

	private static Logger logger = LoggerFactory.getLogger(Console.class);

	public enum ConsoleMode {
		Cypher, JavaScript, StructrScript, AdminShell, REST
	};

	private final Map<ConsoleMode, TabCompletionProvider> tabCompletionProviders = new HashMap<>();
	private ConsoleMode mode                                                     = null;
	private ActionContext actionContext                                          = null;
	private String username                                                      = null;
	private String password                                                      = null;

	public Console(final SecurityContext securityContext, final ConsoleMode consoleMode, final Map<String, Object> parameters) {

		this.actionContext = new ActionContext(securityContext, parameters);
		this.mode          = consoleMode;

		tabCompletionProviders.put(ConsoleMode.Cypher,        new CypherTabCompletionProvider());
		tabCompletionProviders.put(ConsoleMode.JavaScript,    new JavaScriptTabCompletionProvider());
		tabCompletionProviders.put(ConsoleMode.StructrScript, new StructrScriptTabCompletionProvider());
		tabCompletionProviders.put(ConsoleMode.AdminShell,    new AdminTabCompletionProvider());
		tabCompletionProviders.put(ConsoleMode.REST,          new RestTabCompletionProvider());
	}

	public String runForTest(final String line) throws FrameworkException {

		final PrintWritable writable = new PrintWritable();

		// run
		try { run(line, writable); } catch (IOException ioex) {}

		return writable.getBuffer();
	}

	public void run(final String line, final Writable output) throws FrameworkException, IOException {

		if (line.startsWith("Console.getMode()")) {

			output.println("Mode is '" + getMode() + "'.");

		} else if (line.startsWith("Console.setMode('" + ConsoleMode.JavaScript.name() + "')") || line.startsWith("Console.setMode(\"" + ConsoleMode.JavaScript.name() + "\")")) {

			mode = ConsoleMode.JavaScript;
			output.println("Mode set to '" + ConsoleMode.JavaScript.name() + "'.");

		} else if (line.startsWith("Console.setMode('" + ConsoleMode.Cypher.name() + "')") || line.startsWith("Console.setMode(\"" + ConsoleMode.Cypher.name() + "\")")) {

			mode = ConsoleMode.Cypher;
			output.println("Mode set to '" + ConsoleMode.Cypher.name() + "'.");

		} else if (line.startsWith("Console.setMode('" + ConsoleMode.StructrScript.name() + "')") || line.startsWith("Console.setMode(\"" + ConsoleMode.StructrScript.name() + "\")")) {

			mode = ConsoleMode.StructrScript;
			output.println("Mode set to '" + ConsoleMode.StructrScript.name() + "'.");

		} else if (line.startsWith("Console.setMode('" + ConsoleMode.AdminShell.name() + "')") || line.startsWith("Console.setMode(\"" + ConsoleMode.AdminShell.name() + "\")")) {

			mode = ConsoleMode.AdminShell;
			output.println("Mode set to '" + ConsoleMode.AdminShell.name() + "'. Type 'help' to get a list of commands.");

		} else if (line.startsWith("Console.setMode('" + ConsoleMode.REST.name() + "')") || line.startsWith("Console.setMode(\"" + ConsoleMode.REST.name() + "\")")) {

			mode = ConsoleMode.REST;
			output.println("Mode set to '" + ConsoleMode.REST.name() + "'. Type 'help' to get a list of commands.");

		} else {

			final boolean notificationsEnabled = actionContext.getSecurityContext().doTransactionNotifications();
			actionContext.getSecurityContext().setDoTransactionNotifications(false);

			switch (mode) {

				case Cypher:
					runCypher(line, output);
					break;

				case JavaScript:
					runJavascript(line, output);
					break;

				case StructrScript:
					runStructrScript(line, output);
					break;

				case AdminShell:
					runAdminShell(line, output);
					break;

				case REST:
					RestCommand.run(this, line, output);
					break;
			}

			actionContext.getSecurityContext().setDoTransactionNotifications(notificationsEnabled);
		}
	}

	public List<TabCompletionResult> getTabCompletion(final String line) {

		final TabCompletionProvider provider = tabCompletionProviders.get(mode);
		if (provider != null) {

			return provider.getTabCompletion(actionContext.getSecurityContext(), line);
		}

		return Collections.emptyList();
	}

	public SecurityContext getSecurityContext() {
		return actionContext.getSecurityContext();
	}

	public String getMode() {
		return mode.name();
	}

	public String getPrompt() {

		final PrincipalInterface principal = actionContext.getSecurityContext().getUser(false);
		final StringBuilder buf   = new StringBuilder();

		switch (mode) {

			case Cypher:
			case JavaScript:
			case StructrScript:
			case AdminShell:
				if (principal != null) {
					buf.append(principal.getName());
				}
				break;

			case REST:
				if (username != null) {
					buf.append(username);
				} else {
					buf.append("anonymous");
				}
				break;
		}

		buf.append("@");
		buf.append("Structr");

		return buf.toString();
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public Map<String, Object> getVariables() {
		return actionContext.getRequestStore();
	}

	public void store(final String key, final Object value) {
		actionContext.store(key, value);
	}

	public Object retrieve(final String key) {
		return actionContext.retrieve(key);
	}

	// ----- private methods -----
	private void runCypher(final String line, final Writable writable) throws FrameworkException, IOException {

		final App app = StructrApp.getInstance(actionContext.getSecurityContext());

		try (final Tx tx = app.tx()) {

			final long t0                  = System.currentTimeMillis();

			final NativeQueryCommand nqc = app.command(NativeQueryCommand.class);
			nqc.setRunInNewTransaction(true);

			final List<GraphObject> result = Iterables.toList(nqc.execute(line));
			final long t1                  = System.currentTimeMillis();
			final int size                 = result.size();
			final int maxResults           = Settings.CypherConsoleMaxResults.getValue();

			writable.print("Query returned ", size, " objects in ", (t1-t0), " ms.");
			writable.println();
			writable.println();

			if (size <= maxResults) {

				writable.print(Functions.get("to_json").apply(actionContext, null, new Object[] { result } ));

			} else {

				writable.print("Too many results (> " + maxResults + "), please use LIMIT to reduce the result count of your Cypher query or increase the limit via structr.conf");
			}

			writable.println();

			tx.success();
		} catch (SyntaxErrorException see) {

			writable.println("Unexpected syntax error in console command. " + see.getMessage());
			logger.error("Unexpected syntax error in console command. {}", see.getMessage());
		}

	}

	private void runStructrScript(final String line, final Writable writable) throws FrameworkException, IOException {

		actionContext.setJavaScriptContext(false);

		try (final Tx tx = StructrApp.getInstance(actionContext.getSecurityContext()).tx()) {

			final EvaluationHints hints = new EvaluationHints();
			final Object result = Functions.evaluate(actionContext, null, new Snippet("console", line), hints);
			if (result != null) {

				if (result instanceof Iterable) {

					writable.println(Iterables.toList((Iterable)result).toString());

				} else {

					writable.println(result.toString());
				}
			}

			tx.success();

		} catch (UnlicensedScriptException ex) {

			throw new FrameworkException(422, "Unlicensed function called", ex);

		} catch (Throwable t) {

			throw new FrameworkException(422, t.getMessage());
		}
	}

	private void runJavascript(final String line, final Writable writable) throws FrameworkException {

		actionContext.setJavaScriptContext(true);

		try (final Tx tx = StructrApp.getInstance(actionContext.getSecurityContext()).tx()) {

			Snippet script = new Snippet("interactive script, line ", line, false);
			Object extractedValue = Scripting.evaluateScript(actionContext, null, "js", script);

			if (!extractedValue.toString().isEmpty()) {

				writable.println(extractedValue.toString());
			}

			tx.success();

		} catch (final FrameworkException fex) {

			// just throw the FrameworkException so we dont lose the information contained
			throw fex;

		} catch (final Throwable t) {

			throw new FrameworkException(422, t.getMessage());

		}
	}

	private void runAdminShell(final String line, final Writable writable) throws FrameworkException, IOException {

		final List<String> parts = splitAndClean(line);
		if (!parts.isEmpty()) {

			final AdminConsoleCommand cmd = AdminConsoleCommand.getCommand(parts.get(0));
			if (cmd != null) {

				if (cmd.requiresEnclosingTransaction()) {

					try (final Tx tx = StructrApp.getInstance(actionContext.getSecurityContext()).tx()) {

						cmd.run(actionContext.getSecurityContext(), parts, writable);

						tx.success();
					}

				} else {

					cmd.run(actionContext.getSecurityContext(), parts, writable);

				}

			} else {

				writable.println("Unknown command '" + line + "'.");
			}

		} else {

			writable.println("Syntax error.");
		}
	}

	private List<String> splitAndClean(final String src) {

		final List<String> list = new ArrayList<>();

		String[] parts;

		try {
			parts = CommandLineUtils.translateCommandline(src);

		} catch (Exception ex) {

			parts = src.split("[ ]+");
		}

		for (final String part : parts) {

			final String trimmed = part.trim();

			if (StringUtils.isNotBlank(trimmed)) {

				list.add(trimmed);
			}
		}

		return list;
	}

	// ----- nested classes -----
	private static class PrintWritable implements Writable {

		final StringBuilder buf = new StringBuilder();

		@Override
		public void print(final Object... text) throws IOException {
			for (final Object o : text) {
				buf.append(o);
			}
		}

		@Override
		public void println(final Object... text) throws IOException {
			for (final Object o : text) {
				buf.append(o);
			}
			println();
		}

		@Override
		public void println() throws IOException {
			buf.append("\r\n");
		}

		@Override
		public void flush() throws IOException {
		}

		public String getBuffer() {
			return buf.toString();
		}
	}
}
