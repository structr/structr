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
package org.structr.cloud.sync;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.NotFoundException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudService;
import org.structr.cloud.CloudTransmission;
import org.structr.cloud.message.Delete;
import org.structr.cloud.message.FileNodeChunk;
import org.structr.cloud.message.FileNodeDataContainer;
import org.structr.cloud.message.FileNodeEndChunk;
import org.structr.cloud.message.NodeDataContainer;
import org.structr.cloud.message.RelationshipDataContainer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.dynamic.File;

/**
 *
 *
 */
public class SyncTransmission implements CloudTransmission {

	private static final Logger logger = Logger.getLogger(SyncTransmission.class.getName());
	private List<ModificationEvent> transaction = null;

	public SyncTransmission(final List<ModificationEvent> transaction) {

		this.transaction = transaction;
	}

	@Override
	public Boolean doRemote(final CloudConnection client) throws IOException, FrameworkException {

		int count = 0;

		try (final Tx tx = StructrApp.getInstance().tx()) {

			for (final ModificationEvent event : transaction) {

				final GraphObject graphObject  = event.getGraphObject();

				if (event.isDeleted()) {

					final String id = event.getRemovedProperties().get(GraphObject.id);
					if (id != null) {

						client.send(new Delete(id));
					}

				} else {

					try {

						final Set<String> propertyKeys = new LinkedHashSet<>();

						// collect all possibly modified property keys
						mapPropertyKeysToStrings(propertyKeys, event.getNewProperties().keySet());
						mapPropertyKeysToStrings(propertyKeys, event.getModifiedProperties().keySet());
						mapPropertyKeysToStrings(propertyKeys, event.getRemovedProperties().keySet());

						if (graphObject.isNode()) {

							if (graphObject instanceof File) {

								sendFile(client, (File)graphObject, CloudService.CHUNK_SIZE);

							} else {

								client.send(new NodeDataContainer(graphObject.getSyncNode(), count, propertyKeys));
							}

						} else {

							client.send(new RelationshipDataContainer(graphObject.getSyncRelationship(), count, propertyKeys));
						}

					} catch (NotFoundException nfex) {

						logger.log(Level.INFO, "Trying to synchronize deleted entity, ignoring");
					}
				}

				count++;
			}

			tx.success();
		}

		// synchronize last sync timestamp with slave instance
		// (we're sending out own instance ID (master) for the slave to store)
		final String masterId = StructrApp.getInstance().getInstanceId();
		client.send(new ReplicationStatus(masterId, StructrApp.getInstance().getGlobalSetting(masterId + ".lastModified", 0L)));

		// wait for end of transmission
		client.waitForTransmission();

		return true;
	}

	/**
	 * Splits the given file and sends it over the client connection. This method first creates a <code>FileNodeDataContainer</code> and sends it to the remote end. The file from disk is then
	 * split into multiple instances of <code>FileChunkContainer</code> while being sent. To finalize the transfer, a <code>FileNodeEndChunk</code> is sent to notify the receiving end of the
	 * successful transfer.
	 *
	 * @param client the client to send over
	 * @param file the file to split and send
	 * @param chunkSize the chunk size for a single chunk
	 * @throws org.structr.common.error.FrameworkException
	 * @throws java.io.IOException
	 */
	public static void sendFile(final CloudConnection client, final File file, final int chunkSize) throws FrameworkException, IOException {

		// send file container first
		FileNodeDataContainer container = new FileNodeDataContainer(file);
		client.send(container);

		// send chunks
		for (FileNodeChunk chunk : FileNodeDataContainer.getChunks(file, chunkSize)) {
			client.send(chunk);
		}

		// mark end of file with special chunk
		client.send(new FileNodeEndChunk(container.getSourceNodeId(), container.getFileSize()));
	}

	// ----- private methods -----
	private void mapPropertyKeysToStrings(final Set<String> propertyKeys, final Set<PropertyKey> source) {

		for (final PropertyKey key : source) {
			propertyKeys.add(key.dbName());
		}
	}
}
