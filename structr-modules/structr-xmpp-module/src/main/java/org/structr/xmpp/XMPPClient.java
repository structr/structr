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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.EvaluationHints;
import org.structr.xmpp.traits.definitions.XMPPClientTraitDefinition;
import org.structr.xmpp.traits.definitions.XMPPRequestTraitDefinition;

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

	// ----- static methods -----
	static void onMessage(final String uuid, final Message message) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final NodeInterface client = StructrApp.getInstance().getNodeById(StructrTraits.XMPP_CLIENT, uuid);
			if (client != null) {

				final String callbackName   = "onXMPP" + message.getClass().getSimpleName();
				final AbstractMethod method = Methods.resolveMethod(client.getTraits(), callbackName);

				if (method != null) {

					final Arguments arguments = new Arguments();

					arguments.add("sender",  message.getFrom());
					arguments.add("message", message.getBody());

					method.execute(SecurityContext.getSuperUserInstance(), client, arguments, new EvaluationHints());
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(XMPPClientTraitDefinition.class);
			logger.warn("", fex);
		}
	}

	static void onRequest(final String uuid, final IQ request) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final NodeInterface client = StructrApp.getInstance().getNodeById(StructrTraits.XMPP_CLIENT, uuid);
			if (client != null) {

				final Traits traits = Traits.of(StructrTraits.XMPP_REQUEST);

				app.create(StructrTraits.XMPP_REQUEST,
					new NodeAttribute(traits.key(XMPPRequestTraitDefinition.CLIENT_PROPERTY),       client),
					new NodeAttribute(traits.key(XMPPRequestTraitDefinition.SENDER_PROPERTY),       request.getFrom()),
					new NodeAttribute(traits.key(NodeInterfaceTraitDefinition.OWNER_PROPERTY),      client.as(AccessControllable.class).getOwnerNode()),
					new NodeAttribute(traits.key(XMPPRequestTraitDefinition.CONTENT_PROPERTY),      request.toXML("").toString()),
					new NodeAttribute(traits.key(XMPPRequestTraitDefinition.REQUEST_TYPE_PROPERTY), request.getType())
				);
			}

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(XMPPClientTraitDefinition.class);
			logger.warn("", fex);
		}
	}
}
