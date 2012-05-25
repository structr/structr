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

import java.util.HashMap;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.web.entity.Page;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.html.HtmlElement;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.RelType;
import org.structr.core.node.*;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Title;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to create a simple HTML page
 *
 * @author Axel Morgner
 */
public class CreateSimplePage extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(WrapInComponentCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final Command createNode              = Services.command(securityContext, CreateNodeCommand.class);
		final Command createRel               = Services.command(securityContext, CreateRelationshipCommand.class);
		StructrTransaction transaction        = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {
				
				Map<String, Object> nodeData = webSocketData.getNodeData();
				nodeData.put(AbstractNode.Key.visibleToAuthenticatedUsers.name(), true);
				nodeData.put(AbstractNode.Key.type.name(), Page.class.getSimpleName());
				
				
				Map<String, Object> relData = new HashMap<String, Object>();

				AbstractNode page = (AbstractNode) createNode.execute(nodeData);
				
				String pageId = page.getUuid();
				relData.put(pageId, 0);
				
				nodeData.put(AbstractNode.Key.type.name(), Html.class.getSimpleName());
				nodeData.put(HtmlElement.UiKey.name.name(), "New Html");
				nodeData.put(HtmlElement.UiKey.tag.name(), "html");
				
				AbstractNode html = (AbstractNode) createNode.execute(nodeData);
				
				createRel.execute(page, html, RelType.CONTAINS, relData, false);

				nodeData.put(AbstractNode.Key.type.name(), Head.class.getSimpleName());
				nodeData.put(HtmlElement.UiKey.name.name(), "New Head");
				nodeData.put(HtmlElement.UiKey.tag.name(), "head");

				AbstractNode head = (AbstractNode) createNode.execute(nodeData);
				
				createRel.execute(html, head, RelType.CONTAINS, relData, false);

				nodeData.put(AbstractNode.Key.type.name(), Body.class.getSimpleName());
				nodeData.put(HtmlElement.UiKey.name.name(), "New Body");
				nodeData.put(HtmlElement.UiKey.tag.name(), "body");
				
				AbstractNode body = (AbstractNode) createNode.execute(nodeData);

				relData.put(pageId, 1);
				createRel.execute(html, body, RelType.CONTAINS, relData, false);

				relData.put(pageId, 0);
				nodeData.put(AbstractNode.Key.type.name(), Title.class.getSimpleName());
				nodeData.put(HtmlElement.UiKey.name.name(), "New Title");
				nodeData.put(HtmlElement.UiKey.tag.name(), "title");
				
				AbstractNode title = (AbstractNode) createNode.execute(nodeData);

				createRel.execute(head, title, RelType.CONTAINS, relData, false);
				
				return page;
			}
		};

		try {

			// create nodes in transaction
			Services.command(securityContext, TransactionCommand.class).execute(transaction);
		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Could not create node.", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "CREATE_SIMPLE_PAGE";
	}
}
