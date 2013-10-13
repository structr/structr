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

import org.structr.websocket.command.dom.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

/**
 * Wrap a content node in a new DOM element
 * 
 * @author Axel Morgner
 */
public class WrapContentCommand extends AbstractCommand {

	//private static final Logger logger = Logger.getLogger(WrapContentCommand.class.getName());

	static {
		
		StructrWebSocket.addCommand(WrapContentCommand.class);
	}
	
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final String parentId              = (String) nodeData.get("parentId");
		final String pageId                = webSocketData.getPageId();
		
		nodeData.remove("parentId");

		if (pageId != null) {

			// check for parent ID before creating any nodes
			if (parentId == null) {
		
				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without parentId").build(), true);		
				return;
			}

			// check if content node with given ID exists
			final DOMNode contentNode = getDOMNode(parentId);
			if (contentNode == null) {
		
				getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);		
				return;
			}
			
			final Document document = getPage(pageId);
			if (document != null) {

				final String tagName  = (String) nodeData.get("tagName");
				nodeData.remove("tagName");
				
				final DOMNode parentNode = (DOMNode) contentNode.getParentNode();

				
				try {

					final DOMNode elementNode = (DOMNode) Services.command(getWebSocket().getSecurityContext(), TransactionCommand.class).execute(new StructrTransaction() {

						@Override
						public DOMNode execute() throws FrameworkException {
							
							DOMNode newNode = null;
							
							if (tagName != null && !tagName.isEmpty()) {

								newNode = (DOMNode) document.createElement(tagName);

							}

							// append new node to parent parent node
							if (newNode != null) {

								parentNode.appendChild(newNode);

							}

							return newNode;
						}
						
					});
					
					Services.command(getWebSocket().getSecurityContext(), TransactionCommand.class).execute(new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {
							
							// append new node to parent parent node
							if (elementNode != null) {

								// append content node to new node
								elementNode.appendChild(contentNode);

							}

							return null;
						}
						
					});

				} catch (DOMException dex) {
						
					// send DOM exception
					getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);		
					
				} catch (FrameworkException ex) {
					
					Logger.getLogger(CreateAndAppendDOMNodeCommand.class.getName()).log(Level.SEVERE, null, ex);
				}
				
			} else {
		
				getWebSocket().send(MessageBuilder.status().code(404).message("Page not found").build(), true);		
			}
			
		} else {
		
			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot create node without pageId").build(), true);		
		}
	}

	@Override
	public String getCommand() {
		return "WRAP_CONTENT";
	}

}
