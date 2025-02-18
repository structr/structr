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
package org.structr.xmpp.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.xmpp.XMPPClient;

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
		return wrappedObject.getProperty(traits.key("xmppUsername"));
	}

	@Override
	public String getPassword() {
		return wrappedObject.getProperty(traits.key("xmppPassword"));
	}

	@Override
	public String getService() {
		return wrappedObject.getProperty(traits.key("xmppService"));
	}

	@Override
	public String getHostName() {
		return wrappedObject.getProperty(traits.key("xmppHost"));
	}

	@Override
	public int getPort() {
		return wrappedObject.getProperty(traits.key("xmppPort"));
	}

	@Override
	public void setIsConnected(final boolean isConnected) throws FrameworkException {
		setProperty(traits.key("isConnected"), isConnected);
	}

	@Override
	public String getPresenceMode() {
		return wrappedObject.getProperty(traits.key("presenceMode"));
	}

	@Override
	public boolean getIsConnected() {
		return wrappedObject.getProperty(traits.key("isConnected"));
	}

	@Override
	public boolean getIsEnabled() {
		return wrappedObject.getProperty(traits.key("isEnabled"));
	}
}
