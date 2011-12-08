/*
 *  Copyright (C) 2011 Axel Morgner
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

import org.structr.common.PropertyView;
import org.structr.core.entity.AbstractNode;
import org.structr.websocket.WebSocketMessage;

/**
 *
 * @author Christian Morgner
 */
public class GetCommand extends AbstractCommand {

	@Override
	public boolean processMessage(final WebSocketMessage webSocketData) {

		AbstractNode node = getNode(webSocketData.getId());
		String view = webSocketData.getView();

		if(node != null) {

			if(view == null) {
				view = PropertyView.All;
			}

			for(String key : node.getPropertyKeys(view)) {
				webSocketData.setData(key, node.getStringProperty(key));

			}

			// send only over local connection
			getWebSocket().send(getConnection(), webSocketData, true);
		}

		return false;
	}

	@Override
	public String getCommand() {
		return "GET";
	}
}
