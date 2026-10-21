/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.xmpp.traits.definitions.XMPPClientTraitDefinition;

import java.util.Set;

/**
 *
 *
 */
public interface XMPPClient extends NodeInterface, XMPPInfo {

	String getUsername();
	String getPassword();
	String getService();
	String getHostName();
	int getPort();
	void setIsConnected(final boolean isConnected) throws FrameworkException;
	String getPresenceMode();
	boolean getIsConnected();
	boolean getIsEnabled();

	// the presence types that request or answer a subscription, see onPresence()
	Set<Presence.Type> SUBSCRIPTION_TYPES = Set.of(Presence.Type.subscribe, Presence.Type.subscribed, Presence.Type.unsubscribe, Presence.Type.unsubscribed);

	// ----- static methods -----
	static void onMessage(final String uuid, final Message message) {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final NodeInterface client = StructrApp.getInstance().getNodeById(StructrTraits.XMPP_CLIENT, uuid);
			if (client != null) {

				final String callbackName   = "onXMPP" + message.getClass().getSimpleName();
				final AbstractMethod method = Methods.resolveMethod(client.getTraits(), callbackName);

				if (method != null) {

					final NamedArguments arguments = new NamedArguments();

					// as text, like the sender of onXMPPPresence, so that a script can work with it like with any string
					arguments.add("sender",  message.getFrom() != null ? message.getFrom().toString() : null);
					arguments.add("message", message.getBody());

					method.execute(new ActionContext(SecurityContext.getSuperUserInstance()), client, arguments);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(XMPPClientTraitDefinition.class);
			logger.warn("", fex);
		}
	}

	/**
	 * Passes a subscription request or answer to the onXMPPPresence method of the client, with the bare JID of the sender
	 * and the type of the stanza: subscribe, subscribed, unsubscribe or unsubscribed. The method answers a request with
	 * doConfirmSubscription or doDenySubscription. A client without the method rejects requests, as Smack did before
	 * the requests were passed on, so that they do not stay pending. A request whose method fails stays pending.
	 *
	 * Presences that announce availability are not passed on: every status change of every contact and of every
	 * occupant of a joined room would run a transaction.
	 */
	static void onPresence(final XMPPContext.StructrXMPPConnection connection, final Presence presence) {

		final Presence.Type type = presence.getType();
		if (!SUBSCRIPTION_TYPES.contains(type) || presence.getFrom() == null) {

			return;
		}

		final String sender = presence.getFrom().asBareJid().toString();
		final App app       = StructrApp.getInstance();
		boolean hasMethod   = false;

		try (final Tx tx = app.tx()) {

			final NodeInterface client = app.getNodeById(StructrTraits.XMPP_CLIENT, connection.getUuid());
			if (client != null) {

				final AbstractMethod method = Methods.resolveMethod(client.getTraits(), "onXMPPPresence");
				if (method != null) {

					final NamedArguments arguments = new NamedArguments();

					hasMethod = true;

					arguments.add("sender", sender);
					arguments.add("type",   type.name());

					method.execute(new ActionContext(SecurityContext.getSuperUserInstance()), client, arguments);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(XMPPClientTraitDefinition.class);
			logger.warn("", fex);
		}

		if (!hasMethod && type == Presence.Type.subscribe) {

			try {

				connection.denySubscription(sender);

			} catch (FrameworkException fex) {

				final Logger logger = LoggerFactory.getLogger(XMPPClientTraitDefinition.class);
				logger.warn("Unable to reject the subscription request of {}: {}", sender, fex.getMessage());
			}
		}
	}
}
