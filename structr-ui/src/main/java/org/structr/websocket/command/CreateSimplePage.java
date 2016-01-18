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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to create a simple HTML page
 *
 *
 */
public class CreateSimplePage extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(CreateSimplePage.class.getName());

	static {

		StructrWebSocket.addCommand(CreateSimplePage.class);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final String pageName                 = (String) webSocketData.getNodeData().get(Page.name.dbName());
		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		try {

			Page.createSimplePage(securityContext, pageName);

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			logger.log(Level.WARNING, "Could not create node.", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.toString()).build(), true);
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "CREATE_SIMPLE_PAGE";

	}

}
