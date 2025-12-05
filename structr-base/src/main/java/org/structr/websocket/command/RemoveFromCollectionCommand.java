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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.List;

/**
 *
 */
public class RemoveFromCollectionCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(RemoveFromCollectionCommand.class.getName());

	static {

		StructrWebSocket.addCommand(RemoveFromCollectionCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String keyString  = webSocketData.getNodeDataStringValue("key");
		if (keyString == null) {

			logger.error("Unable to remove given object from collection: key is null");
			getWebSocket().send(MessageBuilder.status().code(400).build(), true);

		}

		final String idToRemove = webSocketData.getNodeDataStringValue("idToRemove");
		if (idToRemove == null) {

			logger.error("Unable to remove given object from collection: idToRemove is null");
			getWebSocket().send(MessageBuilder.status().code(400).build(), true);

		}

		GraphObject obj         = getNode(webSocketData.getId());
		if (obj != null) {

			if (!((NodeInterface)obj).isGranted(Permission.write, getWebSocket().getSecurityContext())) {

				getWebSocket().send(MessageBuilder.status().message("No write permission").code(400).build(), true);
				logger.warn("No write permission for {} on {}", new Object[]{getWebSocket().getCurrentUser().toString(), obj.toString()});
				return;

			}

		}

		if (obj == null) {

			// No node? Try to find relationship
			obj = getRelationship(webSocketData.getId());
		}

		GraphObject objToRemove = getNode(idToRemove);

		if (obj != null && objToRemove != null) {

			try {

				PropertyKey key = obj.getTraits().key(keyString);
				if (key != null) {

					List collection = Iterables.toList((Iterable) obj.getProperty(key));
					collection.remove(objToRemove);
					obj.setProperties(obj.getSecurityContext(), new PropertyMap(key, collection));

					if (obj instanceof NodeInterface) {

						TransactionCommand.registerNodeCallback((NodeInterface) obj, callback);

					} else if (obj instanceof RelationshipInterface) {

						TransactionCommand.registerRelCallback((RelationshipInterface) obj, callback);
					}

				}

			} catch (FrameworkException ex) {

				logger.error("Unable to set properties: {}", ((FrameworkException) ex).toString());
				getWebSocket().send(MessageBuilder.status().code(400).build(), true);

			}

		} else {

			logger.warn("Graph object with uuid {} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {

		return "REMOVE_FROM_COLLECTION";

	}

}
