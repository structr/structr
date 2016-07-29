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
import java.util.List;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudService;
import org.structr.cloud.transmission.PushTransmission;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.SyncCommand;
import org.structr.dynamic.File;

/**
 * Encapsulates a pull request for a node
 *
 *
 *
 */
public class PullNode extends NodeDataContainer {

	protected String key    = null;
	protected int nodeIndex = 0;

	public PullNode() {}

	public PullNode(final String key, final int nodeIndex) {

		this.key       = key;
		this.nodeIndex = nodeIndex;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {

		final Object value = serverConnection.getValue(key + "Nodes");
		if (value instanceof List) {

			final List<NodeInterface> nodes = (List<NodeInterface>)value;
			final NodeInterface node        = nodes.get(nodeIndex);

			if (node instanceof File) {

				PushTransmission.sendFile(serverConnection, (File)node, CloudService.CHUNK_SIZE);

			} else {

				serverConnection.send(new NodeDataContainer(node, nodeIndex));
			}
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.key       = (String)SyncCommand.deserialize(inputStream);
		this.nodeIndex = (Integer)SyncCommand.deserialize(inputStream);

		super.deserializeFrom(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, key);
		SyncCommand.serialize(outputStream, nodeIndex);

		super.serializeTo(outputStream);
	}
}
