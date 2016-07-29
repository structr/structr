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
import org.structr.cloud.CloudConnection;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.SyncCommand;

/**
 * Serializable data container for a node to be transported over network.
 *
 * To be initialized with {@link AbstractNode} in constructor.
 *
 *
 */
public class NodeDataContainer extends DataContainer {

	protected String sourceNodeId = null;
	protected String type         = null;

	public NodeDataContainer() {
		super(0);
	}

	public NodeDataContainer(final NodeInterface node, final int sequenceNumber) throws FrameworkException {
		this(node, sequenceNumber, node.getNode().getPropertyKeys());
	}

	public NodeDataContainer(final NodeInterface node, final int sequenceNumber, final Iterable<String> propertyKeys) throws FrameworkException {

		super(sequenceNumber);

		type         = node.getClass().getSimpleName();
		sourceNodeId = node.getUuid();

		collectProperties(node.getNode(), propertyKeys);
	}

	/**
	 * Return id of node in source instance
	 *
	 * @return source node id
	 */
	public String getSourceNodeId() {
		return sourceNodeId;
	}

	public String getType() {
		return type;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {
		serverConnection.storeNode(this);
		sendKeepalive(serverConnection);
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.sourceNodeId = (String)SyncCommand.deserialize(inputStream);
		this.type         = (String)SyncCommand.deserialize(inputStream);

		super.deserializeFrom(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, sourceNodeId);
		SyncCommand.serialize(outputStream, type);

		super.serializeTo(outputStream);
	}
}
