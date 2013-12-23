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
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.html.Link;
import org.structr.web.entity.html.relation.ResourceLink;
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
		final App app                         = StructrApp.getInstance(securityContext);
		String sourceId                       = webSocketData.getId();
		Map<String, Object> properties        = webSocketData.getNodeData();
		String targetId                       = (String) properties.get("targetId");
		final Link sourceNode                 = (Link) getNode(sourceId);
		final Linkable targetNode             = (Linkable) getNode(targetId);

		if ((sourceNode != null) && (targetNode != null)) {

			try {
				app.beginTx();
				app.create(sourceNode, targetNode, ResourceLink.class);
				app.commitTx();

			} catch (Throwable t) {

				getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);

			} finally {
				
				app.finishTx();
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
