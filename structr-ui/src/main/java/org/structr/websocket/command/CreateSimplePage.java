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

import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.Content;
import org.structr.web.entity.Page;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.H1;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.html.HtmlElement;
import org.structr.web.entity.html.Title;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyMap;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to create a simple HTML page
 *
 * @author Axel Morgner
 */
public class CreateSimplePage extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(WrapInComponentCommand.class.getName());
	private static CreateNodeCommand createNode;
	private static CreateRelationshipCommand createRel;
	private static String pageName;
	
	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		pageName = (String) webSocketData.getNodeData().get(Page.name.dbName());

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		createNode = Services.command(securityContext, CreateNodeCommand.class);
		createRel  = Services.command(securityContext, CreateRelationshipCommand.class);

		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Page page = (Page) createElement(null, Page.class.getSimpleName(), 0, null, pageName);

				page.setProperty(Page.contentType, "text/html");

				Html html   = (Html) createElement(page, Html.class.getSimpleName(), 0, page);
				Head head   = (Head) createElement(page, Head.class.getSimpleName(), 0, html);
				Body body   = (Body) createElement(page, Body.class.getSimpleName(), 1, html);
				Title title = (Title) createElement(page, Title.class.getSimpleName(), 0, head);

				// nodeData.put(Content.UiKey.content.name(), "Page Title");
				Content content = (Content) createElement(page, Content.class.getSimpleName(), 0, title);

				// nodeData.remove(Content.UiKey.content.name());
				content.setProperty(Content.content, "Page Title");

				H1 h1 = (H1) createElement(page, H1.class.getSimpleName(), 0, body);

				// nodeData.put(Content.UiKey.content.name(), "Page Title");
				Content h1Content = (Content) createElement(page, Content.class.getSimpleName(), 0, h1);

				// nodeData.remove(Content.UiKey.content.name());
				h1Content.setProperty(Content.content, "Page Title");

				Div div = (Div) createElement(page, Div.class.getSimpleName(), 1, body);

				// nodeData.put(Content.UiKey.content.name(), "Body Text");
				Content divContent = (Content) createElement(page, Content.class.getSimpleName(), 0, div);

				divContent.setProperty(Content.content, "Body Text");

				return page;

			}

		};

		try {

			// create nodes in transaction
			Services.command(securityContext, TransactionCommand.class).execute(transaction);
		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Could not create node.", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.toString()).build(), true);

		}

	}

	private AbstractNode createElement(final AbstractNode page, final String type, final int position, final AbstractNode parentElement) throws FrameworkException {
		return createElement(page, type, position, parentElement, null);
	}
	
	private AbstractNode createElement(final AbstractNode page, final String type, final int position, final AbstractNode parentElement, final String name) throws FrameworkException {

		PropertyMap nodeData = new PropertyMap();

		nodeData.put(AbstractNode.name, name != null ? name : type.toLowerCase());
		nodeData.put(AbstractNode.visibleToAuthenticatedUsers, true);

		PropertyMap relData = new PropertyMap();

		if (page != null) {

			String pageId = page.getUuid();

			relData.put(new LongProperty(pageId), position);

		}

		nodeData.put(AbstractNode.type, type);

		if (!Content.class.getSimpleName().equals(type)) {

			nodeData.put(HtmlElement.tag, type.toLowerCase());
		}

		AbstractNode element = createNode.execute(nodeData);

		if (parentElement != null) {

			createRel.execute(parentElement, element, RelType.CONTAINS, relData, false);
		}

		return element;

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "CREATE_SIMPLE_PAGE";

	}

}
