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

import java.io.IOException;

import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.common.FileHelper;
import org.structr.websocket.StructrWebSocket;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command for uploading files.
 * 
 * This command expects a file name and a base64-encoded string.
 * 
 * @author Axel Morgner
 */
public class UploadCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(UploadCommand.class.getName());
	
	static {
		
		StructrWebSocket.addCommand(UploadCommand.class);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app = StructrApp.getInstance(securityContext);
		
		try {

			app.beginTx();
			
			final String name                   = (String) webSocketData.getNodeData().get("name");
			final String rawData                = (String) webSocketData.getNodeData().get("fileData");
			org.structr.web.entity.File newFile = FileHelper.createFileBase64(securityContext, rawData, null);
			
			newFile.setProperty(AbstractNode.name, name);
			
			app.commitTx();

		} catch (Throwable t) {

			String msg = t.toString();

			// return error message
			getWebSocket().send(MessageBuilder.status().code(400).message("Could not upload file: ".concat((msg != null)
				? msg
				: "")).build(), true);
			
		} finally {
			
			app.finishTx();
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "UPLOAD";
	}
}