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
public class BeginTx extends Message {

	private String recipient = null;
	private long timeout     = 0L;

	public BeginTx() {
		this(null, null, 0L);
	}

	public BeginTx(final String sender, final String recipient, final long timeout) {
		super(10, sender);

		this.recipient = recipient;
		this.timeout   = timeout;
	}

	@Override
	public void onMessage(Peer peer, PeerInfo sender) {

		if (peer.getUuid().equals(recipient)) {

			final String possibilityId = peer.getRepository().beginTransaction(timeout);
			peer.broadcast(new Ack(peer.getUuid(), getSender(), getId(), possibilityId));
		}
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		super.serialize(dos);

		serializeUUID(dos, recipient); // dos.writeUTF(recipient);
		dos.writeLong(timeout);
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		super.deserialize(dis);

		this.recipient = deserializeUUID(dis); // dis.readUTF();
		this.timeout   = dis.readLong();
	}
}
