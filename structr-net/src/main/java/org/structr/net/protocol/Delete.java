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
import org.structr.net.repository.Repository;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 */
public class Delete extends Message {

	private PseudoTime time = null;
	private String objectId = null;

	public Delete() {
		this(null, null, null);
	}

	public Delete(final String sender, final String objectId, final PseudoTime time) {
		super(5, sender);

		this.objectId = objectId;
		this.time     = time;
	}

	@Override
	public void onMessage(final Peer peer, final PeerInfo sender) {

		final Repository repository = peer.getRepository();
		if (repository.contains(objectId)) {

			repository.objectDeleted(objectId, time);
		}
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		super.serialize(dos);

		serializeUUID(dos, objectId); // dos.writeUTF(objectId);

		time.serialize(dos);
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		super.deserialize(dis);

		this.objectId    = deserializeUUID(dis); // dis.readUTF();
		this.time        = new PseudoTime();

		time.deserialize(dis);
	}

	public String getObjectId() {
		return objectId;
	}

	public PseudoTime getTime() {
		return time;
	}
}
