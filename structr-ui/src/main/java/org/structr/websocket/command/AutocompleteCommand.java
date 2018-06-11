/**
 * Copyright (C) 2010-2018 Structr GmbH
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

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to return the children of the given node
 *
 *
 */
public class AutocompleteCommand extends AbstractCommand {

	private static final Logger logger                                   = LoggerFactory.getLogger(AutocompleteCommand.class.getName());
	private static final Property<List<GraphObjectMap>> list             = new GenericProperty("list");
	private static final Map<String, AbstractHintProvider> hintProviders = new HashMap<>();

	static {

		StructrWebSocket.addCommand(AutocompleteCommand.class);

		hintProviders.put("text/plain",             new PlaintextHintProvider());
		hintProviders.put("text/javascript",        new JavascriptHintProvider());
		hintProviders.put("application/javascript", new JavascriptHintProvider());
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final Map<String, Object> data    = webSocketData.getNodeData();
		final String id                   = webSocketData.getId();
		final List<GraphObject> result    = new LinkedList<>();
		final String contentType          = getOrDefault(data.get("contentType"), "text/plain");

		if (contentType != null) {

			final AbstractHintProvider hintProvider = hintProviders.get(contentType);
			if (hintProvider != null) {

				final String currentToken  = getAndTrim(data.get("currentToken"));
				final String previousToken = getAndTrim(data.get("previousToken"));
				final String thirdToken    = getAndTrim(data.get("thirdToken"));
				final String type          = getAndTrim(data.get("type"));
				final int cursorPosition   = getInt(data.get("cursorPosition"));
				final int line             = getInt(data.get("line"));

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

	// ----- private methods -----
	private String getAndTrim(final Object source) {

		if (source != null && source instanceof String) {
			return ((String)source).trim();
		}

		return "";
	}

	private String getOrDefault(final Object source, final String defaultValue) {

		if (source != null && source instanceof String) {
			return ((String)source).trim();
		}

		return defaultValue;
	}

	private int getInt(final Object source) {

		if (source != null) {

			if (source instanceof Number) {
				return ((Number)source).intValue();
			}

			if (source instanceof String) {
				try { return Integer.parseInt(source.toString()); } catch (Throwable t) {}
			}
		}

		return -1;
	}
}
