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
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonSchema.Cascade;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.schema.action.EvaluationHints;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;

/**
 *
 *
 */
public interface XMPPClient extends NodeInterface, XMPPInfo {

	static class Impl { static {

		final JsonSchema schema      = SchemaService.getDynamicSchema();
		final JsonObjectType type    = schema.addType("XMPPClient");
		final JsonObjectType request = schema.addType("XMPPRequest");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/XMPPClient"));

		type.addFunctionProperty("xmppHandle", PropertyView.Public, PropertyView.Ui).setTypeHint("String").setFormat("concat(this.xmppUsername, '@', this.xmppHost)").setIndexed(true);
		type.addStringProperty("xmppUsername", PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("xmppPassword", PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("xmppService",  PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("xmppHost",     PropertyView.Public, PropertyView.Ui);
		type.addIntegerProperty("xmppPort",    PropertyView.Public, PropertyView.Ui);
		type.addEnumProperty("presenceMode",   PropertyView.Public, PropertyView.Ui).setEnumType(Mode.class);
		type.addBooleanProperty("isEnabled",   PropertyView.Public, PropertyView.Ui);
		type.addBooleanProperty("isConnected", PropertyView.Public, PropertyView.Ui);

		type.addPropertyGetter("isConnected",  Boolean.TYPE);
		type.addPropertyGetter("isEnabled",    Boolean.TYPE);
		type.addPropertyGetter("presenceMode", Mode.class);

		type.addPropertySetter("isConnected", Boolean.TYPE);

		type.overrideMethod("onCreation",     true, "if (getProperty(isEnabledProperty)) { " + XMPPContext.class.getName() + ".connect(this); }");
		type.overrideMethod("onModification", true, XMPPClient.class.getName() + ".onModification(this, arg0, arg1, arg2);");
		type.overrideMethod("onDeletion",     true, XMPPClient.class.getName() + ".onDeletion(this, arg0, arg1, arg2);");

		type.overrideMethod("getUsername", false, "return getProperty(xmppUsernameProperty);");
		type.overrideMethod("getPassword", false, "return getProperty(xmppPasswordProperty);");
		type.overrideMethod("getService",  false, "return getProperty(xmppServiceProperty);");
		type.overrideMethod("getHostName", false, "return getProperty(xmppHostProperty);");
		type.overrideMethod("getPort",     false, "return getProperty(xmppPortProperty);");

		type.addMethod("doSendMessage")
			.setReturnType(RestMethodResult.class.getName())
			.addParameter("recipient", String.class.getName())
			.addParameter("message", String.class.getName())
			.setSource("return " + XMPPClient.class.getName() + ".doSendMessage(this, recipient, message);")
			.addException(FrameworkException.class.getName());

		type.addMethod("doSubscribe")
			.setReturnType(RestMethodResult.class.getName())
			.addParameter("recipient", String.class.getName())
			.setSource("return " + XMPPClient.class.getName() + ".doSubscribe(this, recipient);")
			.addException(FrameworkException.class.getName());

		type.addMethod("doUnsubscribe")
			.setReturnType(RestMethodResult.class.getName())
			.addParameter("recipient", String.class.getName())
			.setSource("return " + XMPPClient.class.getName() + ".doUnsubscribe(this, recipient);")
			.addException(FrameworkException.class.getName());

		type.addMethod("doConfirmSubscription")
			.setReturnType(RestMethodResult.class.getName())
			.addParameter("recipient", String.class.getName())
			.setSource("return " + XMPPClient.class.getName() + ".doConfirmSubscription(this, recipient);")
			.addException(FrameworkException.class.getName());

		type.addMethod("doDenySubscription")
			.setReturnType(RestMethodResult.class.getName())
			.addParameter("recipient", String.class.getName())
			.setSource("return " + XMPPClient.class.getName() + ".doDenySubscription(this, recipient);")
			.addException(FrameworkException.class.getName());

		type.addMethod("doJoinChat")
			.setReturnType(RestMethodResult.class.getName())
			.addParameter("chatroom", String.class.getName())
			.addParameter("nickname", String.class.getName())
			.addParameter("password", String.class.getName())
			.setSource("return " + XMPPClient.class.getName() + ".doJoinChat(this, chatroom, nickname, password);")
			.addException(FrameworkException.class.getName());

		type.addMethod("doSendChatMessage")
			.setReturnType(RestMethodResult.class.getName())
			.addParameter("chatroom", String.class.getName())
			.addParameter("message", String.class.getName())
			.addParameter("password", String.class.getName())
			.setSource("return " + XMPPClient.class.getName() + ".doSendChatMessage(this, chatroom, message, password);")
			.addException(FrameworkException.class.getName());

		type.relate(request, "PENDING_REQUEST", Cardinality.OneToMany, "client", "pendingRequests").setCascadingDelete(Cascade.sourceToTarget);


		// view configuration
		type.addViewProperty(PropertyView.Public, "pendingRequests");
		type.addViewProperty(PropertyView.Ui,     "pendingRequests");
	}}

	void setIsConnected(final boolean value) throws FrameworkException;
	Mode getPresenceMode();
	boolean getIsConnected();
	boolean getIsEnabled();

	static void onModification(final XMPPClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		XMPPClientConnection connection = XMPPContext.getClientForId(thisClient.getUuid());
		boolean enabled                 = thisClient.getIsEnabled();

		if (!enabled) {

			if (connection != null && connection.isConnected()) {
				connection.disconnect();
			}

		} else {

			if (connection == null || !connection.isConnected()) {
				XMPPContext.connect(thisClient);
			}

			connection = XMPPContext.getClientForId(thisClient.getUuid());
			if (connection != null) {

				if (connection.isConnected()) {

					thisClient.setIsConnected(true);
					connection.setPresence(thisClient.getPresenceMode());

				} else {

					thisClient.setIsConnected(false);
				}
			}
		}
	}

	static void onDeletion(final XMPPClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		final String uuid = properties.get(id);
		if (uuid != null) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(uuid);
			if (connection != null) {

				connection.disconnect();
			}
		}
	}

	static RestMethodResult doSendMessage(final XMPPClient thisClient, final String recipient, final String message) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(thisClient.getUuid());
			if (connection.isConnected()) {

				connection.sendMessage(recipient, message);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	static RestMethodResult doSubscribe(final XMPPClient thisClient, final String recipient) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(thisClient.getUuid());
			if (connection.isConnected()) {

				connection.subscribe(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	static RestMethodResult doUnsubscribe(final XMPPClient thisClient, final String recipient) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(thisClient.getUuid());
			if (connection.isConnected()) {

				connection.unsubscribe(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	static RestMethodResult doConfirmSubscription(final XMPPClient thisClient, final String recipient) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(thisClient.getUuid());
			if (connection.isConnected()) {

				connection.confirmSubscription(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	static RestMethodResult doDenySubscription(final XMPPClient thisClient, final String recipient) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(thisClient.getUuid());
			if (connection.isConnected()) {

				connection.denySubscription(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	static RestMethodResult doJoinChat(final XMPPClient thisClient, final String chatRoom, final String nickname, final String password) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(thisClient.getUuid());
			if (connection.isConnected()) {

				connection.joinChat(chatRoom, nickname, password);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);

	}

	static RestMethodResult doSendChatMessage(final XMPPClient thisClient, final String chatRoom, final String message, final String password) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(thisClient.getUuid());
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

				final AbstractMethod method = Methods.resolveMethod(client.getClass(), client, callbackName);
				if (method != null) {

					method.execute(SecurityContext.getSuperUserInstance(), properties, new EvaluationHints());
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

			final XMPPClient client = StructrApp.getInstance().get(XMPPClient.class, uuid);
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
