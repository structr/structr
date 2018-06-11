/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
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
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 *
 *
 */
public class UpdateCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(UpdateCommand.class.getName());

	static {

		StructrWebSocket.addCommand(UpdateCommand.class);

	}

	private int count = 0;

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final App app          = StructrApp.getInstance(getWebSocket().getSecurityContext());
		final Boolean recValue = (Boolean) webSocketData.getNodeData().get("recursive");
		final String nodeId    = (String) webSocketData.getNodeData().get("nodeId");
		final boolean rec      = recValue != null ? recValue : false;
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

				tx.success();
			}
		}

		final Set<GraphObject> entities = new LinkedHashSet<>();
		PropertyMap properties = null;

		try (final Tx tx = app.tx()) {

			collectEntities(entities, obj, null, rec);

			properties = PropertyMap.inputTypeToJavaType(this.getWebSocket().getSecurityContext(), obj.getClass(), webSocketData.getNodeData());

			tx.success();
		}

		final Iterator<GraphObject> iterator = entities.iterator();
		while (iterator.hasNext()) {

			count = 0;
			try (final Tx tx = app.tx()) {

				while (iterator.hasNext() && count++ < 100) {

					setProperties(iterator.next(), properties, true);
				}

				// commit and close transaction
				tx.success();
			}

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

	//~--- set methods ----------------------------------------------------
	private void setProperties(final GraphObject obj, final PropertyMap properties, final boolean rec) throws FrameworkException {

		obj.setProperties(obj.getSecurityContext(), properties);

		if (obj instanceof NodeInterface) {

			TransactionCommand.registerNodeCallback((NodeInterface) obj, callback);

		} else if (obj instanceof RelationshipInterface) {

			TransactionCommand.registerRelCallback((RelationshipInterface) obj, callback);
		}
	}

	private void collectEntities(final Set<GraphObject> entities, final GraphObject obj, final PropertyMap properties, final boolean rec) throws FrameworkException {

		entities.add(obj);

		if (rec && obj instanceof LinkedTreeNode) {

			LinkedTreeNode node = (LinkedTreeNode) obj;

			for (Object child : node.treeGetChildren()) {

				collectEntities(entities, (GraphObject) child, properties, rec);
			}
		}
	}

}
