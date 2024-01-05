/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.net.common;

import org.structr.net.PeerListener;
import org.structr.net.peer.PeerInfo;
import org.structr.net.protocol.AbstractMessage;

/**
 * A simple Listener implementation that prints to the standard output.
 */
public class PrintListener implements PeerListener {

	@Override
	public void onMessage(AbstractMessage message) {
		System.out.println(message);
	}

	@Override
	public void onAddPeer(PeerInfo peer) {
		System.out.println("ADD:    " + peer);
	}

	@Override
	public void onRemovePeer(PeerInfo peer) {
		System.out.println("REMOVE: " + peer);
	}
}
