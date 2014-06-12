/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 *
 * @author Christian Morgner
 */
public class UpdateCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(UpdateCommand.class.getName());

	static {

		StructrWebSocket.addCommand(UpdateCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		GraphObject obj = getNode(webSocketData.getId());
		Boolean recValue = (Boolean) webSocketData.getNodeData().get("recursive");

		boolean rec = recValue != null ? recValue : false;

		webSocketData.getNodeData().remove("recursive");

		if (obj != null) {

			if (!getWebSocket().getSecurityContext().isAllowed(((AbstractNode) obj), Permission.write)) {

				getWebSocket().send(MessageBuilder.status().message("No write permission").code(400).build(), true);
				logger.log(Level.WARNING, "No write permission for {0} on {1}", new Object[]{getWebSocket().getCurrentUser().toString(), obj.toString()});
				return;

			}

		}

		if (obj == null) {

			// No node? Try to find relationship
			obj = getRelationship(webSocketData.getId());
		}

		if (obj != null) {

			try {

				setProperties(obj, PropertyMap.inputTypeToJavaType(this.getWebSocket().getSecurityContext(), obj.getClass(), webSocketData.getNodeData()), rec);

			} catch (FrameworkException ex) {

				logger.log(Level.SEVERE, "Unable to set properties: {0}", ((FrameworkException) ex).toString());
				getWebSocket().send(MessageBuilder.status().code(400).build(), true);

			}

		} else {

			logger.log(Level.WARNING, "Graph object with uuid {0} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {

		return "UPDATE";

	}

	//~--- set methods ----------------------------------------------------
	private void setProperties(final GraphObject obj, final PropertyMap properties, final boolean rec) throws FrameworkException {

		for (Entry<PropertyKey, Object> entry : properties.entrySet()) {

			PropertyKey key = entry.getKey();
			Object value = entry.getValue();

			obj.setProperty(key, value);

			if (rec && obj instanceof LinkedTreeNode) {

				LinkedTreeNode node = (LinkedTreeNode) obj;

				for (Object child : node.treeGetChildren()) {

					setProperties((GraphObject) child, properties, true);

				}
			}
		}
	}

}
