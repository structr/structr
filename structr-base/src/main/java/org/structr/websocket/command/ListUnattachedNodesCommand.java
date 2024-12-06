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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.dom.*;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.structr.common.helper.PagingHelper;

/**
 * Websocket command to retrieve DOM nodes which are not attached to a parent
 * element.
 */
public class ListUnattachedNodesCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ListUnattachedNodesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListUnattachedNodesCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			// do search
			webSocketData.setResult(getUnattachedNodes(app, securityContext, webSocketData));

			// send only over local connection
			getWebSocket().send(webSocketData, true);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}

	}

	/**
	 * Return list of nodes which are not attached to a page and have no
	 * parent element (no incoming CONTAINS rel)
	 *
	 * @param app
	 * @param securityContext
	 * @param webSocketData
	 * @return
	 * @throws FrameworkException
	 */
	protected static Iterable<DOMNode> getUnattachedNodes(final App app, final SecurityContext securityContext, final WebSocketMessage webSocketData) throws FrameworkException {

		final Set<String> types    = SearchCommand.getAllSubtypesAsStringSet("Page");
		final String sortOrder     = webSocketData.getSortOrder();
		final String sortKey       = webSocketData.getSortKey();
		final int pageSize         = webSocketData.getPageSize();
		final int page             = webSocketData.getPage();

		final Query<DOMNode> query = app.nodeQuery("DOMNode")
			.includeHidden()
			.pageSize(pageSize)
			.page(page)
			.and(DOMNode.ownerDocumentProperty, null)
			.and(DOMNode.parentProperty, null);

		if (sortKey != null) {

			query.sort(StructrApp.key(DOMNode.class, sortKey), "desc".equals(sortOrder));
		}

		return Iterables.filter(n -> !types.contains(n.getType()), query.getResultStream());
	}

	@Override
	public String getCommand() {
		return "LIST_UNATTACHED_NODES";
	}
}
