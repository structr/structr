/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.deploy.DeploymentCommentHandler;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

public class ImportCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ImportCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ImportCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final Map<String, Object> properties  = webSocketData.getNodeData();
		final String code                     = (String) properties.get("code");
		final String address                  = (String) properties.get("address");
		final String name                     = (String) properties.get("name");
		final boolean publicVisible           = (Boolean) properties.get("publicVisible");
		final boolean authVisible             = (Boolean) properties.get("authVisible");
		final boolean processDeploymentInfo   = (Boolean) properties.get("processDeploymentInfo");

		try {

			final Importer pageImporter = new Importer(securityContext, code, address, name, publicVisible, authVisible);

			if (processDeploymentInfo) {

				pageImporter.setIsDeployment(true);
				pageImporter.setCommentHandler(new DeploymentCommentHandler());
			}

			final boolean parseOk       = pageImporter.parse();

			if (parseOk) {

				if (address != null) {
					logger.info("Successfully parsed {}", address);
					getWebSocket().send(MessageBuilder.status().code(200).message("Successfully parsed address " + address).build(), true);
				}

				String pageId                  = pageImporter.readPage().getUuid();
				Map<String, Object> resultData = new HashMap();

				if (pageId != null) {

					resultData.put("id", pageId);
					getWebSocket().send(MessageBuilder.status().code(200).message("Successfully created page " + name).data(resultData).build(), true);

				} else {

					getWebSocket().send(MessageBuilder.status().code(400).message("Error while creating page " + name).data(resultData).build(), true);
				}
			}

		} catch (FrameworkException fex) {

			logger.warn("Error while importing content", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}

	}

	@Override
	public String getCommand() {
		return "IMPORT";
	}
}
