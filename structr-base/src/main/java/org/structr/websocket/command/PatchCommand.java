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


import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedList;

/**
 *
 */
public class PatchCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(PatchCommand.class.getName());

	static {

		StructrWebSocket.addCommand(PatchCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final PropertyKey<String> contentKey = Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_PROPERTY);
		final NodeInterface node             = getNode(webSocketData.getId());
		final String patch                   = webSocketData.getNodeDataStringValue("patch");

		if (node != null) {

			final DiffMatchPatch dmp        = new DiffMatchPatch();
			final String oldText            = node.getProperty(contentKey);
			final LinkedList<Patch> patches = new LinkedList<>(dmp.patchFromText(patch));
			final Object[] results          = dmp.patchApply(patches, oldText);

			try {

				node.setProperty(contentKey, results[0].toString());

				TransactionCommand.registerNodeCallback(node, callback);

			} catch (Throwable t) {

				logger.warn("Could not apply patch {}", patch);
				getWebSocket().send(MessageBuilder.status().code(400).message("Could not apply patch. " + t.getMessage()).build(), true);
			}

		} else {

			logger.warn("Node with uuid {} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).message("Node with uuid " + webSocketData.getId() + " not found.").build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "PATCH";
	}
}
