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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.rest.maintenance.SnapshotCommand;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to retrieve DOM nodes which are not attached to a parent element
 *
 *
 */
public class SnapshotsCommand extends AbstractCommand {

	private static final Logger logger                            = Logger.getLogger(SnapshotsCommand.class.getName());
	private static final Property<List<String>> snapshotsProperty = new GenericProperty<>("snapshots");
	private static final Property<String> statusProperty          = new GenericProperty<>("status");

	static {

		StructrWebSocket.addCommand(SnapshotsCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final Map<String, Object> data        = webSocketData.getNodeData();
		final String mode                     = (String)data.get("mode");
		final String name                     = (String)data.get("name");

		if (mode != null) {

			final List<GraphObject> result = new LinkedList<>();

			switch (mode) {

				case "list":
					final List<String> snapshots = SnapshotCommand.listSnapshots();
					if (snapshots != null) {

						final GraphObjectMap snapshotContainer = new GraphObjectMap();

						snapshotContainer.put(snapshotsProperty, snapshots);
						result.add(snapshotContainer);

						// set full result list
						webSocketData.setResult(result);
						webSocketData.setRawResultCount(1);
						getWebSocket().send(webSocketData, true);
					}
					break;

				default:
					final GraphObjectMap msg = new GraphObjectMap();
					result.add(msg);

					try {

						app.command(SnapshotCommand.class).execute(mode, name);
						msg.put(statusProperty, "success");

					} catch (Throwable t) {
						t.printStackTrace();
						msg.put(statusProperty, t.getMessage());
					}
			}

			// set full result list
			webSocketData.setResult(result);
			webSocketData.setRawResultCount(1);
			getWebSocket().send(webSocketData, true);

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Mode must be one of list, export, restore.").build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "SNAPSHOTS";

	}
}
