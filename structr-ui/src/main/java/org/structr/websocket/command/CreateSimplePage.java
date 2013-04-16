/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.websocket.command;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.websocket.StructrWebSocket;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to create a simple HTML page
 *
 * @author Axel Morgner
 */
public class CreateSimplePage extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(CreateSimplePage.class.getName());

	static {

		StructrWebSocket.addCommand(CreateSimplePage.class);

	}
	
	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final String pageName                 = (String) webSocketData.getNodeData().get(Page.name.dbName());
		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		try {
			
			Page newPage = Page.createNewPage(securityContext, pageName);
			if (newPage != null) {
				
				Element html  = newPage.createElement("html");
				Element head  = newPage.createElement("head");
				Element body  = newPage.createElement("body");
				Element title = newPage.createElement("title");
				Element h1    = newPage.createElement("h1");
				Element div   = newPage.createElement("div");
				
				try {
					// add HTML element to page
					newPage.appendChild(html);
					
					// add HEAD and BODY elements to HTML
					html.appendChild(head);
					html.appendChild(body);
					
					// add TITLE element to HEAD
					head.appendChild(title);
					
					// add H1 element to BODY
					body.appendChild(h1);

					// add DIV element to BODY
					body.appendChild(div);
					
					// add text nodes
					title.appendChild(newPage.createTextNode("Page Title"));					
					h1.appendChild(newPage.createTextNode("Page Heading"));
					div.appendChild(newPage.createTextNode("Body Text"));
					
				} catch (DOMException dex) {
					
					dex.printStackTrace();
					
					throw new FrameworkException(422, dex.getMessage());
				}
			}
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			logger.log(Level.WARNING, "Could not create node.", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.toString()).build(), true);
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "CREATE_SIMPLE_PAGE";

	}

}
