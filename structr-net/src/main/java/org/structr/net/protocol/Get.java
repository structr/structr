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

import org.structr.net.data.time.PseudoTime;
import org.structr.net.peer.Peer;
import org.structr.net.peer.PeerInfo;
import org.structr.net.repository.RepositoryObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 */
public class Get extends Message {

	private String recipient     = null;
	private String transactionId = null;
	private PseudoTime time      = null;
	private String objectId      = null;
	private String key           = null;

	public Get() {
		this(null, null, null, null, null);
	}

	public Get(final String sender, final RepositoryObject object, final PseudoTime time, final String transactionId, final String key) {
		super(7, sender);

		this.recipient     = object != null ? object.getDeviceId() : null;
		this.objectId      = object != null ? object.getUuid() : null;
		this.transactionId = transactionId;
		this.time          = time;
		this.key           = key;
	}

	@Override
	public void onMessage(Peer peer, PeerInfo sender) {

		if (peer.getUuid().equals(recipient)) {

			final RepositoryObject obj = peer.getRepository().getObject(objectId);
			final Object value         = obj.getProperty(time, transactionId, key);

			peer.broadcast(new Value(peer.getUuid(), getSender(), getId(), value));

		} else {

			final RepositoryObject obj = peer.getRepository().getObject(objectId);
			if (obj == null) {

				// object is missing, contact owner and fetch history from beginning of time
				peer.log("Unknown object ", objectId, ", requesting full history..");
				peer.broadcast(new GetHistory(peer.getUuid(), recipient, objectId, PseudoTime.now(peer)));
			}
		}
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		super.serialize(dos);

		serializeUUID(dos, recipient);     // dos.writeUTF(recipient);
		serializeUUID(dos, objectId);      // dos.writeUTF(objectId);
		serializeUUID(dos, transactionId); // dos.writeUTF(transactionId);
		dos.writeUTF(key);

		time.serialize(dos);
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		super.deserialize(dis);

		this.recipient     = deserializeUUID(dis); // dis.readUTF();
		this.objectId      = deserializeUUID(dis); // dis.readUTF();
		this.transactionId = deserializeUUID(dis); // dis.readUTF();
		this.key           = dis.readUTF();
		this.time          = new PseudoTime();

		time.deserialize(dis);
	}

	public String getObjectId() {
		return objectId;
	}

	public PseudoTime getTime() {
		return time;
	}
}
