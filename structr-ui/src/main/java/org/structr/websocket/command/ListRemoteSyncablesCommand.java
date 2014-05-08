/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.cloud.CloudService;
import org.structr.cloud.message.ListSyncables;
import org.structr.cloud.message.SyncableInfo;
import org.structr.cloud.transmission.SingleTransmission;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class ListRemoteSyncablesCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(ListRemoteSyncablesCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final Map<String, Object> properties = webSocketData.getNodeData();
		final String username                = (String)properties.get("username");
		final String password                = (String)properties.get("password");
		final String host                    = (String)properties.get("host");
		final Long port                      = (Long)properties.get("port");

		if (host != null && port != null && username != null && password != null) {

			final App app    = StructrApp.getInstance();
			try (final Tx tx = app.tx()) {

				final List<SyncableInfo> syncables = CloudService.doRemote(new SingleTransmission<>(new ListSyncables(), username, password, host, port.intValue()), null);
				final StructrWebSocket webSocket   = getWebSocket();
				if (syncables != null) {

					final List<GraphObject> result = new LinkedList<>();
					for (final SyncableInfo info : syncables) {

						final GraphObjectMap map = new GraphObjectMap();
						map.put(GraphObject.id,     info.getId());
						map.put(NodeInterface.name, info.getName());
						map.put(GraphObject.type,   info.getType());

						result.add(map);
					}

					webSocketData.setResult(result);
					webSocket.send(webSocketData, true);
				}

			} catch (FrameworkException fex) {

				getWebSocket().send(MessageBuilder.status().code(400).message(fex.getMessage()).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("The PULL command needs sourceId, username, password, host, port and key!").build(), true);
		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "LIST_SYNCABLES";
	}
}

