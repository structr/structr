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

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.Widget;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Websocket command to load suggestions for a given HTML element.
 */
public class GetSuggestionsCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetSuggestionsCommand.class.getName());

	static {

		StructrWebSocket.addCommand(GetSuggestionsCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final PropertyKey selectorsKey        = StructrApp.key(Widget.class, "selectors");
		final List<String> classes            = webSocketData.getNodeDataStringList("classes");
		final String name                     = webSocketData.getNodeDataStringValue("name");
		final String htmlId                   = webSocketData.getNodeDataStringValue("htmlId");
		final String tag                      = webSocketData.getNodeDataStringValue("tag");
		final App app                         = StructrApp.getInstance(securityContext);

		if (tag != null) {

			try {

				final List<Widget> result    = new ArrayList<>();
				final Element element        = new Element(tag);

				for (final String css : classes) {
					element.addClass(css);
				}

				if (name != null) {   element.attr("name", name); }
				if (htmlId != null) { element.attr("id",   htmlId); }

				try (final ResultStream<Widget> resultStream = app.nodeQuery(Widget.class).getResultStream()) {

					for (final Widget widget : resultStream) {

						final String[] selectors = getSelectors(widget, selectorsKey);
						if (selectors != null) {

							for (final String selector : selectors) {

								if (element.select(selector).first() != null) {

									result.add(widget);
									break;
								}
							}
						}
					}
				}

				webSocketData.setResult(result);

				// send only over local connection (no broadcast)
				getWebSocket().send(webSocketData, true);

			} catch (Throwable t) {

				logger.error("", t);
				getWebSocket().send(MessageBuilder.status().code(422).build(), true);
			}

		} else {

			// send empty result
			getWebSocket().send(webSocketData, true);
		}
	}

	@Override
	public String getCommand() {
		return "GET_SUGGESTIONS";
	}

	// ----- private methods -----
	private String[] getSelectors(final Widget widget, final PropertyKey selectorsKey) {

		final Object value = widget.getProperty(selectorsKey);
		if (value != null && value instanceof String[]) {

			return (String[])value;
		}

		return null;
	}
}
