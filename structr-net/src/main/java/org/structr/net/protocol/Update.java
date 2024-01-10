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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.net.data.time.PseudoTime;
import org.structr.net.peer.Peer;
import org.structr.net.peer.PeerInfo;
import org.structr.net.repository.Repository;
import org.structr.net.repository.RepositoryObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 */
public class Update extends Message {

	private static final Logger logger = LoggerFactory.getLogger(Update.class.getName());

	private final Map<String, Object> data = new HashMap<>();
	private PseudoTime created             = null;
	private PseudoTime lastModified        = null;
	private String objectId                = null;
	private String userId                  = null;
	private String type                    = null;

	public Update() {
		this(null, null, null, null, null, null, null);
	}

	public Update(final String sender, final String objectId, final String type, final String userId, final PseudoTime created, final PseudoTime lastModified, final Map<String, Object> data) {
		super(4, sender);

		this.objectId     = objectId;
		this.userId       = userId;
		this.type         = type;
		this.created      = created;
		this.lastModified = lastModified;

		if (data != null) {
			this.data.putAll(data);
		}
	}

	@Override
	public void onMessage(final Peer peer, final PeerInfo sender) {

		final Repository repository = peer.getRepository();
		final RepositoryObject obj  = repository.getObject(objectId);
		if (obj != null) {

			repository.update(obj, type, objectId, userId, lastModified, data);

		} else {

			repository.objectCreated(objectId, type, getSender(), userId, created, lastModified, data);
		}
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		super.serialize(dos);

		serializeUUID(dos, objectId); // os.writeUTF(objectId);
		dos.writeUTF(userId);
		dos.writeUTF(type);

		created.serialize(dos);
		lastModified.serialize(dos);

		final int size = data.size();
		dos.writeInt(size);

		for (final Entry<String, Object> entry : data.entrySet()) {

			dos.writeUTF(entry.getKey());
			serializeObject(dos, entry.getValue());
		}
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		super.deserialize(dis);

		this.objectId     = deserializeUUID(dis); // dis.readUTF();
		this.userId       = dis.readUTF();
		this.type         = dis.readUTF();
		this.created      = PseudoTime.fromStream(dis);
		this.lastModified = PseudoTime.fromStream(dis);

		final int size = dis.readInt();

		for (int i=0; i<size; i++) {

			try {
				final String key   = dis.readUTF();
				final Object value = deserializeObject(dis);

				data.put(key, value);

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}
	}

	public String getObjectId() {
		return objectId;
	}

	public String getUserId() {
		return userId;
	}

	public PseudoTime getTime() {
		return created;
	}
}
