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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.relationship.DOMChildren;

//~--- classes ----------------------------------------------------------------

/**
 * Insert a node as child of the given node
 *
 *
 */
public class InsertCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(InsertCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		// new node properties
		final Map<String, Object> properties = webSocketData.getNodeData();
		String parentId                      = (String) properties.get("id");
		final Map<String, Object> relData    = webSocketData.getRelData();

		if (parentId != null) {

			DOMNode parentNode        = (DOMNode) getNode(parentId);
			DOMNode nodeToInsert      = null;

			try {

				PropertyMap nodeProperties = PropertyMap.inputTypeToJavaType(securityContext, properties);

				nodeToInsert = app.create(DOMNode.class, nodeProperties);
				
			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Could not create node.", fex);
				getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

			}

			if ((nodeToInsert != null) && (parentNode != null)) {

				try {

					PropertyMap relProperties = PropertyMap.inputTypeToJavaType(securityContext, relData);
					app.create(parentNode, nodeToInsert, DOMChildren.class, relProperties);
					
				} catch (FrameworkException t) {

					getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);

				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("Insertion of new node failed.").build(), true);
		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "INSERT";

	}

}
