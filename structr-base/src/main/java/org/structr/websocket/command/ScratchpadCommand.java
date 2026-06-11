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
package org.structr.websocket.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.web.function.ToJsonFunction;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Command to interact with a multi-mode server console.
 */
public class ScratchpadCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ScratchpadCommand.class.getName());

	private static int scratchCounter = 0;

	private static final String COMMAND_KEY    = "command";
	private static final String SCRIPT_KEY     = "script";
	private static final String SCRATCH_ID_KEY = "scratchId";

	static {

		StructrWebSocket.addCommand(ScratchpadCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final String command = webSocketData.getNodeDataStringValue(COMMAND_KEY);
		final String script  = webSocketData.getNodeDataStringValue(SCRIPT_KEY);
		final int scratchId  = webSocketData.getNodeDataIntegerValue(SCRATCH_ID_KEY);

		try (final Tx tx = StructrApp.getInstance().tx()) {

			tx.prefetchHint("Websocket ScratchpadCommand");

			switch (command) {

				case "getNewScratchId": {

					final int scratchCounterForRun = scratchCounter;

					scratchCounter++;

					final String scratchLogIdentifier = "[scratch_" + scratchCounterForRun + "]";
					final GraphObjectMap result = new GraphObjectMap();
					result.setProperty(new IntProperty(SCRATCH_ID_KEY), scratchCounterForRun);
					result.setProperty(new StringProperty("logString"), scratchLogIdentifier);

					webSocketData.setResult(List.of(result));

					getWebSocket().send(webSocketData, true);
					break;
				}

				case "run": {

					final String scratchLogIdentifier = "[scratch_" + scratchId + "]";

					MDC.put("structrScratchMDC" , scratchLogIdentifier);

					final SecurityContext securityContext = getWebSocket().getSecurityContext();

					final Object scriptResult = Scripting.evaluate(new ActionContext(securityContext), null, "${" + script.trim() + "}", "scratchpad");

					final GraphObjectMap result = new GraphObjectMap();
					result.setProperty(new GenericProperty("result"), scriptResult);
					result.setProperty(new StringProperty("logString"), scratchLogIdentifier);

					webSocketData.setResult(List.of(result));
					webSocketData.setView(PropertyView.Public);
					getWebSocket().send(webSocketData, true);

					break;
				}

				default: {

					logger.error("Unknown command: {}", command);
					break;
				}
			}

			tx.success();

		} catch (final FrameworkException ex) {

			logger.debug("Error while executing scratchpad script: {}", script, ex);

			final String message = (ex.getCause() instanceof UnlicensedScriptException) ? ex.getCause().getMessage() : ex.toJSON().toString();

			getWebSocket().send(MessageBuilder.forName(getCommand())
					.callback(webSocketData.getCallback())
					.message(message)
					.build(), true);
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public String getCommand() {
		return "SCRATCHPAD";
	}
}
