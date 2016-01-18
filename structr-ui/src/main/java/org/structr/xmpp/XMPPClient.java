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
package org.structr.xmpp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;

/**
 *
 *
 */
public class XMPPClient extends AbstractNode implements XMPPInfo {

	private static final Logger logger = Logger.getLogger(XMPPClient.class.getName());

	public static final Property<List<XMPPRequest>> pendingRequests = new EndNodes<>("pendingRequests", XMPPClientRequest.class);
	public static final Property<String>            xmppHandle      = new FunctionProperty("xmppHandle").format("concat(this.xmppUsername, '@', this.xmppHost)").indexed();
	public static final Property<String>            xmppUsername    = new StringProperty("xmppUsername").indexed();
	public static final Property<String>            xmppPassword    = new StringProperty("xmppPassword");
	public static final Property<String>            xmppService     = new StringProperty("xmppService");
	public static final Property<String>            xmppHost        = new StringProperty("xmppHost");
	public static final Property<Integer>           xmppPort        = new IntProperty("xmppPort");
	public static final Property<Mode>              presenceMode    = new EnumProperty("presenceMode", Mode.class, Mode.available);
	public static final Property<Boolean>           isEnabled       = new BooleanProperty("isEnabled");
	public static final Property<Boolean>           isConnected     = new BooleanProperty("isConnected");

	static {

		SchemaService.registerBuiltinTypeOverride("XMPPClient", XMPPClient.class.getName());
	}

	public static final View publicView = new View(XMPPClient.class, PropertyView.Public,
		xmppHandle, xmppUsername, xmppPassword, xmppService, xmppHost, xmppPort, presenceMode, isEnabled, isConnected, pendingRequests
	);

	public static final View uiView = new View(XMPPClient.class, PropertyView.Ui,
		xmppHandle, xmppUsername, xmppPassword, xmppService, xmppHost, xmppPort, presenceMode, isEnabled, isConnected, pendingRequests
	);

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (getProperty(isEnabled)) {
			XMPPContext.connect(this);
		}

		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
		boolean enabled                 = getProperty(isEnabled);
		if (!enabled) {

			if (connection != null && connection.isConnected()) {
				connection.disconnect();
			}

		} else {

			if (connection == null || !connection.isConnected()) {
				XMPPContext.connect(this);
			}

			connection = XMPPContext.getClientForId(getUuid());
			if (connection != null) {

				if (connection.isConnected()) {

					setProperty(isConnected, true);
					connection.setPresence(getProperty(presenceMode));

				} else {

					setProperty(isConnected, false);
				}
			}
		}

		return super.onModification(securityContext, errorBuffer);
	}

	@Override
	public boolean onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		final String uuid = properties.get(id);
		if (uuid != null) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(uuid);
			if (connection != null) {

				connection.disconnect();
			}
		}

		return super.onDeletion(securityContext, errorBuffer, properties);
	}

	@Override
	public String getUsername() {
		return getProperty(xmppUsername);
	}

	@Override
	public String getPassword() {
		return getProperty(xmppPassword);
	}

	@Override
	public String getService() {
		return getProperty(xmppService);
	}

	@Override
	public String getHostName() {
		return getProperty(xmppHost);
	}

	@Override
	public int getPort() {
		return getProperty(xmppPort);
	}

	@Export
	public RestMethodResult doSendMessage(final String recipient, final String message) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
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

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
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

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
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

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
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

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
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

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
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

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
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

			final XMPPClient client = StructrApp.getInstance().get(XMPPClient.class, uuid);
			if (client != null) {

				final String callbackName            = "onXMPP" + message.getClass().getSimpleName();
				final Map<String, Object> properties = new HashMap<>();

				properties.put("sender", message.getFrom());
				properties.put("message", message.getBody());

				client.invokeMethod(callbackName, properties, false);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	public static void onRequest(final String uuid, final IQ request) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final XMPPClient client = StructrApp.getInstance().get(XMPPClient.class, uuid);
			if (client != null) {

				app.create(XMPPRequest.class,
					new NodeAttribute(XMPPRequest.client, client),
					new NodeAttribute(XMPPRequest.sender, request.getFrom()),
					new NodeAttribute(XMPPRequest.owner, client.getProperty(XMPPClient.owner)),
					new NodeAttribute(XMPPRequest.content, request.toXML().toString()),
					new NodeAttribute(XMPPRequest.requestType, request.getType())
				);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}
}
