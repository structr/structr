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

import org.jivesoftware.smack.packet.IQ.Type;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;

/**
 *
 *
 */
public class XMPPRequest extends AbstractNode {

	public static final Property<XMPPClient> client = new StartNode<>("client", XMPPClientRequest.class);
	public static final Property<String> sender     = new StringProperty("sender").indexed();
	public static final Property<String> content    = new StringProperty("content");
	public static final Property<Type> requestType  = new EnumProperty("requestType", Type.class);

	public static final View publicView = new View(XMPPRequest.class, PropertyView.Public,
		client, sender, content, requestType
	);

	public static final View uiView = new View(XMPPRequest.class, PropertyView.Ui,
		client, sender, content, requestType
	);
}
