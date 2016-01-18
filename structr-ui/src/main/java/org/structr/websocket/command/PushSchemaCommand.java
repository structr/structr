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

import java.util.Map;
import org.structr.cloud.CloudService;
import org.structr.cloud.HostInfo;
import org.structr.cloud.WebsocketProgressListener;
import org.structr.cloud.transmission.PushTransmission;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.Tx;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class PushSchemaCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(PushSchemaCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final Map<String, Object> properties = webSocketData.getNodeData();
		final String username                = (String)properties.get("username");
		final String password                = (String)properties.get("password");
		final String host                    = (String)properties.get("host");
		final Long port                      = (Long)properties.get("port");
		final String key                     = (String)properties.get("key");

		if (host != null && port != null && username != null && password != null && key != null) {

			final App app    = StructrApp.getInstance();
			try (final Tx tx = app.tx()) {

				// create push transmission to be filled later
				final PushTransmission tms = new PushTransmission();

				for (final SchemaNode node : app.nodeQuery(SchemaNode.class).getAsList()) {

					if (isFalseOrNull(node.getProperty(SchemaNode.isBuiltinType))) {
						tms.getExportSet().add(node);
					}
				}

				for (final SchemaRelationshipNode rel : app.nodeQuery(SchemaRelationshipNode.class).getAsList()) {
					tms.getExportSet().add(rel);
				}

				// push schema
				CloudService.doRemote(
					webSocket.getSecurityContext(),
					tms,
					new HostInfo(username, password, host, port.intValue()),
					new WebsocketProgressListener(getWebSocket(), key)
				);

				tx.success();

			} catch (FrameworkException fex) {

				getWebSocket().send(MessageBuilder.status().code(400).message(fex.getMessage()).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("The PUSH_SCHEMA command needs username, password, host, port and key!").build(), true);
		}

	}

	public boolean isFalseOrNull(final Boolean value) {
		return value == null || !value;
	}


	@Override
	public String getCommand() {
		return "PUSH_SCHEMA";
	}
}

