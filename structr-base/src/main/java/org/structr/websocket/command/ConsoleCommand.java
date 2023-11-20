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
package org.structr.websocket.command;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.console.Console;
import org.structr.console.Console.ConsoleMode;
import org.structr.console.tabcompletion.TabCompletionResult;
import org.structr.util.Writable;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.structr.common.helper.VersionHelper;

/**
 * Command to interact with a multi-mode server console.
 */
public class ConsoleCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ConsoleCommand.class.getName());

	private static final String LINE_KEY           = "line";
	private static final String MODE_KEY           = "mode";
	private static final String COMPLETION_KEY     = "completion";
	private static final String COMMANDS_KEY       = "commands";
	private static final String PROMPT_KEY         = "prompt";
	private static final String VERSION_INFO_KEY   = "versionInfo";
	private static final String IS_JSON_KEY        = "isJSON";

	static {

		StructrWebSocket.addCommand(ConsoleCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final String sessionId = webSocketData.getSessionId();
		logger.debug("CONSOLE received from session {}", sessionId);

		Console console = getWebSocket().getConsole(ConsoleMode.JavaScript);

		final String  line       = webSocketData.getNodeDataStringValue(LINE_KEY);
		final String  mode       = webSocketData.getNodeDataStringValue(MODE_KEY);
		final Boolean completion = webSocketData.getNodeDataBooleanValue(COMPLETION_KEY);

		if (StringUtils.isNotBlank(mode)) {
			console    = getWebSocket().getConsole(ConsoleMode.valueOf(mode));
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWritable writeable = new OutputStreamWritable(out);

		try {
			if (Boolean.TRUE.equals(completion)) {

				final List<TabCompletionResult> tabCompletionResult = console.getTabCompletion(line);

				final List<String> commands = new ArrayList<>();

				for (final TabCompletionResult res : tabCompletionResult) {
					commands.add(res.getCommand());
				}

				getWebSocket().send(MessageBuilder.forName(getCommand())
						.callback(webSocketData.getCallback())
						.data(COMMANDS_KEY, commands)
						.data(PROMPT_KEY, console.getPrompt())
						.data(MODE_KEY, console.getMode())
						.data(VERSION_INFO_KEY, VersionHelper.getFullVersionInfo())
						.message(out.toString("UTF-8"))
						.build(), true);

			} else {

				console.run(line, writeable);

				getWebSocket().send(MessageBuilder.forName(getCommand())
						.callback(webSocketData.getCallback())
						.data(PROMPT_KEY, console.getPrompt())
						.data(MODE_KEY, console.getMode())
						.data(VERSION_INFO_KEY, VersionHelper.getFullVersionInfo())
						.message(out.toString("UTF-8"))
						.build(), true);
			}

		} catch (final FrameworkException ex) {

			logger.debug("Error while executing console line {}", line, ex);

			final String message = (ex.getCause() instanceof UnlicensedScriptException) ? ex.getCause().getMessage() : ex.toJSON().toString();

			getWebSocket().send(MessageBuilder.forName(getCommand())
					.callback(webSocketData.getCallback())
					.data(MODE_KEY, console.getMode())
					.data(VERSION_INFO_KEY, VersionHelper.getFullVersionInfo())
					.data(IS_JSON_KEY, true)
					.message(message)
					.build(), true);

		} catch (final IOException ex) {

			logger.debug("Error while executing console line {}", line, ex);

			getWebSocket().send(MessageBuilder.forName(getCommand())
					.callback(webSocketData.getCallback())
					.data(MODE_KEY, console.getMode())
					.data(VERSION_INFO_KEY, VersionHelper.getFullVersionInfo())
					.message(ex.getMessage())
					.build(), true);
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public String getCommand() {
		return "CONSOLE";
	}

	// ----- nested classes -----
	private static class OutputStreamWritable implements Writable {

		private Writer writer = null;

		public OutputStreamWritable(final OutputStream out) {
			this.writer = new OutputStreamWriter(out);
		}

		@Override
		public void print(final Object... text) throws IOException {

			if (text != null) {

				for (final Object o : text) {

					if (o != null) {

						writer.write(o.toString().replaceAll("\n", "\r\n"));

					} else {

						writer.write("null");
					}
				}
			}

			writer.flush();
		}

		@Override
		public void println(final Object... text) throws IOException {

			print(text);
			println();
			writer.flush();
		}

		@Override
		public void println() throws IOException {
			writer.write(10);
			writer.write(13);
		}

		@Override
		public void flush() throws IOException {
			writer.flush();
		}
	}
}
