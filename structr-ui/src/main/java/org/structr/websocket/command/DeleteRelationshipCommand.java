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

import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 *
 *
 */
public class DeleteRelationshipCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(DeleteRelationshipCommand.class.getName());

	static {

		StructrWebSocket.addCommand(DeleteRelationshipCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		final AbstractRelationship obj = getRelationship(webSocketData.getId());

		if (obj != null) {

			final App app = StructrApp.getInstance(securityContext);

			app.delete(obj);

		} else {
			// Don't throw a 404. If relationship doesn't exist, it doesn't need to be removed,
			// and everything is fine!.
			//logger.log(Level.WARNING, "Relationship with id {0} not found.", webSocketData.getId());
			//getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "DELETE_RELATIONSHIP";
	}
}
