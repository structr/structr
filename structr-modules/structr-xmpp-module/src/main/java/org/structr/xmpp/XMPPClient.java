/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.xmpp;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.EvaluationHints;

/**
 *
 *
 */
public class XMPPClient extends AbstractNode implements XMPPInfo {

	public static final Property<Iterable<XMPPRequest>> pendingRequestsProperty = new EndNodes<>("pendingRequests", XMPPClientRequest.class);
	public static final Property<String> xmppHandleProperty                     = new FunctionProperty("xmppHandle").readFunction("concat(this.xmppUsername, '@', this.xmppHost)").typeHint("String").indexed();
	public static final Property<String> xmppUsernameProperty                   = new StringProperty("xmppUsername").indexed();
	public static final Property<String> xmppPasswordProperty                   = new StringProperty("xmppPassword");
	public static final Property<String> xmppServiceProperty                    = new StringProperty("xmppService");
	public static final Property<String> xmppHostProperty                       = new StringProperty("xmppHost");
	public static final Property<Integer> xmppPortProperty                      = new IntProperty("xmppPort");
	public static final Property<Mode> presenceModeProperty                     = new EnumProperty("presenceMode", Mode.class);
	public static final Property<Boolean> isEnabledProperty                     = new BooleanProperty("isEnabled");
	public static final Property<Boolean> isConnectedProperty                   = new BooleanProperty("isConnected");

	public static final View defaultView = new View(XMPPClient.class, PropertyView.Public,
		xmppHandleProperty, xmppUsernameProperty, xmppPasswordProperty, xmppServiceProperty, xmppHostProperty, xmppPortProperty,
		presenceModeProperty, isEnabledProperty, isConnectedProperty, pendingRequestsProperty
	);

	public static final View uiView = new View(XMPPClient.class, PropertyView.Ui,
		xmppHandleProperty, xmppUsernameProperty, xmppPasswordProperty, xmppServiceProperty, xmppHostProperty, xmppPortProperty,
		presenceModeProperty, isEnabledProperty, isConnectedProperty, pendingRequestsProperty
	);

	public String getUsername() {
		return getProperty(xmppUsernameProperty);
	}

	public String getPassword() {
		return getProperty(xmppPasswordProperty);
	}

	public String getService() {
		return getProperty(xmppServiceProperty);
	}

	public String getHostName() {
		return getProperty(xmppHostProperty);
	}

	public int getPort() {
		return getProperty(xmppPortProperty);
	}

	public void setIsConnected(final boolean isConnected) throws FrameworkException {
		setProperty(isConnectedProperty, isConnected);
	}

	public Mode getPresenceMode() {
		return getProperty(presenceModeProperty);
	}

	public boolean getIsConnected() {
		return getProperty(isConnectedProperty);
	}

	public boolean getIsEnabled() {
		return getProperty(isEnabledProperty);
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		if (getIsEnabled()) {
			XMPPContext.connect(this);
		}
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		XMPPClientConnection connection = XMPPContext.getClientForId(this.getUuid());
		boolean enabled                 = this.getIsEnabled();

		if (!enabled) {

			if (connection != null && connection.isConnected()) {
				connection.disconnect();
			}

		} else {

			if (connection == null || !connection.isConnected()) {
				XMPPContext.connect(this);
			}

			connection = XMPPContext.getClientForId(this.getUuid());
			if (connection != null) {

				if (connection.isConnected()) {

					this.setIsConnected(true);
					connection.setPresence(this.getPresenceMode());

				} else {

					this.setIsConnected(false);
				}
			}
		}
	}

	@Override
	public void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		super.onDeletion(securityContext, errorBuffer, properties);

		final String uuid = properties.get(id);
		if (uuid != null) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(uuid);
			if (connection != null) {

				connection.disconnect();
			}
		}
	}

	@Export
	public RestMethodResult doSendMessage(final String recipient, final String message) throws FrameworkException {

		if (this.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(this.getUuid());
			if (connection.isConnected()) {

				connection.sendMessage(recipient, message);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doSubscribe(final String recipient) throws FrameworkException {

		if (this.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(this.getUuid());
			if (connection.isConnected()) {

				connection.subscribe(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doUnsubscribe(final String recipient) throws FrameworkException {

		if (this.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(this.getUuid());
			if (connection.isConnected()) {

				connection.unsubscribe(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doConfirmSubscription(final String recipient) throws FrameworkException {

		if (this.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(this.getUuid());
			if (connection.isConnected()) {

				connection.confirmSubscription(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doDenySubscription(final String recipient) throws FrameworkException {

		if (this.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(this.getUuid());
			if (connection.isConnected()) {

				connection.denySubscription(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doJoinChat(final String chatRoom, final String nickname, final String password) throws FrameworkException {

		if (this.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(this.getUuid());
			if (connection.isConnected()) {

				connection.joinChat(chatRoom, nickname, password);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);

	}

	@Export
	public RestMethodResult doSendChatMessage(final String chatRoom, final String message, final String password) throws FrameworkException {

		if (this.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(this.getUuid());
			if (connection.isConnected()) {

				connection.sendChatMessage(chatRoom, message, password);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	// ----- static methods -----
	public static void onMessage(final String uuid, final Message message) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final XMPPClient client = StructrApp.getInstance().getNodeById(XMPPClient.class, uuid);
			if (client != null) {

				final String callbackName   = "onXMPP" + message.getClass().getSimpleName();
				final AbstractMethod method = Methods.resolveMethod(client.getClass(), callbackName);

				if (method != null) {

					final Arguments arguments = new Arguments();

					arguments.add("sender",  message.getFrom());
					arguments.add("message", message.getBody());

					method.execute(SecurityContext.getSuperUserInstance(), client, arguments, new EvaluationHints());
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(XMPPClient.class);
			logger.warn("", fex);
		}
	}

	public static void onRequest(final String uuid, final IQ request) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final XMPPClient client = StructrApp.getInstance().getNodeById(XMPPClient.class, uuid);
			if (client != null) {

				app.create(XMPPRequest.class,
					new NodeAttribute(StructrApp.key(XMPPRequest.class, "client"),      client),
					new NodeAttribute(StructrApp.key(XMPPRequest.class, "sender"),      request.getFrom()),
					new NodeAttribute(StructrApp.key(XMPPRequest.class, "owner"),       client.getProperty(XMPPClient.owner)),
					new NodeAttribute(StructrApp.key(XMPPRequest.class, "content"),     request.toXML("").toString()),
					new NodeAttribute(StructrApp.key(XMPPRequest.class, "requestType"), request.getType())
				);
			}

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(XMPPClient.class);
			logger.warn("", fex);
		}
	}
}
