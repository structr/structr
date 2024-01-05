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
public class Inventory extends Message {

	private PseudoTime lastModificationDate = null;
	private String objectId                 = null;
	private String owner                    = null;

	public Inventory() {
		this(null, null, null, null);
	}

	public Inventory(final String sender, final String objectId, final String owner, final PseudoTime lastModificationDate) {
		super(17, sender);

		this.lastModificationDate = lastModificationDate;
		this.objectId             = objectId;
		this.owner                = owner;
	}

	@Override
	public void onMessage(Peer peer, PeerInfo sender) {

		if (!peer.getUuid().equals(getSender())) {

			final RepositoryObject obj = peer.getRepository().getObject(objectId);
			if (obj == null || obj.getLastModificationTime().before(lastModificationDate)) {

				peer.log("GetHistory(", objectId, ")");

				peer.broadcast(new GetHistory(peer.getUuid(), getSender(), objectId, PseudoTime.now(peer)));
			}
		}
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		super.serialize(dos);

		serializeUUID(dos, objectId); // os.writeUTF(objectId);
		serializeUUID(dos, owner);    // dos.writeUTF(owner);

		lastModificationDate.serialize(dos);
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		super.deserialize(dis);

		this.objectId = deserializeUUID(dis); // dis.readUTF();
		this.owner    = deserializeUUID(dis); // dis.readUTF();

		this.lastModificationDate = new PseudoTime();
		this.lastModificationDate.deserialize(dis);
	}
}
