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
import org.structr.cloud.WebsocketProgressListener;
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
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.web.entity.File;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class ListRemoteSyncablesCommand extends AbstractCommand {

	private static final Property<Boolean> isSynchronized = new BooleanProperty("isSynchronized", false);

	static {

		StructrWebSocket.addCommand(ListRemoteSyncablesCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final Map<String, Object> properties = webSocketData.getNodeData();
		final String username                = (String)properties.get("username");
		final String password                = (String)properties.get("password");
		final String host                    = (String)properties.get("host");
		final String key                     = (String)properties.get("key");
		final String type                    = (String)properties.get("type");
		final Long port                      = (Long)properties.get("port");

		if (host != null && port != null && username != null && password != null && key != null) {

			final App app    = StructrApp.getInstance();
			try (final Tx tx = app.tx()) {

				final List<SyncableInfo> syncables = CloudService.doRemote(new SingleTransmission<>(new ListSyncables(type), username, password, host, port.intValue()), new WebsocketProgressListener(getWebSocket(), key));
				final StructrWebSocket webSocket   = getWebSocket();
				if (syncables != null) {

					final List<GraphObject> result = new LinkedList<>();
					for (final SyncableInfo info : syncables) {

						final GraphObjectMap map = new GraphObjectMap();
						map.put(GraphObject.id,               info.getId());
						map.put(NodeInterface.name,           info.getName());
						map.put(File.size,                    info.getSize());
						map.put(GraphObject.type,             info.getType());
						map.put(GraphObject.lastModifiedDate, info.getLastModified());

						// check for existance
						map.put(isSynchronized, isSynchronized(info));

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

	@Override
	public String getCommand() {
		return "LIST_SYNCABLES";
	}

	// ----- private methods -----
	private boolean isSynchronized(final SyncableInfo info) throws FrameworkException {

		if (info.isNode()) {

			return StructrApp.getInstance(getWebSocket().getSecurityContext()).nodeQuery().and(GraphObject.id, info.getId()).getFirst() != null;

		} else {

			return StructrApp.getInstance(getWebSocket().getSecurityContext()).relationshipQuery().and(GraphObject.id, info.getId()).getFirst() != null;
		}
	}
}
