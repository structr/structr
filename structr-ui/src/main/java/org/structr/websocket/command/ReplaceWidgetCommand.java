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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;

import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Div;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 *
 */
public class ReplaceWidgetCommand extends AbstractCommand {

	private static final Logger logger     = Logger.getLogger(ReplaceWidgetCommand.class.getName());
	
	static {

		StructrWebSocket.addCommand(ReplaceWidgetCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final String id	                   = webSocketData.getId();
		String pageId                      = webSocketData.getPageId();
		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final String parentId              = (String) nodeData.get("parentId");
		final String baseUrl               = (String) nodeData.get("widgetHostBaseUrl");
		final App app                      = StructrApp.getInstance(getWebSocket().getSecurityContext());
		
		final Page page			= getPage(pageId);
		final AbstractNode origNode	= getNode(id);
		
		try {
			
			DOMNode existingParent = null;
			if (parentId != null) {
				// Remove original node from existing parent to ensure correct position
				existingParent = (DOMNode) getNode(parentId);
			}

			// Create temporary parent node
			DOMNode parent = app.create(Div.class);

			// Expand source code to widget
			Widget.expandWidget(getWebSocket().getSecurityContext(), page, parent, baseUrl, nodeData);

			DOMNode newWidget = (DOMNode) parent.getChildNodes().item(0);
			moveSyncRels((DOMElement) origNode, (DOMElement) newWidget);

			if (existingParent != null) {
				existingParent.removeChild((DOMNode) origNode);

			}

			deleteRecursively((DOMNode) origNode);

			// Set uuid of original node to new widget node
			newWidget.setProperty(GraphObject.id, id);

			if (existingParent != null) {

				// Append new widget to existing parent
				existingParent.appendChild(newWidget);
			}						

			// Delete temporary parent node
			app.delete(parent);
			
		} catch (FrameworkException ex) {
			
			logger.log(Level.SEVERE, null, ex);
			
		}

	}

	@Override
	public String getCommand() {

		return "REPLACE_WIDGET";

	}

	/**
	 * Delete node and all its child nodes.
	 * 
	 * NOTE: This method does not create a transaction.
	 * 
	 * @param node 
	 */
	private void deleteRecursively(final DOMNode node) throws FrameworkException {
		
		final DeleteNodeCommand deleteNode = StructrApp.getInstance(getWebSocket().getSecurityContext()).command(DeleteNodeCommand.class);
		
		// Delete original node recursively
		Set<DOMNode> allChildren = DOMNode.getAllChildNodes(node);

		for (DOMNode n : allChildren) {
			deleteNode.execute(n);
		}

		deleteNode.execute(node);
		
	}
	
	/**
	 * Move all incoming and outgoing SYNC relationships from the
	 * source node to the target node.
	 * 
	 * NOTE: This method does not create a transaction.
	 * 
	 * @param source
	 * @param target 
	 */
	private void moveSyncRels(final DOMElement source, final DOMElement target) throws FrameworkException {

		List<DOMNode> syncedNodes = source.getProperty(DOMNode.syncedNodes);
		
		if (syncedNodes != null) {
		
			for (DOMNode syncedNode : syncedNodes) {

				List<DOMNode> masterNodes = syncedNode.getProperty(DOMElement.syncedNodes);
				//masterNodes.remove(source);
				masterNodes.add(target);
				syncedNode.setProperty(DOMElement.syncedNodes, masterNodes);

			}
		}
		
		target.setProperty(DOMElement.syncedNodes, source.getProperty(DOMElement.syncedNodes));
		//source.setProperty(DOMElement.syncedNodes, Collections.EMPTY_LIST);
		
	}
}
