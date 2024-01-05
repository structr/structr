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
import java.util.Map;

/**
 *
 */
public class GetHistory extends Message {

	private PseudoTime instant = null;
	private String recipient   = null;
	private String objectId    = null;

	public GetHistory() {
		this(null, null, null, null);
	}

	public GetHistory(final String sender, final String recipient, final String objectId, final PseudoTime instant) {
		super(15, sender);

		this.instant   = instant;
		this.recipient = recipient;
		this.objectId  = objectId;
	}

	@Override
	public void onMessage(Peer peer, PeerInfo sender) {

		if (peer.getUuid().equals(recipient)) {

			final RepositoryObject obj = peer.getRepository().getObject(objectId);
			if (obj != null) {

				final PseudoTime lastModified = obj.getLastModificationTime();
				if (lastModified.before(instant)) {

					final Map<String, Object> data = obj.getProperties(instant);

					peer.log("History(", objectId, ")");

					peer.broadcast(new History(peer.getUuid(), getSender(), objectId, obj.getType(), obj.getUserId(), obj.getCreationTime(), lastModified, data));

				} else {

					peer.log("GetHistory(", objectId, "): not modified");
				}

			} else {

				peer.log("GetHistory(", objectId, "): object not found");
			}
		}
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		super.serialize(dos);

		serializeUUID(dos, recipient); // dos.writeUTF(recipient);
		serializeUUID(dos, objectId);  // dos.writeUTF(objectId);

		instant.serialize(dos);
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		super.deserialize(dis);

		this.recipient = deserializeUUID(dis); // dis.readUTF();
		this.objectId  = deserializeUUID(dis); // dis.readUTF();
		this.instant   = new PseudoTime();

		instant.deserialize(dis);
	}
}
