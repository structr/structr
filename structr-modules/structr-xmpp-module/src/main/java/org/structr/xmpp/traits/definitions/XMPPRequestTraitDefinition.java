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
package org.structr.xmpp.traits.definitions;

import org.jivesoftware.smack.packet.IQ.Type;
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
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

	public XMPPRequestTraitDefinition() {
		super("XMPPRequest");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> client = new StartNode("client", "XMPPClientRequest");
		final Property<String> sender        = new StringProperty("sender").indexed();
		final Property<String> content       = new StringProperty("content");
		final Property<String> requestType   = new EnumProperty("requestType", Type.class);

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
				"client", "sender", "content", "requestType"
			),
			PropertyView.Ui,
			newSet(
				"client", "sender", "content", "requestType"
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
