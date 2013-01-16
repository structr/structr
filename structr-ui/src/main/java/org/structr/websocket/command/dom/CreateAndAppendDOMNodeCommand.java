package org.structr.websocket.command.dom;

import java.util.Map;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

/**
 *
 * @author Christian Morgner
 */
public class CreateAndAppendDOMNodeCommand extends AbstractCommand {

	static {
		
		StructrWebSocket.addCommand(CreateAndAppendDOMNodeCommand.class);
	}
	
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		Map<String, Object> nodeData = webSocketData.getNodeData();
		String parentId              = (String) nodeData.get("parentId");
		String pageId                = webSocketData.getPageId();
		
		if (pageId != null) {

			// check for parent ID before creating any nodes
			if (parentId == null) {
		
				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without parentId").build(), true);		
				return;
			}

			// check if parent node with given ID exists
			DOMNode parentNode = getDOMNode(parentId);
			if (parentNode == null) {
		
				getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);		
				return;
			}
			
			Document document = getPage(pageId);
			if (document != null) {

				String tagName  = (String) nodeData.get("tagName");
				DOMNode newNode = null;
				
				try {

					if (tagName != null && !tagName.isEmpty()) {

						newNode = (DOMNode)document.createElement(tagName);

					} else {

						newNode = (DOMNode)document.createTextNode("");
					}

					// append new node to parent
					if (newNode != null) {

						parentNode.appendChild(newNode);
					}
					
				} catch (DOMException dex) {
						
					// send DOM exception
					getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);		
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
		return "CREATE_AND_APPEND_DOM_NODE";
	}

}
