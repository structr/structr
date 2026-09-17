/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.diff.websocket;

import com.google.gson.Gson;
import org.structr.common.error.FrameworkException;
import org.structr.diff.compare.ExportComparison;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Answers the admin UI's comparison of two exports held in the virtual filesystem.
 *
 * The comparison itself lives in {@link ExportComparison}, which the {@code compareExports()} function
 * calls as well. This class is the websocket half only: it reads the two uuids off the message, and turns
 * the report and the failures into what a websocket caller expects.
 */
public class CompareExportsCommand extends AbstractCommand {

	public static void register() {

		StructrWebSocket.addCommand(CompareExportsCommand.class);
	}

	@Override
	public String getCommand() {

		return "COMPARE_EXPORTS";
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(false);

		try {

			final String json = new Gson().toJson(ExportComparison.of(
				getWebSocket().getSecurityContext(),
				webSocketData.getNodeDataStringValue("leftId"),
				webSocketData.getNodeDataStringValue("rightId")));

			// FINISHED with the original callback is how a command answers one caller; echoing the request
			// back does not reach it. The report travels as JSON text because the websocket serializer
			// turns anything that is not a primitive into value.toString().
			getWebSocket().send(MessageBuilder.finished()
				.callback(webSocketData.getCallback())
				.data("json", json)
				.build(), true);

		} catch (final FrameworkException fex) {

			// the producer states the status it wants a caller to see, so both surfaces answer alike
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
		}
	}
}
