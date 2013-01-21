package org.structr.websocket.command.dom;

import java.util.Map;

import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import org.w3c.dom.DOMException;

/**
 *
 * @author Axel Morgner
 */
public class InsertBeforeCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(InsertBeforeCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		String id                    = webSocketData.getId();
		Map<String, Object> nodeData = webSocketData.getNodeData();
		String refId                 = (String) nodeData.get("refId");
		String parentId              = (String) nodeData.get("parentId");

		// check node to append
		if (id == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot no node to append").build(), true);

			return;

		}

		// check for parent ID
		if (parentId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without parentId").build(), true);

			return;

		}

		DOMNode refNode = getDOMNode(refId );

		// check if parent node with given ID exists
		DOMNode parentNode = getDOMNode(parentId);

		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);

			return;

		}

		DOMNode node = getDOMNode(id);

		try {

			// append node to parent
			if (node != null) {

				parentNode.insertBefore(node, refNode);
				
			}
		} catch (DOMException dex) {

			// send DOM exception
			getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);
		}

	}

	@Override
	public String getCommand() {

		return "INSERT_BEFORE";

	}

}
