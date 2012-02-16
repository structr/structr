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

import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelation;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class AddCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		// create static relationship
		String sourceId = webSocketData.getId();
		Map<String, Object> properties = webSocketData.getData();
		String targetId = (String) properties.get("id");
		properties.remove("id");

		if ((sourceId != null) && (targetId != null)) {

			AbstractNode sourceNode = getNode(sourceId);
			AbstractNode targetNode = getNode(targetId);

			if ((sourceNode != null) && (targetNode != null)) {

				DirectedRelation rel = EntityContext.getDirectedRelationship(sourceNode.getClass(), targetNode.getClass());

				if (rel != null) {

					try {
						rel.createRelationship(securityContext, sourceNode, targetNode, properties);
					} catch (Throwable t) {
						getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
					}

				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);

			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("Add needs id and data.id!").build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "ADD";
	}
}
