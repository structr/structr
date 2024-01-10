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
package org.structr.net.protocol;

import org.structr.net.peer.Peer;
import org.structr.net.peer.PeerInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 */
public class Discovery extends AbstractMessage {

	private boolean isReply  = false;
	private byte[] hash      = null;

	public Discovery() {
		this(null, false);
	}

	public Discovery(final byte[] hash) {
		this(hash, false);
	}

	public Discovery(final byte[] hash, final boolean isReply) {
		super(0);

		this.hash      = hash;
		this.isReply   = isReply;
	}

	@Override
	public void onMessage(final Peer peer, final PeerInfo sender) {
		peer.onPeerDiscovery(sender, hash);
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		dos.writeInt(hash.length);
		dos.write(hash, 0, hash.length);

		dos.writeBoolean(isReply);
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		// read content hash
		final int hashLength = dis.readInt();
		this.hash = new byte[hashLength];
		dis.read(hash, 0, hashLength);

		this.isReply = dis.readBoolean();
	}
}
