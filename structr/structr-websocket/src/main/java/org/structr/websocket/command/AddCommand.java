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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.entity.RelationClass;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class AddCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(AddCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// create static relationship
		String sourceId                = webSocketData.getId();
		Map<String, Object> properties = webSocketData.getData();
		String targetId                = (String) properties.get("id");

		properties.remove("id");

		if (sourceId != null) {

			AbstractNode sourceNode = getNode(sourceId);
			AbstractNode targetNode = null;

			if (targetId != null) {

				targetNode = getNode(targetId);

			} else {

				String type = (String) properties.get("type");
				properties.remove("type");

				String tag = (String) properties.get("tag");
				properties.remove("tag");

				String name = (String) properties.get("name");
				properties.remove("name");

				final Map<String, Object> props = new HashMap<String, Object>();

				props.put("tag", tag);
				props.put("type", type);
				props.put("name", name);

				StructrTransaction transaction = new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {
						return Services.command(securityContext, CreateNodeCommand.class).execute(props);
					}
				};

				try {

					// create node in transaction
					targetNode = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);
				} catch (FrameworkException fex) {

					logger.log(Level.WARNING, "Could not create node.", fex);
					getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
				}

			}

			if ((sourceNode != null) && (targetNode != null)) {

				RelationClass rel = EntityContext.getRelationClass(sourceNode.getClass(), targetNode.getClass());

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
