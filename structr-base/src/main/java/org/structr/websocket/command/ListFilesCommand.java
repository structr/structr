/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.ImageTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 */
public class ListFilesCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ListFilesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListFilesCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final String rawType                  = webSocketData.getNodeDataStringValue("type");
		final Traits type                     = Traits.of(rawType);
		final String sortOrder                = webSocketData.getSortOrder();
		final String sortKey                  = webSocketData.getSortKey();
		final int pageSize                    = webSocketData.getPageSize();
		final int page                        = webSocketData.getPage();
		final PropertyKey sortProperty        = type.key(sortKey);

		final QueryGroup<NodeInterface> query = StructrApp.getInstance(securityContext)
			.nodeQuery(rawType)
			.includeHidden()
			.sort(sortProperty, "desc".equals(sortOrder))
			.page(page)
			.pageSize(pageSize)
			.and()
			.key(type.key(AbstractFileTraitDefinition.PARENT_PROPERTY), null);

		// for image lists, suppress thumbnails
		if (type.contains(StructrTraits.IMAGE)) {

			query.key(Traits.of(StructrTraits.IMAGE).key(ImageTraitDefinition.IS_THUMBNAIL_PROPERTY), false);
		}

		try {

			// set full result list
			webSocketData.setResult(query.getResultStream());

			// send only over local connection
			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}
	}

	@Override
	public String getCommand() {
		return "LIST_FILES";
	}
}
