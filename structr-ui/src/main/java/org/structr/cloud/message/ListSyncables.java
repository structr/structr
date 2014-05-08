package org.structr.cloud.message;

import java.util.List;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudContext;
import org.structr.common.SyncState;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class ListSyncables implements Message<List<SyncableInfo>> {

	private List<SyncableInfo> syncables = null;

	public ListSyncables() {}

	@Override
	public Message process(CloudConnection connection, final CloudContext context) {

		try {

			this.syncables = context.listSyncables(SyncState.all());

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return this;
	}

	@Override
	public void postProcess(CloudConnection connection, final CloudContext context) {
	}

	@Override
	public List<SyncableInfo> getPayload() {
		return syncables;
	}
}
