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
 */
public class Commit extends Message {

	private String recipient   = null;
	private String transactionId = null;

	public Commit() {
		this(null, null, null);
	}

	public Commit(final String sender, final String recipient, final String transactionId) {
		super(12, sender);

		this.recipient     = recipient;
		this.transactionId = transactionId;
	}

	@Override
	public void onMessage(Peer peer, PeerInfo sender) {

		if (peer.getUuid().equals(recipient)) {

			peer.getRepository().complete(transactionId);
			peer.broadcast(new Ack(peer.getUuid(), getSender(), getId()));
			peer.broadcast(new Committed(peer.getUuid(), getSender(), transactionId));
		}
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		super.serialize(dos);

		serializeUUID(dos, recipient);     // dos.writeUTF(recipient);
		serializeUUID(dos, transactionId); // dos.writeUTF(possibilityId);
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		super.deserialize(dis);

		this.recipient     = deserializeUUID(dis); // dis.readUTF();
		this.transactionId = deserializeUUID(dis); // dis.readUTF();
	}
}
