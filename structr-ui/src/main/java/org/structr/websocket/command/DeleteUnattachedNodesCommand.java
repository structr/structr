/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Template;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 * Websocket command to delete all DOM nodes which are not attached to a parent
 * element
 *
 * @author Axel Morgner
 */
public class DeleteUnattachedNodesCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(DeleteUnattachedNodesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(DeleteUnattachedNodesCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) throws FrameworkException {

		final SecurityContext securityContext        = getWebSocket().getSecurityContext();
		final App app                                = StructrApp.getInstance(securityContext);
		final Query query                            = app.nodeQuery();
		final List<? extends GraphObject> resultList = new LinkedList<>();
		final Set<AbstractNode> filteredResults      = new LinkedHashSet<>();

		query.includeDeletedAndHidden();
		query.orTypes(DOMElement.class);
		query.orType(Content.class);
		query.orType(Template.class);

		try (final Tx tx = app.tx()) {

			resultList.addAll(query.getAsList());

			// determine which of the nodes have incoming CONTAINS relationships and are not components
			for (GraphObject obj : resultList) {

				if (obj instanceof DOMNode) {

					DOMNode node = (DOMNode) obj;

					if (node.getProperty(DOMNode.ownerDocument) == null) {
						filteredResults.add(node);
					}

					for (final DOMNode child : DOMNode.getAllChildNodes(node)) {

						if (child.getProperty(DOMNode.ownerDocument) == null) {
							filteredResults.add(child);
						}
					}
				}
			}

			tx.success();
		}

		final Iterator<AbstractNode> iterator = filteredResults.iterator();
		int count                             = 0;

		while (iterator.hasNext()) {

			count = 0;
			try (final Tx tx = app.tx()) {

				while (iterator.hasNext() && count++ < 100) {

					app.delete(iterator.next());
				}

				// commit and close transaction
				tx.success();
			}
		}
	}

	@Override
	public String getCommand() {

		return "DELETE_UNATTACHED_NODES";

	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

}
