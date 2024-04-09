/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.AbstractFile;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Websocket command to list all files with identical paths
 *
 */
public class FindDuplicateFilesCommand extends AbstractCommand {

	protected static final Logger logger = LoggerFactory.getLogger(FindDuplicateFilesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(FindDuplicateFilesCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final PropertyKey<String> path        = StructrApp.key(AbstractFile.class, "path");
		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final Query query                     = StructrApp.getInstance(securityContext).nodeQuery(AbstractFile.class).sort(path);

		try {

			AbstractFile lastFile = null;
			String lastFilepath = null;
			boolean lastWasDupe = false;
			final ArrayList<GraphObject> filesWithSamePath = new ArrayList<>();

			final Iterator it = query.getAsList().iterator();

			while (it.hasNext()) {

				final AbstractNode node = (AbstractNode)it.next();

				try {

					final AbstractFile file = (AbstractFile)node;
					final String currentFilepath = file.getProperty(path);

					// skip the first file as we can not compare it to the previous one
					if (lastFile != null) {

						if (currentFilepath.equals(lastFilepath)) {

							if (!lastWasDupe) {
								// if this is the first duplicate found we need to add both files
								filesWithSamePath.add(lastFile);
							}
							filesWithSamePath.add(file);
							lastWasDupe = true;

						} else {

							lastWasDupe = false;
						}

					}

					lastFilepath = currentFilepath;
					lastFile = file;

				} catch (ClassCastException cce) {

					logger.warn("Tried casting node '{}' of type '{}' to AbstractFile. Most likely a node type inheriting from File was deleted and an instance remains. Please delete this node or change its type.", node.getUuid(), node.getType());

				}

			}

			// set full result list
			webSocketData.setResult(filesWithSamePath);
			webSocketData.setRawResultCount(filesWithSamePath.size());

			// send only over local connection
			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException fex) {

			logger.warn("Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {

		return "FIND_DUPLICATES";

	}

}
