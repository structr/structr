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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;

//~--- classes ----------------------------------------------------------------
/**
 *
 *
 */
public class DeleteNodeCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(DeleteNodeCommand.class.getName());

	static {

		StructrWebSocket.addCommand(DeleteNodeCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		final Boolean recursive = (Boolean) webSocketData.getNodeData().get("recursive");
		final AbstractNode obj = getNode(webSocketData.getId());

		if (obj != null) {

			final App app = StructrApp.getInstance(securityContext);

			try (final Tx tx = app.tx()) {

				if (!(obj.isGranted(Permission.delete, getWebSocket().getSecurityContext()))) {

					logger.log(Level.WARNING, "No delete permission for {0} on {1}", new Object[]{getWebSocket().getCurrentUser().toString(), obj.toString()});
					getWebSocket().send(MessageBuilder.status().message("No delete permission").code(400).build(), true);
					tx.success();

					return;

				}

			} catch (FrameworkException ex) {
				ex.printStackTrace();
			}

			if (Boolean.TRUE.equals(recursive)) {

				// Remove all child nodes first
				try {

					final List<AbstractNode> filteredResults = new LinkedList();
					if (obj instanceof DOMNode) {

						DOMNode node = (DOMNode) obj;

						filteredResults.addAll(DOMNode.getAllChildNodes(node));

					} else if (obj instanceof LinkedTreeNode) {

						LinkedTreeNode node = (LinkedTreeNode) obj;

						filteredResults.addAll(node.getAllChildNodes());

					}

					for (NodeInterface node : filteredResults) {
						app.delete(node);
					}

				} catch (FrameworkException fex) {

					logger.log(Level.WARNING, "Exception occured", fex);
					getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

				} catch (DOMException dex) {

					logger.log(Level.WARNING, "DOMException occured.", dex);
					getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);

				}

			}

			try {
				app.delete(obj);

			} catch (FrameworkException fex) {
				logger.log(Level.WARNING, "Unable to delete node(s)", fex);
			}

		} else {
			// Don't throw a 404. If node doesn't exist, it doesn't need to be removed,
			// and everything is fine!.
			//logger.log(Level.WARNING, "Node with id {0} not found.", webSocketData.getId());
			//getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "DELETE";
	}
}
