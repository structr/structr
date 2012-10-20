/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket.command;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.entity.AbstractNode;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class GetCommand extends AbstractCommand {

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		AbstractNode node = getNode(webSocketData.getId());
		String view       = webSocketData.getView();

		if (node != null) {

			if (view == null) {

				view = PropertyView.All;

			}

			for (PropertyKey key : node.getPropertyKeys(view)) {

				webSocketData.setNodeData(key.name(), node.getProperty(key));

			}

			// send only over local connection (no broadcast)
			getWebSocket().send(webSocketData, true);

		} else {

			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "GET";
	}
}
