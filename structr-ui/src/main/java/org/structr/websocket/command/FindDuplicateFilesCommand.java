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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.web.entity.AbstractFile;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket command to list all files with identical paths
 *
 */
public class FindDuplicateFilesCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(FindDuplicateFilesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(FindDuplicateFilesCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		final Query query = StructrApp.getInstance(securityContext).nodeQuery(AbstractFile.class).sort(AbstractFile.path);

		try {

			AbstractFile lastFile;
			String lastFilepath;
			boolean lastWasDupe = false;
			final ArrayList<GraphObject> filesWithSamePath = new ArrayList<>();

			final Iterator it = query.getAsList().iterator();

			if (it.hasNext()) {
				lastFile = (AbstractFile)it.next();
				lastFilepath = lastFile.getProperty(AbstractFile.path);

				while (it.hasNext()) {
					AbstractFile file = (AbstractFile)it.next();

					final String currentFilepath = file.getProperty(AbstractFile.path);

					if (lastFilepath.equals(currentFilepath)) {

						if (!lastWasDupe) {
							// if this is the first duplicate found we need to add both files
							filesWithSamePath.add(lastFile);
						}
						filesWithSamePath.add(file);
						lastWasDupe = true;

					} else {

						// only update the lastFilePath if it changed (saves calculating the virtual path when we know it did not change)
						lastFilepath = currentFilepath;
						lastWasDupe = false;

					}

					lastFile = file;

				}
			}

			// set full result list
			webSocketData.setResult(filesWithSamePath);
			webSocketData.setRawResultCount(filesWithSamePath.size());

			// send only over local connection
			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {

		return "FIND_DUPLICATES";

	}

}
