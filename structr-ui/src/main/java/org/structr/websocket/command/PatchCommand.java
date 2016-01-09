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

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Patch;

import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.dom.Content;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class PatchCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(PatchCommand.class.getName());
	
	static {
		StructrWebSocket.addCommand(PatchCommand.class);
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final AbstractNode node        = getNode(webSocketData.getId());
		Map<String, Object> properties = webSocketData.getNodeData();
		String patch                   = (String) properties.get("patch");

		if (node != null) {

			diff_match_patch dmp      = new diff_match_patch();
			String oldText            = node.getProperty(Content.content);
			LinkedList<Patch> patches = (LinkedList<Patch>) dmp.patch_fromText(patch);
			final Object[] results    = dmp.patch_apply(patches, oldText);

			try {
				node.setProperty(Content.content, results[0].toString());
				
			} catch (Throwable t) {

				logger.log(Level.WARNING, "Could not apply patch {0}", patch);
				getWebSocket().send(MessageBuilder.status().code(400).message("Could not apply patch. " + t.getMessage()).build(), true);

			}

		} else {

			logger.log(Level.WARNING, "Node with uuid {0} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).message("Node with uuid " + webSocketData.getId() + " not found.").build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "PATCH";

	}

}
