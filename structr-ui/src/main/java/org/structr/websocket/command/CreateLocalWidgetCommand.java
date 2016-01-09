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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 *
 */
public class CreateLocalWidgetCommand extends AbstractCommand {

	private static final Logger logger     = Logger.getLogger(CreateLocalWidgetCommand.class.getName());
	
	static {

		StructrWebSocket.addCommand(CreateLocalWidgetCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final App app                      = StructrApp.getInstance(getWebSocket().getSecurityContext());
		final String id	                   = webSocketData.getId();
		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final String source                = (String) nodeData.get("source");
		final String name                  = (String) nodeData.get("name");

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
			PropertyMap properties = new PropertyMap();

			properties.put(AbstractNode.type, Widget.class.getSimpleName());
			properties.put(AbstractNode.name, name);
			properties.put(Widget.source, source);

			app.create(Widget.class, properties);

		} catch (Throwable t) {

			logger.log(Level.WARNING, t.toString());

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message(t.toString()).build(), true);

		}

	}

	@Override
	public String getCommand() {

		return "CREATE_LOCAL_WIDGET";

	}

}
