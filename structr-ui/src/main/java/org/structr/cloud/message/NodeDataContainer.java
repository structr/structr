/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.SyncCommand;

/**
 * Serializable data container for a node to be transported over network.
 *
 * To be initialized with {@link AbstractNode} in constructor.
 *
 * @author axel
 */
public class NodeDataContainer extends DataContainer {

	protected String sourceNodeId = null;
	protected String type         = null;

	public NodeDataContainer() {
		super(0);
	}

	public NodeDataContainer(final NodeInterface node, final int sequenceNumber) throws FrameworkException {

		super(sequenceNumber);

		type         = node.getClass().getSimpleName();
		sourceNodeId = node.getUuid();

		collectProperties(node.getNode());
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
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		serverConnection.storeNode(this);
		serverConnection.send(ack());

		context.progress();
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
		context.progress();
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	@Override
	protected void deserializeFrom(Reader reader) throws IOException {

		this.sourceNodeId = (String)SyncCommand.deserialize(reader);
		this.type         = (String)SyncCommand.deserialize(reader);

		super.deserializeFrom(reader);
	}

	@Override
	protected void serializeTo(Writer writer) throws IOException {

		SyncCommand.serialize(writer, sourceNodeId);
		SyncCommand.serialize(writer, type);

		super.serializeTo(writer);
	}
}
