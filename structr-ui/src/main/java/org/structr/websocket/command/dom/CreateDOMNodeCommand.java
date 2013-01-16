package org.structr.websocket.command.dom;

import java.util.Map;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.Document;

/**
 *
 * @author Christian Morgner
 */
public class CreateDOMNodeCommand extends AbstractCommand {

	static {
		
		StructrWebSocket.addCommand(CreateDOMNodeCommand.class);
	}
	
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		Map<String, Object> nodeData = webSocketData.getNodeData();

		String pageId                = webSocketData.getPageId();
		if (pageId != null) {
			
			Document document = getPage(pageId);
			if (document != null) {

				String tagName    = (String) nodeData.get("tagName");
				if (tagName != null && !tagName.isEmpty()) {

					document.createElement(tagName);

				} else {

					document.createTextNode("");
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
		return "CREATE_DOM_NODE";
	}
}
