package org.structr.cloud.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.message.Finish;
import org.structr.cloud.message.Message;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.SyncCommand;

/**
 *
 * @author Christian Morgner
 */
public class ReplicationStatus extends Message<ReplicationStatus> {

	private String masterId = null;
	private String slaveId  = null;
	private long lastSync   = 0L;
	private boolean update  = false;

	public ReplicationStatus() {}

	public ReplicationStatus(final String masterId) {

		// initialize this message to READ the lastSync property of the slave database
		this(masterId, 0L, false);
	}

	public ReplicationStatus(final String masterId, final long lastSync) {

		// initialize this message to WRITE the lastSync property of the slave database
		this(masterId, lastSync, true);
	}

	private ReplicationStatus(final String masterId, final long lastSync, final boolean update) {

		this.masterId = masterId;
		this.lastSync = lastSync;
		this.update   = update;
	}

	public String getSlaveId() {
		return slaveId;
	}

	public long getLastSync() {
		return lastSync;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {

		final App app = StructrApp.getInstance();

		this.slaveId = app.getInstanceId();

		if (update) {

			// this is not an error, we want the sync time for the
			// given MASTER, since a slave can have multiple masters
			app.setGlobalSetting(masterId, lastSync);

		} else {

			// this is not an error, we want the sync time for the
			// given MASTER, since a slave can have multiple masters
			this.lastSync = app.getGlobalSetting(masterId, 0L);
		}

		serverConnection.send(this);
		serverConnection.send(new Finish());
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
		clientConnection.setPayload(this);
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.masterId = (String)SyncCommand.deserialize(inputStream);
		this.slaveId  = (String)SyncCommand.deserialize(inputStream);
		this.lastSync = (Long)SyncCommand.deserialize(inputStream);
		this.update   = (Boolean)SyncCommand.deserialize(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, masterId);
		SyncCommand.serialize(outputStream, slaveId);
		SyncCommand.serialize(outputStream, lastSync);
		SyncCommand.serialize(outputStream, update);
	}
}
