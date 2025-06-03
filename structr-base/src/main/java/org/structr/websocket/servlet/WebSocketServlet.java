/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.websocket.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.structr.api.config.Settings;
import org.structr.core.graph.TransactionCommand;
import org.structr.rest.common.StatsCallback;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.StructrWebSocketCreator;
import org.structr.websocket.WebSocketDataGSONAdapter;
import org.structr.websocket.WebsocketController;
import org.structr.websocket.message.WebSocketMessage;

import java.time.Duration;

/**
 *
 */
public class WebSocketServlet extends JettyWebSocketServlet implements HttpServiceServlet {

	private static final int MAX_TEXT_MESSAGE_SIZE = 1024 * 1024;

	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();
	protected StatsCallback stats                 = null;

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public String getModuleName() {
		return "ui";
	}

	@Override
	public void configure(final JettyWebSocketServletFactory factory) {

		// create GSON serializer
		final GsonBuilder gsonBuilder = new GsonBuilder()
			.serializeNulls()
			.registerTypeAdapter(WebSocketMessage.class, new WebSocketDataGSONAdapter(config.getOutputNestingDepth()));

		if (Settings.WsIndentation.getValue()) {
			gsonBuilder.setPrettyPrinting();
		}

		final boolean lenient = Settings.JsonLenient.getValue();
		if (lenient) {
			// Serializes NaN, -Infinity, Infinity, see http://code.google.com/p/google-gson/issues/detail?id=378
			gsonBuilder.serializeSpecialFloatingPointValues();

		}

		final Gson gson = gsonBuilder.create();

		final WebsocketController syncController = new WebsocketController(gson);

		// register (Structr) transaction listener
		TransactionCommand.registerTransactionListener(syncController);

		factory.setIdleTimeout(Duration.ofSeconds(60));
		factory.setCreator(new StructrWebSocketCreator(syncController, gson, config.getAuthenticator()));
		factory.register(StructrWebSocket.class);

		// Disable compression (experimental features)
		// TODO: Check if these are available in Jetty 10
		//factory.getExtensionFactory().unregister("x-webkit-deflate-frame");
		//factory.getExtensionFactory().unregister("permessage-deflate");

		factory.setMaxTextMessageSize(MAX_TEXT_MESSAGE_SIZE);


	}

	@Override
	public void registerStatsCallback(final StatsCallback stats) {
		this.stats = stats;
	}
}
