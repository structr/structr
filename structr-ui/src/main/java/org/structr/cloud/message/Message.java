/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.SyncCommand;

/**
 *
 * @author Christian Morgner
 */
public abstract class Message<T> {

	private static final Logger logger              = Logger.getLogger(Message.class.getName());
	private static final AtomicLong idGenerator     = new AtomicLong();
	private static final Map<String, Class> typeMap = new LinkedHashMap<>();

	static {

		// initialize type map
		typeMap.put(Ack.class.getSimpleName(),                       Ack.class);
		typeMap.put(AuthenticationRequest.class.getSimpleName(),     AuthenticationRequest.class);
		typeMap.put(AuthenticationResponse.class.getSimpleName(),    AuthenticationResponse.class);
		typeMap.put(Begin.class.getSimpleName(),                     Begin.class);
		typeMap.put(Crypt.class.getSimpleName(),                     Crypt.class);
		typeMap.put(Delete.class.getSimpleName(),                    Delete.class);
		typeMap.put(End.class.getSimpleName(),                       End.class);
		typeMap.put(Error.class.getSimpleName(),                     Error.class);
		typeMap.put(FileNodeChunk.class.getSimpleName(),             FileNodeChunk.class);
		typeMap.put(FileNodeDataContainer.class.getSimpleName(),     FileNodeDataContainer.class);
		typeMap.put(FileNodeEndChunk.class.getSimpleName(),          FileNodeEndChunk.class);
		typeMap.put(Finish.class.getSimpleName(),                    Finish.class);
		typeMap.put(ListSyncables.class.getSimpleName(),             ListSyncables.class);
		typeMap.put(NodeDataContainer.class.getSimpleName(),         NodeDataContainer.class);
		typeMap.put(PullChunk.class.getSimpleName(),                 PullChunk.class);
		typeMap.put(PullFile.class.getSimpleName(),                  PullFile.class);
		typeMap.put(PullNode.class.getSimpleName(),                  PullNode.class);
		typeMap.put(PullNodeRequestContainer.class.getSimpleName(),  PullNodeRequestContainer.class);
		typeMap.put(PullRelationship.class.getSimpleName(),          PullRelationship.class);
		typeMap.put(PushNodeRequestContainer.class.getSimpleName(),  PushNodeRequestContainer.class);
		typeMap.put(RelationshipDataContainer.class.getSimpleName(), RelationshipDataContainer.class);
	}

	protected long id       = 0;
	protected int sendCount = 0;

	public abstract void onRequest(final CloudConnection serverConnection, final ExportContext context) throws IOException, FrameworkException;
	public abstract void onResponse(final CloudConnection clientConnection, final ExportContext context) throws IOException, FrameworkException;
	public abstract void afterSend(final CloudConnection connection);

	protected abstract void deserializeFrom(final DataInputStream inputStream) throws IOException;
	protected abstract void serializeTo(final DataOutputStream outputStream) throws IOException;

	public Message() {
		this.id = idGenerator.incrementAndGet();
	}

	public Message(final long id, final int sendCount) {

		this.id        = id;
		this.sendCount = sendCount;
	}

	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getId() + ")";
	}

	protected Ack ack() {
		return new Ack(this.id, this.sendCount);
	}

	public void serialize(final DataOutputStream outputStream) throws IOException {

		// write type
		final String type = getClass().getSimpleName();
		SyncCommand.serialize(outputStream, type);

		// write ID
		SyncCommand.serialize(outputStream, id);
		SyncCommand.serialize(outputStream, ++sendCount);

		// write attributes
		serializeTo(outputStream);

		outputStream.flush();
	}

	public boolean wasSentFromHere() {
		return sendCount > 1;
	}

	// ----- private methods -----
	private void setId(final long id) {
		this.id = id;
	}

	private void setSendCount(final int sendCount) {
		this.sendCount = sendCount;
	}

	// ----- public static methods -----
	public static Message deserialize(final DataInputStream inputStream) throws IOException {

		// read type
		final String type = (String)SyncCommand.deserialize(inputStream);
		if (type != null) {

			final Class clazz = typeMap.get(type);
			if (clazz != null) {

				try {

					final Message msg = (Message)clazz.newInstance();

					msg.setId((Long)SyncCommand.deserialize(inputStream));
					msg.setSendCount((Integer)SyncCommand.deserialize(inputStream));

					msg.deserializeFrom(inputStream);

					return msg;

				} catch (Throwable t) {
					t.printStackTrace();
				}

			} else {

				logger.log(Level.WARNING, "Invalid CloudService message: unknown type {0}", type);

				throw new EOFException("Invalid type, aborting.");
			}

		} else {

			logger.log(Level.WARNING, "Invalid CloudService message: no type found.");
		}

		return null;
	}
}
