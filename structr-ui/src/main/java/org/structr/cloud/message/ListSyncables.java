package org.structr.cloud.message;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.Syncable;
import org.structr.common.error.FrameworkException;
import org.structr.schema.SchemaHelper;

/**
 *
 * @author Christian Morgner
 */
public class ListSyncables extends Message<List<SyncableInfo>> {

	private List<SyncableInfo> syncables = null;
	private String type = null;

	public ListSyncables(final String type) {
		this.type = type;
	}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		final String[] rawTypes = StringUtils.split(type, ",");

		final Set<Class<Syncable>> types = new HashSet();

		if (type != null) {

			for (final String rawType : rawTypes) {

				Class entityClass = SchemaHelper.getEntityClassForRawType(rawType);

				if (entityClass != null && Syncable.class.isAssignableFrom(entityClass)) {

					types.add(entityClass);

				}

			}
		}

		this.syncables = serverConnection.listSyncables(types);
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
