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
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.Tx;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.List;

/**
 *
 *
 */
public class FavoritesCommand extends AbstractCommand {

	private static final Logger logger                          = LoggerFactory.getLogger(FavoritesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(FavoritesCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final String mode                     = webSocketData.getNodeDataStringValue("mode");
		final String favoritableId            = webSocketData.getNodeDataStringValue("id");
		final PrincipalInterface currentUser           = webSocket.getCurrentUser();

		if (mode == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: No mode given. Valid modes: add, remove").build(), true);

		} else if (favoritableId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: No favoritable id given").build(), true);

		} else {

			final App app = StructrApp.getInstance(webSocket.getSecurityContext());

			try (final Tx tx = app.tx(true, true, true)) {

				final Favoritable file = app.get(Favoritable.class, favoritableId);
				if (file != null) {

					final List<Favoritable> favorites = Iterables.toList(currentUser.getFavorites());

					switch (mode) {

						case "add": {

							favorites.add((Favoritable)file);
							break;

						}

						case "remove": {

							favorites.remove((Favoritable)file);
							break;

						}

						default:

							getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: Invalid mode '" + mode + "'. Valid modes: add, remove").build(), true);
							return;

					}

					currentUser.setFavorites(favorites);
					getWebSocket().send(MessageBuilder.finished().callback(callback).build(), true);

				} else {

					getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: Favoritable with id '" + favoritableId + "'does not exist!").build(), true);

				}

				tx.success();

			} catch (FrameworkException fex) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Favorites Command: Favoritable with id '" + favoritableId + "'does not exist!").build(), true);

			}

		}

	}

	@Override
	public String getCommand() {

		return "FAVORITES";

	}

}