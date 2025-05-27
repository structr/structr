/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.rest.maintenance.SnapshotCommand;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class SnapshotsCommand extends AbstractCommand {

	private static final Logger logger                            = LoggerFactory.getLogger(SnapshotsCommand.class.getName());
	private static final Property<List<String>> snapshotsProperty = new GenericProperty<>("snapshots");
	private static final Property<String> statusProperty          = new GenericProperty<>("status");

	static {

		StructrWebSocket.addCommand(SnapshotsCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		final String mode                     = webSocketData.getNodeDataStringValue("mode");
		final String name                     = webSocketData.getNodeDataStringValue("name");
		final String typesString              = webSocketData.getNodeDataStringValue("types");

		final List<String> types;
		if (typesString != null) {
			types = Arrays.asList(StringUtils.split(typesString, ","));
		} else {
			types = null;
		}

		if (mode != null) {

			final List<GraphObject> result = new LinkedList<>();

			switch (mode) {

				case "list":

					final List<String> snapshots = SnapshotCommand.listSnapshots();
					if (snapshots != null) {

						final GraphObjectMap snapshotContainer = new GraphObjectMap();

						snapshotContainer.put(snapshotsProperty, snapshots);
						result.add(snapshotContainer);

					}
					break;

				case "get":

					final Path snapshotFile = Paths.get(SnapshotCommand.getSnapshotsPath() + name);

					if (Files.exists(snapshotFile)) {

						try {
							final String content = new String(Files.readAllBytes(snapshotFile));

							// Send content directly
							getWebSocket().send(MessageBuilder.finished().callback(callback).data("schemaJson", content).build(), true);
							return;


						} catch (IOException ex) {
							LoggerFactory.getLogger(SnapshotsCommand.class.getName()).error("", ex);
						}

					}

					break;

				default:

					final GraphObjectMap msg = new GraphObjectMap();
					result.add(msg);

					try {

						app.command(SnapshotCommand.class).execute(mode, name, types);
						msg.put(statusProperty, "success");

					} catch (Throwable t) {
						logger.warn("", t);
						msg.put(statusProperty, t.getMessage());
					}
			}

			// set full result list
			webSocketData.setResult(result);
			webSocketData.setRawResultCount(1);
			getWebSocket().send(webSocketData, true);

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Mode must be one of list, export, add or restore.").build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "SNAPSHOTS";

	}
}
