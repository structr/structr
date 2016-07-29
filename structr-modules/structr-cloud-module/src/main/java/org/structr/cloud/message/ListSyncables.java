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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.structr.cloud.CloudConnection;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.SyncCommand;
import org.structr.schema.SchemaHelper;

/**
 *
 *
 */
public class ListSyncables extends Message<List<SyncableInfo>> {

	private List<SyncableInfo> syncables = new LinkedList<>();
	private String type = null;

	public ListSyncables() {
	}

	public ListSyncables(final String type) {
		this.type = type;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {

		final String[] rawTypes = StringUtils.split(type, ", ");

		final Set<Class<? extends GraphObject>> types = new HashSet();

		if (type != null) {

			for (final String rawType : rawTypes) {

				Class entityClass = SchemaHelper.getEntityClassForRawType(rawType);
				if (entityClass != null) {

					types.add(entityClass);
				}

			}
		}

		this.syncables = serverConnection.listSyncables(types);
		serverConnection.send(this);
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
		clientConnection.setPayload(syncables);
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.type = (String)SyncCommand.deserialize(inputStream);

		// read number of syncables from stream
		final int num = (Integer)SyncCommand.deserialize(inputStream);

		// read syncables
		for (int i=0; i<num; i++) {

			SyncableInfo info = new SyncableInfo();
			info.deserializeFrom(inputStream);

			syncables.add(info);
		}
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, type);
		SyncCommand.serialize(outputStream, syncables.size());

		for (final SyncableInfo syncable : syncables) {
			syncable.serializeTo(outputStream);
		}
	}
}
