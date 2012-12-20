/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket.command;

import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Component;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.Element;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to wrap an existing node in a (new) component node
 * @author Axel Morgner
 */
public class WrapInComponentCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(WrapInComponentCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// Node to wrap
		String nodeId                      = webSocketData.getId();
		final AbstractNode nodeToWrap      = getNode(nodeId);
		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final Map<String, Object> relData  = webSocketData.getRelData();
		final String pageId                = (String) relData.get("pageId");
		final String parentId              = (String) nodeData.get("parentId");
		final AbstractNode parentNode      = getNode(parentId);

		if (nodeToWrap != null) {

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					PropertyMap nodeProps  = PropertyMap.inputTypeToJavaType(securityContext, nodeData);
					Component newComponent = (Component) Services.command(securityContext, CreateNodeCommand.class).execute(nodeProps);
					String componentId     = newComponent.getUuid();

					RelationshipHelper.moveIncomingRelationships(SecurityContext.getSuperUserInstance(), nodeToWrap, newComponent, RelType.CONTAINS, pageId, componentId, false);

					if ((parentNode != null) && (newComponent != null)) {

						// First element in new component, so set position to 0
						PropertyMap relProps = new PropertyMap();

						relProps.put(new GenericProperty(pageId), 0);

						// relProps.put("pageId", pageId);
						relProps.put(new StringProperty("componentId"), componentId);

						try {
							Element.elements.createRelationship(securityContext, newComponent, nodeToWrap, relProps);
							
						} catch (Throwable t) {
							getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
						}

						RelationshipHelper.tagOutgoingRelsWithComponentId(newComponent, newComponent, componentId);

					} else {

						getWebSocket().send(MessageBuilder.status().code(404).build(), true);

					}

					return null;
				}
			};

			try {
				Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(transaction);
			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Could not create node.", fex);
				getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
			}

		} else {

			logger.log(Level.WARNING, "Node with uuid {0} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "WRAP";
	}
}
