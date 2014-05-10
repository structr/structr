package org.structr.cloud.message;

import java.io.IOException;
import java.util.List;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.SyncState;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class ListSyncables extends Message<List<SyncableInfo>> {

	private List<SyncableInfo> syncables = null;

	public ListSyncables() {}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		this.syncables = serverConnection.listSyncables(SyncState.all());
		serverConnection.send(this);
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {

		context.progress();
		clientConnection.setPayload(syncables);
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	public List<SyncableInfo> getPayload() {
		return syncables;
	}
}
