package org.structr.cloud.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudService;
import org.structr.cloud.message.Delete;
import org.structr.cloud.message.Message;
import org.structr.cloud.message.NodeDataContainer;
import org.structr.cloud.message.RelationshipDataContainer;
import org.structr.cloud.transmission.PushTransmission;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.SyncCommand;
import org.structr.dynamic.File;

/**
 *
 * @author Christian Morgner
 */
public class Diff extends Message {

	private String uuid = null;
	private String hash = null;

	public Diff() {}

	public Diff(final String uuid, final String contentHashCode) throws FrameworkException {

		this.hash = contentHashCode;
		this.uuid = uuid;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {

		// this message arrives from the replication slave for each entity in the
		// remote database, asking the master to update the corresponding node if
		// necessary.
		final GraphObject entityToBeCompared = StructrApp.getInstance().get(uuid);
		if (entityToBeCompared != null) {

			final String localHash = contentHashCode(entityToBeCompared);
			if (!localHash.equals(hash)) {

				// entity needs to be updated
				if (entityToBeCompared.isNode()) {

					if (entityToBeCompared instanceof File) {

						PushTransmission.sendFile(serverConnection, (File)entityToBeCompared, CloudService.CHUNK_SIZE);

					} else {

						serverConnection.send(new NodeDataContainer(entityToBeCompared.getSyncNode(), 0));

					}

				} else {

					serverConnection.send(new RelationshipDataContainer(entityToBeCompared.getSyncRelationship(), 0));
				}
			}

		} else {

			// remote entity can be deleted, master does not know of this one
			serverConnection.send(new Delete(uuid));
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.uuid = (String)SyncCommand.deserialize(inputStream);
		this.hash = (String)SyncCommand.deserialize(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, uuid);
		SyncCommand.serialize(outputStream, hash);
	}
}
