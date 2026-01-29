/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;


import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Template;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;


/**
 * Replace a template with another template.
 *
 * The existing template has to be part of a real page, the replacement template node
 * has to be part __ShadowDocument__ so it can be cloned into the page tree first.
 *
 */
public class ReplaceTemplateCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(ReplaceTemplateCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		String id				      = webSocketData.getId();
		String newTemplateId		  = webSocketData.getNodeDataStringValue("newTemplateId");

		// check node to append
		if (id == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot replace template, no id is given").build(), true);

			return;

		}

		// check for parent ID
		if (newTemplateId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot replace template node without newTemplateId").build(), true);

			return;

		}

		// check if parent node with given ID exists
		final Template newTemplate = getNodeAs(newTemplateId, Template.class, StructrTraits.TEMPLATE);

		if (newTemplate == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Replacement template node not found").build(), true);

			return;

		}

		final DOMNode templateToBeReplaced = getDOMNode(id);
		
		if (templateToBeReplaced == null) {
			
			getWebSocket().send(MessageBuilder.status().code(422).message("Unable to find template node to be replaced").build(), true);

			return;

		}

		try {

			// 1: Clone new template into tree			
			final DOMNode newClonedTemplate = CloneComponentCommand.cloneComponent(newTemplate, templateToBeReplaced.getParent());
			final DOMNode parent            = templateToBeReplaced.getParent();
			
			// 2: Move new template before existing template
			parent.insertBefore(newClonedTemplate, templateToBeReplaced);

			// 3: Move child nodes from existing template to new template
			for (final NodeInterface child : templateToBeReplaced.getAllChildNodes()) {
				newClonedTemplate.appendChild(child.as(DOMNode.class));
			}
			
			// 4: Remove old template node
			parent.removeChild(templateToBeReplaced);
			
			TransactionCommand.registerNodeCallback(newClonedTemplate, callback);

		} catch (DOMException | FrameworkException ex) {

			// send DOM exception
			getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);

		}


	}

	@Override
	public String getCommand() {

		return "REPLACE_TEMPLATE";

	}
}
