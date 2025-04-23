/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.json.ElkGraphJson;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedList;
import java.util.List;

/**
 * Websocket command to use the Eclipse Layout Kernel to layout an object graph.
 */
public class LayoutCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(LayoutCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final List<GraphObject> result = new LinkedList<>();
			final GraphObjectMap map       = new GraphObjectMap();
			final String input             = webSocketData.getNodeDataStringValue("input");

			try {

				// We cannot use our serialization code to send the output to the client
				// because the output is limited to the outputDepth setting.
				map.put(new StringProperty("json"), applyLayoutAndTransform(input));

			} catch (Throwable t) {

				t.printStackTrace();
				map.put(new StringProperty("error"), t.getMessage());
			}

			result.add(map);

			webSocketData.setResult(result);
			webSocketData.setRawResultCount(result.size());

			// send only over local connection
			getWebSocket().send(webSocketData, true);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public String getCommand() {
		return "LAYOUT";
	}

	// ----- private methods -----
	private String applyLayoutAndTransform(final String input) {

		final RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
		final ElkNode graph                     = ElkGraphJson.forGraph(input).toElk();
		final BasicProgressMonitor monitor      = new BasicProgressMonitor();

		engine.layout(graph, monitor);

		return ElkGraphJson.forGraph(graph).toJson();
	}
}
