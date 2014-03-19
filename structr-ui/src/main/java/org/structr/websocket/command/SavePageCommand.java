/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.GraphMergeHelper;
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.web.Importer;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 * @author Axel Morgner
 */
public class SavePageCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(SavePageCommand.class.getName());

	
	static {

		StructrWebSocket.addCommand(SavePageCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final String pageId                   = webSocketData.getId();
		final Map<String, Object> nodeData    = webSocketData.getNodeData();
		final String newSource                = (String) nodeData.get("source");
		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		Page page = getPage(pageId);
		if (page != null) {

			try {
				logger.log(Level.INFO, newSource);
				
				Importer imp = new Importer(securityContext, newSource, null, "imported-page", 5000, false, false);
					
				boolean parseOk = imp.parse();
				
				if (parseOk) {
					
					final Page newPage = imp.readPage();
					logger.log(Level.INFO, "New page created: ", newPage);
					
					Set<DOMNode> origDomNodes = DOMNode.getAllChildNodes(page);
					Set<DOMNode> newDomNodes = DOMNode.getAllChildNodes(newPage);
					
					origDomNodes.add(page);
					
					newPage.setProperty(DOMNode.dataHashProperty, page.getUuid());
					newDomNodes.add(newPage);
					
					GraphMergeHelper.merge(origDomNodes, newDomNodes, DOMNode.dataHashProperty);
					
					
				} else {
					getWebSocket().send(MessageBuilder.status().code(422).message("Unable to parse\n" + newSource).build(), true);
				}
				

			} catch (Throwable t) {

				logger.log(Level.WARNING, t.toString());
				t.printStackTrace();

				// send exception
				getWebSocket().send(MessageBuilder.status().code(422).message(t.toString()).build(), true);
			}

		} else {

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot save page").build(), true);
		}

	}

	@Override
	public String getCommand() {

		return "SAVE_PAGE";

	}

}
