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
import org.structr.autocomplete.AbstractHintProvider;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedList;
import java.util.List;

/**
 * Websocket command to support autocompletion in the backend ui.
 */
public class AutocompleteCommand extends AbstractCommand {

	private static final Logger logger                       = LoggerFactory.getLogger(AutocompleteCommand.class.getName());
	private static final Property<List<GraphObjectMap>> list = new GenericProperty("list");

	static {

		StructrWebSocket.addCommand(AutocompleteCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final String id                       = webSocketData.getId();
		final List<GraphObject> result        = new LinkedList<>();
		final String contentType              = webSocketData.getNodeDataStringValueTrimmedOrDefault("contentType", "text/plain");
		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		if (contentType != null) {

			final boolean isAutoscriptEnv = webSocketData.getNodeDataBooleanValue("isAutoscriptEnv");
			final String before           = webSocketData.getNodeDataStringValue("before");
			final String after            = webSocketData.getNodeDataStringValue("after");
			final int cursorPosition      = webSocketData.getNodeDataIntegerValue("cursorPosition");
			final int line                = webSocketData.getNodeDataIntegerValue("line");

			try {

				final List<GraphObject> hints = AbstractHintProvider.getHints(new ActionContext(securityContext), isAutoscriptEnv, StructrApp.getInstance().getNodeById(StructrTraits.NODE_INTERFACE, id), before, after, line, cursorPosition);
				result.addAll(hints);

			} catch(FrameworkException fex) {
				logger.warn("", fex);
			}

		} else {

			logger.warn("No content type for AutocompleteCommand, ignoring.");
		}

		// set full result list
		webSocketData.setResult(result);
		webSocketData.setRawResultCount(result.size());
		getWebSocket().send(webSocketData, true);
	}

	@Override
	public String getCommand() {
		return "AUTOCOMPLETE";
	}
}
