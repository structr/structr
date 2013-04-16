/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.websocket.command;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.Linkable;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import org.structr.web.entity.html.Link;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class LinkCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(LinkCommand.class);
	}
	
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		String sourceId                       = webSocketData.getId();
		Map<String, Object> properties        = webSocketData.getNodeData();
		String targetId                       = (String) properties.get("targetId");
		final AbstractNode sourceNode         = getNode(sourceId);
		final AbstractNode targetNode         = getNode(targetId);

		if ((sourceNode != null) && (targetNode != null)) {

			try {

				StructrTransaction transaction = new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Link.linkable.createRelationship(securityContext, sourceNode, targetNode);

						return null;
					}

				};

				Services.command(securityContext, TransactionCommand.class).execute(transaction);

			} catch (Throwable t) {

				getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);

			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("The LINK command needs id and targetId!").build(), true);
		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "LINK";

	}

}
