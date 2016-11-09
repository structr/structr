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
package org.structr.websocket.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.VersionHelper;
import org.structr.console.Console;
import org.structr.console.tabcompletion.TabCompletionResult;
import org.structr.util.Writable;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 * Command to interact with a multi-mode server console.
 */
public class ConsoleCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ConsoleCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ConsoleCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final String sessionId = webSocketData.getSessionId();
		logger.debug("CONSOLE received from session {}", sessionId);

		final String line                     = (String) webSocketData.getNodeData().get("line");
		final Boolean completion              = (Boolean) webSocketData.getNodeData().get("completion");

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

                Console console = getWebSocket().getConsole();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWritable writeable = new OutputStreamWritable(out);


		try {

			if (Boolean.TRUE.equals(completion)) {

				final List<TabCompletionResult> tabCompletionResult = console.getTabCompletion(line);

				final JsonArray commands = new JsonArray();

				for (final TabCompletionResult res : tabCompletionResult) {
					commands.add(new JsonPrimitive(res.getCommand()));
				}

				getWebSocket().send(MessageBuilder.forName(getCommand())
						.callback(webSocketData.getCallback())
						.data("commands", commands)
						.data("prompt", console.getPrompt())
						.data("mode", console.getMode())
						.data("versionInfo", VersionHelper.getFullVersionInfo())
						.message(out.toString("UTF-8"))
						.build(), true);

			} else {

				console.run(line, writeable);

				getWebSocket().send(MessageBuilder.forName(getCommand())
						.callback(webSocketData.getCallback())
						.data("prompt", console.getPrompt())
						.data("mode", console.getMode())
						.data("versionInfo", VersionHelper.getFullVersionInfo())
						.message(out.toString("UTF-8"))
						.build(), true);
			}


		} catch (Exception ex) {

			logger.debug("Error while executing console line {}", line, ex);

			getWebSocket().send(MessageBuilder.forName(getCommand())
					.callback(webSocketData.getCallback())
					.data("prompt", console.getPrompt())
					.data("mode", console.getMode())
					.data("versionInfo", VersionHelper.getFullVersionInfo())
					.message(ex.getMessage())
					.build(), true);
		}

	}

	//~--- get methods ----------------------------------------------------

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
