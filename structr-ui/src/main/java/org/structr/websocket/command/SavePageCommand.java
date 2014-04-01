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

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.web.Importer;
import org.structr.web.diff.InvertibleModificationOperation;
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

		final String pageId = webSocketData.getId();
		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final String modifiedHtml = (String) nodeData.get("source");
		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app = StructrApp.getInstance(securityContext);

		Page modifiedPage = null;

		Page sourcePage = getPage(pageId);
		if (sourcePage != null) {

			try {

				// parse page from modified source
				modifiedPage = Importer.parsePageFromSource(securityContext, modifiedHtml, "Test");

				final List<InvertibleModificationOperation> changeSet = Importer.diffPages(sourcePage, modifiedPage);

				for (final InvertibleModificationOperation op : changeSet) {

					// execute operation
					op.apply(app, sourcePage, modifiedPage);

				}


			} catch (Throwable t) {

				logger.log(Level.WARNING, t.toString());
				t.printStackTrace();

				// send exception
				getWebSocket().send(MessageBuilder.status().code(422).message(t.toString()).build(), true);
			}

			try {

				app.delete(modifiedPage);

			} catch (FrameworkException ex) {

				ex.printStackTrace();
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
