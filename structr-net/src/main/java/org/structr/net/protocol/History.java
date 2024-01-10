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
import java.util.UUID;

/**
 *
 */
public class History extends Message {

	private static final Logger logger = LoggerFactory.getLogger(History.class.getName());

	private final Map<String, Object> data = new HashMap<>();
	private PseudoTime creationTime        = null;
	private PseudoTime lastModified        = null;
	private String recipient               = null;
	private String objectId                = null;
	private String userId                  = null;
	private String type                    = null;


	public History() {
		this(null, null, null, null, null, null, null, null);
	}

	public History(final String sender, final String recipient, final String objectId, final String type, final String userId, final PseudoTime creationTime, final PseudoTime lastModified, final Map<String, Object> data) {
		super(16, sender);

		this.lastModified = lastModified;
		this.creationTime = creationTime;
		this.recipient    = recipient;
		this.objectId     = objectId;
		this.userId       = userId;
		this.type         = type;

		if (data != null) {
			this.data.putAll(data);
		}
	}

	@Override
	public void onMessage(Peer peer, PeerInfo sender) {

		if (peer.getUuid().equals(recipient)) {

			final Repository repository = peer.getRepository();

			if (!repository.contains(objectId)) {

				repository.objectCreated(objectId, type, getSender(), userId, creationTime, lastModified, data);

			} else {

				// store history
				final RepositoryObject obj = repository.getObject(objectId);
				if (obj != null) {

					final String transactionId = UUID.randomUUID().toString().replaceAll("\\-", "");

					for (final Entry<String, Object> entry : data.entrySet()) {
						obj.setProperty(lastModified, transactionId, entry.getKey(), entry.getValue());
					}

					repository.complete(transactionId);
				}
			}
		}
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		super.serialize(dos);

		serializeUUID(dos, recipient); // dos.writeUTF(recipient);
		serializeUUID(dos, objectId);  // dos.writeUTF(objectId);
		dos.writeUTF(userId);
		dos.writeUTF(type);

		creationTime.serialize(dos);
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

		this.recipient = deserializeUUID(dis); // dis.readUTF();
		this.objectId  = deserializeUUID(dis); // dis.readUTF();
		this.userId    = dis.readUTF();
		this.type      = dis.readUTF();

		this.creationTime = new PseudoTime();
		this.creationTime.deserialize(dis);

		this.lastModified = new PseudoTime();
		this.lastModified.deserialize(dis);

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
}
