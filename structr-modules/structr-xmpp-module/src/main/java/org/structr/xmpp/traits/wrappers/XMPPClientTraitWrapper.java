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
package org.structr.xmpp.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.xmpp.XMPPClient;
import org.structr.xmpp.traits.definitions.XMPPClientTraitDefinition;

/**
 *
 *
 */
public class XMPPClientTraitWrapper extends AbstractNodeTraitWrapper implements XMPPClient {

	public XMPPClientTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getUsername() {
		return wrappedObject.getProperty(traits.key(XMPPClientTraitDefinition.XMPP_USERNAME_PROPERTY));
	}

	@Override
	public String getPassword() {
		return wrappedObject.getProperty(traits.key(XMPPClientTraitDefinition.XMPP_PASSWORD_PROPERTY));
	}

	@Override
	public String getService() {
		return wrappedObject.getProperty(traits.key(XMPPClientTraitDefinition.XMPP_SERVICE_PROPERTY));
	}

	@Override
	public String getHostName() {
		return wrappedObject.getProperty(traits.key(XMPPClientTraitDefinition.XMPP_HOST_PROPERTY));
	}

	@Override
	public int getPort() {
		return wrappedObject.getProperty(traits.key(XMPPClientTraitDefinition.XMPP_PORT_PROPERTY));
	}

	@Override
	public void setIsConnected(final boolean isConnected) throws FrameworkException {
		setProperty(traits.key(XMPPClientTraitDefinition.IS_CONNECTED_PROPERTY), isConnected);
	}

	@Override
	public String getPresenceMode() {
		return wrappedObject.getProperty(traits.key(XMPPClientTraitDefinition.PRESENCE_MODE_PROPERTY));
	}

	@Override
	public boolean getIsConnected() {
		return wrappedObject.getProperty(traits.key(XMPPClientTraitDefinition.IS_CONNECTED_PROPERTY));
	}

	@Override
	public boolean getIsEnabled() {
		return wrappedObject.getProperty(traits.key(XMPPClientTraitDefinition.IS_ENABLED_PROPERTY));
	}
}
