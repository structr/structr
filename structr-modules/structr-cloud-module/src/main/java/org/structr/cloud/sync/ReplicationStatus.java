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
 *
 */
public class ReplicationStatus extends Message<ReplicationStatus> {

	private String masterId = null;
	private String slaveId  = null;
	private String role     = null;
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

	public String getRole() {
		return role;
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
			this.role     = StructrApp.getConfigurationValue("sync.role", "slave");
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
		this.role     = (String)SyncCommand.deserialize(inputStream);
		this.lastSync = (Long)SyncCommand.deserialize(inputStream);
		this.update   = (Boolean)SyncCommand.deserialize(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, masterId);
		SyncCommand.serialize(outputStream, slaveId);
		SyncCommand.serialize(outputStream, role);
		SyncCommand.serialize(outputStream, lastSync);
		SyncCommand.serialize(outputStream, update);
	}
}
