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
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PasswordPolicyViolationException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.Folder;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class UpdateCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(UpdateCommand.class.getName());

	private static final String NODE_ID_KEY   = "nodeId";
	private static final String RECURSIVE_KEY = "recursive";

	static {

		StructrWebSocket.addCommand(UpdateCommand.class);

	}

	private int count = 0;

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		try {

			final App app          = StructrApp.getInstance(getWebSocket().getSecurityContext());

			final String nodeId    = webSocketData.getNodeDataStringValue(NODE_ID_KEY);
			final boolean rec      = webSocketData.getNodeDataBooleanValue(RECURSIVE_KEY);
			final GraphObject obj  = getGraphObject(webSocketData.getId(), nodeId);

			if (obj == null) {

				logger.warn("Graph object with uuid {} not found.", webSocketData.getId());
				getWebSocket().send(MessageBuilder.status().code(404).build(), true);

				return;
			}

			webSocketData.getNodeData().remove("recursive");

			// If it's a node, check permissions
			try (final Tx tx = app.tx()) {

				if (obj instanceof AbstractNode) {

					final AbstractNode node = (AbstractNode) obj;

					if (!node.isGranted(Permission.write, getWebSocket().getSecurityContext())) {

						getWebSocket().send(MessageBuilder.status().message("No write permission").code(400).build(), true);
						logger.warn("No write permission for {} on {}", new Object[]{getWebSocket().getCurrentUser().toString(), obj.toString()});

						tx.success();
						return;
					}
				}

				tx.success();
			}

			final Set<String> entities = new LinkedHashSet<>();
			PropertyMap properties = null;

			try (final Tx tx = app.tx()) {

				collectEntities(entities, obj, null, rec);

				properties = PropertyMap.inputTypeToJavaType(this.getWebSocket().getSecurityContext(), obj.getClass(), webSocketData.getNodeData());

				tx.success();
			}

			final Iterator<String> iterator = entities.iterator();
			while (iterator.hasNext()) {

				count = 0;
				try (final Tx tx = app.tx()) {

					while (iterator.hasNext() && count++ < 100) {

						final String uuid = iterator.next();

						final NodeInterface nodeObj = app.getNodeById(uuid);
						if (nodeObj != null) {

							nodeObj.setProperties(nodeObj.getSecurityContext(), properties);

							TransactionCommand.registerNodeCallback(nodeObj, callback);

						} else {

							final RelationshipInterface relObj = app.getRelationshipById(uuid);

							if (relObj != null) {

								relObj.setProperties(relObj.getSecurityContext(), properties);
								TransactionCommand.registerRelCallback((RelationshipInterface) relObj, callback);
							}
						}
					}

					tx.success();
				}
			}

		} catch (PasswordPolicyViolationException ppve) {

			logger.warn("Could not update node: {}", ppve.getMessage());
			getWebSocket().send(MessageBuilder.status().code(ppve.getStatus()).message(ppve.toString()).build(), true);

		} catch (FrameworkException ex) {

			logger.debug("Exception occurred", ex);
			getWebSocket().send(MessageBuilder.status().code(ex.getStatus()).message(ex.getMessage()).jsonErrorObject(ex.toJSON()).build(), true);
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public String getCommand() {

		return "UPDATE";
	}

	private void collectEntities(final Set<String> entities, final GraphObject obj, final PropertyMap properties, final boolean recursive) throws FrameworkException {

		entities.add(obj.getUuid());

		if (recursive) {

			if (obj instanceof LinkedTreeNode) {

				final LinkedTreeNode node = (LinkedTreeNode) obj;

				for (Object child : node.treeGetChildren()) {

					collectEntities(entities, (GraphObject) child, properties, recursive);
				}

			} else if (obj instanceof Folder) {

				final Folder folder = (Folder) obj;

				entities.addAll(Folder.getAllChildNodes(folder).stream().map(abstractfile -> abstractfile.getUuid()).collect(Collectors.toList()));
			}
		}
	}
}
