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
package org.structr.websocket;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.ee10.servlet.ServletChannel;
import org.eclipse.jetty.ee10.servlet.ServletContextRequest;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.core.auth.Authenticator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

;

/**
 * Custom creator for structr WebSockets.
 *
 *
 */
public class StructrWebSocketCreator implements WebSocketCreator {

	private static final Logger logger          = LoggerFactory.getLogger(StructrWebSocketCreator.class);
	private static final String STRUCTR_PROTOCOL = "structr";

	private final WebsocketController syncController;
	private final Authenticator authenticator;
	private final Gson gson;

	public StructrWebSocketCreator(final WebsocketController syncController, final Gson gson, final Authenticator authenticator) {

		this.syncController = syncController;
		this.authenticator  = authenticator;
		this.gson           = gson;
	}

	@Override
	public Object createWebSocket(final ServerUpgradeRequest request, final ServerUpgradeResponse response, final Callback callback) throws Exception {

		if (!isOriginAllowed(request)) {

			// Returning null causes Jetty to reject the upgrade. Same pattern
			// as an unsupported sub-protocol below.
			return null;
		}

		if (request.getSubProtocols().contains(STRUCTR_PROTOCOL)) {

			response.setAcceptedSubProtocol(STRUCTR_PROTOCOL);

			StructrWebSocket webSocket = new StructrWebSocket(syncController, gson, authenticator);

			final ServletChannel servletChannel = Request.get(request, ServletContextRequest.class, ServletContextRequest::getServletChannel);
			if (servletChannel != null) {

				servletChannel.associate(request, response, callback);

				final ServletContextRequest contextRequest = servletChannel.getServletContextRequest();
				final ServletApiRequest apiRequest         = contextRequest.getServletApiRequest();

				// The WebSocket outlives this HTTP upgrade request; Jetty recycles apiRequest once the
				// upgrade completes. Snapshot everything we need into a detached copy now, while the
				// source request is still valid, instead of holding on to the live (soon-to-be-recycled)
				// object. This replaces the previous approach of reflectively nulling
				// ServletApiRequest._servletChannel, which required an --add-opens on the Jetty servlet module.
				webSocket.setRequest(new DetachedHttpServletRequest(apiRequest));
			}

			return webSocket;
		}

		return null;
	}

	/**
	 * Rejects cross-origin WebSocket upgrades unless the Origin matches the
	 * request's own authority or an entry in
	 * {@code access.control.accepted.origins}. Missing Origin is permitted so
	 * non-browser clients (curl, custom WS libraries) still work — browsers
	 * always send Origin on WebSocket upgrades, so the CSRF vector we care
	 * about is still blocked.
	 */
	private boolean isOriginAllowed(final ServerUpgradeRequest request) {

		final String origin = request.getHeaders().get("Origin");

		if (StringUtils.isBlank(origin)) {
			return true;
		}

		final URI originUri;
		try {
			originUri = new URI(origin);
		} catch (final URISyntaxException e) {
			logger.warn("WebSocket upgrade rejected: invalid Origin header '{}'", origin);
			return false;
		}

		final HttpURI requestUri = request.getHttpURI();
		if (isSameOrigin(originUri, requestUri)) {
			return true;
		}

		final String acceptedOriginsConfig = Settings.AccessControlAcceptedOrigins.getValue();
		if (StringUtils.isNotBlank(acceptedOriginsConfig)) {

			final List<String> accepted = Arrays.stream(acceptedOriginsConfig.split(","))
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toList());

			if (accepted.contains("*") || accepted.contains(origin)) {
				return true;
			}
		}

		logger.warn("WebSocket upgrade rejected: Origin '{}' not allowed for request to {}", origin, requestUri);
		return false;
	}

	private boolean isSameOrigin(final URI originUri, final HttpURI requestUri) {

		if (originUri.getHost() == null || requestUri.getHost() == null) {
			return false;
		}
		if (!originUri.getHost().equalsIgnoreCase(requestUri.getHost())) {
			return false;
		}

		final int originPort  = effectivePort(originUri.getScheme(), originUri.getPort());
		final int requestPort = effectivePort(requestUri.getScheme(), requestUri.getPort());
		return originPort == requestPort;
	}

	private int effectivePort(final String scheme, final int port) {

		if (port > 0) {
			return port;
		}
		if ("https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme)) {
			return 443;
		}
		return 80;
	}
}
