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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.definitions.WidgetTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 *
 */
public class CreateLocalWidgetCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(CreateLocalWidgetCommand.class.getName());

	static {

		StructrWebSocket.addCommand(CreateLocalWidgetCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final App app                      = StructrApp.getInstance(getWebSocket().getSecurityContext());
		final String id	                   = webSocketData.getId();
		final String source                = webSocketData.getNodeDataStringValue("source");
		final String name                  = webSocketData.getNodeDataStringValue("name");

		// check for ID
		if (id == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot create widget without id").build(), true);

			return;

		}

		// check if parent node with given ID exists
		DOMNode node = getDOMNode(id);
		if (node == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Node not found").build(), true);

			return;
		}

		try {

			// convertFromInput
			final PropertyMap properties = new PropertyMap();
			final Traits traits          = Traits.of(StructrTraits.WIDGET);

			properties.put(traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY),   StructrTraits.WIDGET);
			properties.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   name);
			properties.put(traits.key(WidgetTraitDefinition.SOURCE_PROPERTY), source);

			final Widget widget = app.create(StructrTraits.WIDGET, properties).as(Widget.class);

			TransactionCommand.registerNodeCallback(widget, callback);

		} catch (Throwable t) {

			logger.warn(t.toString());

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message(t.toString()).build(), true);

		}

	}

	@Override
	public String getCommand() {

		return "CREATE_LOCAL_WIDGET";

	}

}
