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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.Importer;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.List;

/**
 *
 */
public class SaveNodeCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(SaveNodeCommand.class.getName());

	static {

		StructrWebSocket.addCommand(SaveNodeCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		final String nodeId       = webSocketData.getId();
		final String modifiedHtml = webSocketData.getNodeDataStringValue("source");
		final App app             = StructrApp.getInstance(securityContext);
		final NodeInterface node  = getNode(nodeId);

		if (node != null) {

			TransactionCommand.registerNodeCallback(node, callback);

			final DOMNode sourceNode = node.as(DOMNode.class);

			try {

				// parse page from modified source
				final Page importedPage = Importer.parsePageFromSource(securityContext, modifiedHtml, "__SaveNodeCommand_Temporary_Page__");
				
				if (importedPage == null) {

					final String errorMessage = "Unable to parse " + modifiedHtml;
					logger.warn(errorMessage);
					getWebSocket().send(MessageBuilder.status().code(422).message(errorMessage).build(), true);

				} else {

					try {

						final List<DOMNode> bodyList = importedPage.getElementsByTagName("body");
						if (!bodyList.isEmpty()) {

							final Page hostPage = sourceNode.getOwnerDocument();
							DOMNode child       = bodyList.get(0);

							child = child.getFirstChild();

							// skip first div (why is it there?)
							if (child != null) {
								child = child.getFirstChild();
							}

							while (child != null) {

								hostPage.adoptNode(child);
								sourceNode.appendChild(child);

								// next sibling
								child = child.getNextSibling();
							}

						}

					} finally {

						// make sure we delete the imported page
						app.delete(importedPage);
					}
				}

			} catch (Throwable t) {

				logger.warn("", t);

				// send exception
				getWebSocket().send(MessageBuilder.status().code(422).message(t.toString()).build(), true);
			}

		} else {

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot save page").build(), true);
		}

	}

	@Override
	public String getCommand() {

		return "SAVE_NODE";

	}

}
