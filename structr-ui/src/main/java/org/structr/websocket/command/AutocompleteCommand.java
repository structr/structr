/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.autocomplete.AbstractHintProvider;
import org.structr.autocomplete.JavaHintProvider;
import org.structr.autocomplete.JavascriptHintProvider;
import org.structr.autocomplete.PlaintextHintProvider;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket command to support autocompletion in the backend ui.
 */
public class AutocompleteCommand extends AbstractCommand {

	private static final Logger logger                                   = LoggerFactory.getLogger(AutocompleteCommand.class.getName());
	private static final Property<List<GraphObjectMap>> list             = new GenericProperty("list");
	private static final Map<String, AbstractHintProvider> hintProviders = new HashMap<>();

	static {

		StructrWebSocket.addCommand(AutocompleteCommand.class);

		hintProviders.put("text/javascript",  new JavascriptHintProvider());
		hintProviders.put("text/x-java",      new JavaHintProvider());
		hintProviders.put("text",             new PlaintextHintProvider());
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final Map<String, Object> data    = webSocketData.getNodeData();
		final String id                   = webSocketData.getId();
		final List<GraphObject> result    = new LinkedList<>();
		final String contentType          = webSocketData.getNodeDataStringValueTrimmedOrDefault("contentType", "text/plain");

		if (contentType != null) {

			final AbstractHintProvider hintProvider = hintProviders.get(contentType);
			if (hintProvider != null) {

				final String currentToken  = webSocketData.getNodeDataStringValueTrimmed("currentToken");
				final String previousToken = webSocketData.getNodeDataStringValueTrimmed("previousToken");
				final String thirdToken    = webSocketData.getNodeDataStringValueTrimmed("thirdToken");
				final String type          = webSocketData.getNodeDataStringValueTrimmed("type");
				final int cursorPosition   = webSocketData.getNodeDataIntegerValue("cursorPosition");
				final int line             = webSocketData.getNodeDataIntegerValue("line");

				try {

					final List<GraphObject> hints = hintProvider.getHints(StructrApp.getInstance().get(AbstractNode.class, id), type, currentToken, previousToken, thirdToken, line, cursorPosition);
					result.addAll(hints);

				} catch(FrameworkException fex) {
					logger.warn("", fex);
				}

			} else {

				logger.warn("No HintProvider for content type {}, ignoring.", contentType);
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
