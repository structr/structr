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
public class Ack extends Message {

	private String recipient = null;
	private String messageId = null;
	private String data      = null;

	public Ack() {
		this(null, null, null, null);
	}

	public Ack(final String sender, final String recipient, final String messageId) {
		this(sender, recipient, messageId, null);
	}

	public Ack(final String sender, final String recipient, final String messageId, final String data) {
		super(14, sender);

		this.recipient = recipient;
		this.messageId = messageId;
		this.data      = data;
	}

	@Override
	public void onMessage(Peer peer, PeerInfo sender) {

		if (peer.getUuid().equals(recipient)) {
			peer.callback(messageId, this);
		}
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		super.serialize(dos);

		serializeUUID(dos, recipient); // dos.writeUTF(recipient);
		serializeUUID(dos, messageId); // dos.writeUTF(messageId);

		dos.writeBoolean(data != null);
		if (data != null) {

			dos.writeUTF(data);
		}
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		super.deserialize(dis);

		this.recipient = deserializeUUID(dis); // dis.readUTF();
		this.messageId = deserializeUUID(dis); // dis.readUTF();

		if (dis.readBoolean()) {

			this.data = dis.readUTF();
		}
	}

	public String getData() {
		return data;
	}
}
