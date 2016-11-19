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

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.User;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 *
 */
public class FavoritesCommand extends AbstractCommand {

	private static final Logger logger                          = LoggerFactory.getLogger(LayoutsCommand.class.getName());

	static {

		StructrWebSocket.addCommand(FavoritesCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final Map<String, Object> data        = webSocketData.getNodeData();
		final String mode                     = (String)data.get("mode");
		final String fileId                   = (String)data.get("id");
		final Principal currentUser           = webSocket.getCurrentUser();

		if (mode == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: No mode given. Valid modes: add, remove").build(), true);

		} else if (fileId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: No file id given").build(), true);

		} else {

			final App app = StructrApp.getInstance(webSocket.getSecurityContext());

			try (final Tx tx = app.tx()) {

				final GraphObject file = app.get(fileId);

				if (file != null && file instanceof FileBase) {

					switch (mode) {

						case "add": {

							final List<FileBase> favorites = currentUser.getProperty(User.favoriteFiles);
							favorites.add((FileBase)file);
							currentUser.setProperties(currentUser.getSecurityContext(), new PropertyMap(User.favoriteFiles, favorites));

							getWebSocket().send(MessageBuilder.finished().callback(callback).build(), true);

							break;

						}

						case "remove": {

							final List<FileBase> favorites = currentUser.getProperty(User.favoriteFiles);
							favorites.remove((FileBase)file);
							currentUser.setProperties(currentUser.getSecurityContext(), new PropertyMap(User.favoriteFiles, favorites));

							getWebSocket().send(MessageBuilder.finished().callback(callback).build(), true);

							break;

						}

						default:

							getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: Invalid mode '" + mode + "'. Valid modes: add, remove").build(), true);

					}

				} else {

					getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: File with id '" + fileId + "'does not exist!").build(), true);

				}

				tx.success();

			} catch (FrameworkException fex) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: File with id '" + fileId + "'does not exist!").build(), true);

			}

		}

	}

	@Override
	public String getCommand() {

		return "FAVORITES";

	}

}