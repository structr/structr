/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.ImageTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Set;

/**
 * Websocket command to retrieve nodes of a given type which are on root level,
 * i.e. not children of another node.
 *
 * To get all nodes of a certain type, see the {@link GetCommand}.
 *
 */
public class ListCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ListCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		final String rawType                  = webSocketData.getNodeDataStringValue("type");
		final String properties               = webSocketData.getNodeDataStringValue("properties");
		final boolean rootOnly                = webSocketData.getNodeDataBooleanValue("rootOnly");

		Traits type = Traits.of(rawType);
		if (type == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Type " + rawType + " not found").build(), true);
			return;
		}

		if (properties != null) {
			securityContext.setCustomView(StringUtils.split(properties, ","));
		}

		final String sortOrder         = webSocketData.getSortOrder();
		final String sortKey           = webSocketData.getSortKey();
		final int pageSize             = webSocketData.getPageSize();
		final int page                 = webSocketData.getPage();
		final PropertyKey sortProperty = type.key(sortKey);
		final QueryGroup query         = StructrApp.getInstance(securityContext).nodeQuery().sort(sortProperty, "desc".equals(sortOrder)).page(page).pageSize(pageSize).and().type(rawType);

		if (type.contains(StructrTraits.FILE)) {

			if (rootOnly) {
				query.key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.HAS_PARENT_PROPERTY), false);
			}

			// inverted as isThumbnail is not necessarily present in all objects inheriting from FileBase
			query.not().key(Traits.of(StructrTraits.IMAGE).key(ImageTraitDefinition.IS_THUMBNAIL_PROPERTY), true);

			TransactionCommand.getCurrentTransaction().prefetch(StructrTraits.ABSTRACT_FILE, StructrTraits.ABSTRACT_FILE, Set.of(
				"all/INCOMING/CONTAINS",
				"all/OUTGOING/CONFIGURED_BY"
			));
		}

		// important
		if (type.contains(StructrTraits.FOLDER) && rootOnly) {

			query.key(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.HAS_PARENT_PROPERTY), false);

			TransactionCommand.getCurrentTransaction().prefetch(StructrTraits.ABSTRACT_FILE, StructrTraits.ABSTRACT_FILE, Set.of(
				"all/INCOMING/CONTAINS",
				"all/OUTGOING/CONFIGURED_BY"
			));
		}

		try {

			// set full result list
			webSocketData.setResult(query.getResultStream());

			// send only over local connection
			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException fex) {

			logger.warn("Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}
	}

	@Override
	public String getCommand() {
		return "LIST";
	}
}
