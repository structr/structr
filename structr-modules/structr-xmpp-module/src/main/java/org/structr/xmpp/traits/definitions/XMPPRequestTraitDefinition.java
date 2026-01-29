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
package org.structr.xmpp.traits.definitions;

import org.jivesoftware.smack.packet.IQ.Type;
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.xmpp.XMPPRequest;
import org.structr.xmpp.traits.wrappers.XMPPRequestTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class XMPPRequestTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String CLIENT_PROPERTY       = "client";
	public static final String SENDER_PROPERTY       = "sender";
	public static final String CONTENT_PROPERTY      = "content";
	public static final String REQUEST_TYPE_PROPERTY = "requestType";

	public XMPPRequestTraitDefinition() {
		super(StructrTraits.XMPP_REQUEST);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> client = new StartNode(traitsInstance, CLIENT_PROPERTY, StructrTraits.XMPP_CLIENT_REQUEST);
		final Property<String> sender        = new StringProperty(SENDER_PROPERTY).indexed();
		final Property<String> content       = new StringProperty(CONTENT_PROPERTY);
		final Property<String> requestType   = new EnumProperty(REQUEST_TYPE_PROPERTY, Type.class);

		return newSet(
			client,
			sender,
			content,
			requestType
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				CLIENT_PROPERTY, SENDER_PROPERTY, CONTENT_PROPERTY, REQUEST_TYPE_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				CLIENT_PROPERTY, SENDER_PROPERTY, CONTENT_PROPERTY, REQUEST_TYPE_PROPERTY
			)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			XMPPRequest.class, (traits, node) -> new XMPPRequestTraitWrapper(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
