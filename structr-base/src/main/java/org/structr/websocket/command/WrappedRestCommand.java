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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.StaticValue;
import org.structr.rest.ResourceProvider;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.resource.Resource;
import org.structr.rest.servlet.ResourceHelper;
import org.structr.web.common.HttpServletRequestWrapper;
import org.structr.web.common.UiResourceProvider;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class WrappedRestCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(WrapContentCommand.class.getName());

	static {

		StructrWebSocket.addCommand(WrappedRestCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final String method                  = webSocketData.getNodeDataStringValue("method");

		if (method == null || ! (method.equals("POST") || method.equals("PUT")) ) {

			logger.warn("Method not supported: {}", method);
			getWebSocket().send(MessageBuilder.wrappedRest().code(422).message("Method not supported: " + method).build(), true);

			return;

		}

		ResourceProvider resourceProvider;
		try {

			resourceProvider = UiResourceProvider.class.newInstance();

		} catch (Throwable t) {

			logger.error("Couldn't establish a resource provider", t);
			getWebSocket().send(MessageBuilder.wrappedRest().code(422).message("Couldn't establish a resource provider").build(), true);
			return;

		}

		final Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<>();
		resourceMap.putAll(resourceProvider.getResources());

		final StructrWebSocket socket        = this.getWebSocket();
		final String url                     = webSocketData.getNodeDataStringValue("url");

		// mimic HTTP request
		final HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(socket.getRequest(), url);

		Resource resource;
		final StaticValue fakePropertyView = new StaticValue(PropertyView.Public);
		try {

			resource = ResourceHelper.optimizeNestedResourceChain(socket.getSecurityContext(), wrappedRequest, resourceMap, fakePropertyView);

		} catch (IllegalPathException | NotFoundException e) {

			logger.warn("Illegal path for REST query");
			getWebSocket().send(MessageBuilder.wrappedRest().code(422).message("Illegal path for REST query").build(), true);
			return;

		}

		final String data                    = webSocketData.getNodeDataStringValue("data");
		final Gson gson                      = new GsonBuilder().create();
		final Map<String, Object> jsonData   = gson.fromJson(data, Map.class);

		RestMethodResult result = null;

		switch (method) {
			case "PUT":
				// we want to update data
				result = resource.doPut(jsonData);

				break;

			case "POST":
				// we either want to create data or call a method on an object
				result = resource.doPost(jsonData);


				break;
		}

		// right now we do not send messages
		if (result != null) {
//			getWebSocket().send(MessageBuilder.wrappedRest().code(result.getResponseCode()).message(result.jsonMessage()).build(), true);
		}

	}

	@Override
	public String getCommand() {
		return "WRAPPED_REST";
	}

}
