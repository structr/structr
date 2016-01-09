/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import org.structr.cloud.sync.ReplicationStatus;
import org.structr.cloud.sync.Synchronize;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudListener;
import org.structr.cloud.sync.Diff;
import org.structr.cloud.sync.EndOfSync;
import org.structr.cloud.sync.Ping;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.SyncCommand;

/**
 *
 *
 */
public abstract class Message<T> {

	private static final Logger logger                   = Logger.getLogger(Message.class.getName());
	private static final AtomicLong idGenerator          = new AtomicLong();
	private static final Map<String, Class> typeMap      = new LinkedHashMap<>();
	private static final Set<String> ignoredPropertyKeys = new HashSet<>();

	static {

		// initialize type map, this is basically the instruction set of the CloudService
		typeMap.put(AuthenticationRequest.class.getSimpleName(),     AuthenticationRequest.class);
		typeMap.put(AuthenticationResponse.class.getSimpleName(),    AuthenticationResponse.class);
		typeMap.put(Begin.class.getSimpleName(),                     Begin.class);
		typeMap.put(Crypt.class.getSimpleName(),                     Crypt.class);
		typeMap.put(Delete.class.getSimpleName(),                    Delete.class);
		typeMap.put(Diff.class.getSimpleName(),                      Diff.class);
		typeMap.put(End.class.getSimpleName(),                       End.class);
		typeMap.put(EndOfSync.class.getSimpleName(),                 EndOfSync.class);
		typeMap.put(Error.class.getSimpleName(),                     Error.class);
		typeMap.put(FileNodeChunk.class.getSimpleName(),             FileNodeChunk.class);
		typeMap.put(FileNodeDataContainer.class.getSimpleName(),     FileNodeDataContainer.class);
		typeMap.put(FileNodeEndChunk.class.getSimpleName(),          FileNodeEndChunk.class);
		typeMap.put(Finish.class.getSimpleName(),                    Finish.class);
		typeMap.put(ListSyncables.class.getSimpleName(),             ListSyncables.class);
		typeMap.put(NodeDataContainer.class.getSimpleName(),         NodeDataContainer.class);
		typeMap.put(Ping.class.getSimpleName(),                      Ping.class);
		typeMap.put(PullChunk.class.getSimpleName(),                 PullChunk.class);
		typeMap.put(PullFile.class.getSimpleName(),                  PullFile.class);
		typeMap.put(PullNode.class.getSimpleName(),                  PullNode.class);
		typeMap.put(PullNodeRequestContainer.class.getSimpleName(),  PullNodeRequestContainer.class);
		typeMap.put(PullRelationship.class.getSimpleName(),          PullRelationship.class);
		typeMap.put(RelationshipDataContainer.class.getSimpleName(), RelationshipDataContainer.class);
		typeMap.put(ReplicationStatus.class.getSimpleName(),         ReplicationStatus.class);
		typeMap.put(Synchronize.class.getSimpleName(),               Synchronize.class);

		// add local property keys that should be ignored
		ignoredPropertyKeys.add(GraphObject.createdDate.dbName());
		ignoredPropertyKeys.add(GraphObject.lastModifiedDate.dbName());
	}

	protected long id       = 0;
	protected int sendCount = 0;

	public abstract void onRequest(final CloudConnection serverConnection) throws IOException, FrameworkException;
	public abstract void onResponse(final CloudConnection clientConnection) throws IOException, FrameworkException;
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

	// ----- protected methods -----
	protected String contentHashCode(final GraphObject graphObject)  {

		if (graphObject != null) {

			if (graphObject.isNode()) {

				return contentHashCode(graphObject.getSyncNode().getNode());

			} else {

				return contentHashCode(graphObject.getSyncRelationship().getRelationship());
			}
		}

		return null;
	}

	protected String contentHashCode(final Node node) {
		return contentHashCode(node, null);
	}

	protected String contentHashCode(final Node node, final Set<Long> visitedObjectIDs) {

		int hashCode = propertyHashCode(node, id, visitedObjectIDs);
		return Integer.toString(hashCode);
	}

	protected String contentHashCode(final Relationship relationship) {
		return contentHashCode(relationship, null);
	}

	protected String contentHashCode(final Relationship relationship, final Set<Long> visitedObjectIDs) {

		int hashCode = propertyHashCode(relationship, id, visitedObjectIDs);

		hashCode ^= getNodeIdHashCode(relationship.getStartNode());
		hashCode ^= getNodeIdHashCode(relationship.getEndNode());

		return Integer.toString(hashCode);
	}

	protected void sendKeepalive(final CloudConnection connection) throws IOException, FrameworkException {

		// send keepalive randomly
		if (Math.random() < 0.05) {

			final String message = "Current batch: " + connection.getCount() + ", total: " + connection.getTotal();

			// send message to remote
			connection.send(new Ping(message));

			// update progress locally
			final CloudListener listener = connection.getListener();
			if (listener != null) {

				listener.transmissionProgress(message);
			}
		}
	}

	// ----- private methods -----
	private int propertyHashCode(final PropertyContainer propertyContainer, final long id, final Set<Long> visitedObjectIDs) {

		// can be null when only single content hashes are to be calculated
		if (visitedObjectIDs != null) {
			visitedObjectIDs.add(id);
		}

		final Set<String> sortedKeys = new TreeSet<>();
		int hashCode                 = 34262;	// fixed initial value

		// sort keys before accessing
		for (final String key : propertyContainer.getPropertyKeys()) {

			// filter unwanted property keys
			if (!ignoredPropertyKeys.contains(key)) {
				sortedKeys.add(key);
			}
		}

		for (final String key : sortedKeys) {

			final Object value = propertyContainer.getProperty(key, null);
			if (value != null) {

				hashCode ^= key.hashCode();
				hashCode ^= value.hashCode();
			}
		}

		return hashCode;
	}

	private int getNodeIdHashCode(final Node node) {

		final Object uuid = node.getProperty("id", "");
		if (uuid != null && uuid instanceof String) {

			final String id = (String)uuid;
			return id.hashCode();
		}

		return -1;
	}

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
