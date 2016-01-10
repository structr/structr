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
import java.util.ArrayList;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportSet;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.SyncCommand;

/**
 * Encapsulates a pull request for a node
 *
 *
 *
 */
public class PullNodeRequestContainer extends Message {

	private boolean recursive             = false;
	private String rootNodeId             = null;
	private String key                    = null;
	private int numNodes                  = 0;
	private int numRels                   = 0;

	public PullNodeRequestContainer() {}

	public PullNodeRequestContainer(final String rootNodeId, boolean recursive) {

		this.rootNodeId = rootNodeId;
		this.recursive = recursive;
	}

	public void setSourceNodeId(String rootNodeId) {
		this.rootNodeId = rootNodeId;
	}

	public String getRootNodeId() {
		return rootNodeId;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {

		try {
			final App app = serverConnection.getApplicationContext();

			// try node first, then relationship
			GraphObject syncable = app.nodeQuery().and(GraphObject.id, rootNodeId).includeDeletedAndHidden().getFirst();
			if (syncable == null) {

				syncable = app.relationshipQuery().and(GraphObject.id, rootNodeId).includeDeletedAndHidden().getFirst();
			}

			if (syncable != null) {

				final ExportSet exportSet = ExportSet.getInstance(syncable, recursive);

				// collect export set
				numNodes  = exportSet.getNodes().size();
				numRels   = exportSet.getRelationships().size();
				key       = NodeServiceCommand.getNextUuid();

				serverConnection.storeValue(key + "Nodes", new ArrayList<>(exportSet.getNodes()));
				serverConnection.storeValue(key + "Rels",  new ArrayList<>(exportSet.getRelationships()));

				// send this back
				serverConnection.send(this);
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {

		for (int i=0; i<numNodes; i++) {
			clientConnection.send(new PullNode(key, i));
		}

		for (int i=0; i<numRels; i++) {
			clientConnection.send(new PullRelationship(key, i));
		}

		clientConnection.send(new Finish());
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	public int getNumNodes() {
		return numNodes;
	}

	public int getNumRels() {
		return numRels;
	}

	public String getKey() {
		return key;
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.recursive  = (Boolean)SyncCommand.deserialize(inputStream);
		this.rootNodeId = (String)SyncCommand.deserialize(inputStream);
		this.key        = (String)SyncCommand.deserialize(inputStream);
		this.numNodes   = (Integer)SyncCommand.deserialize(inputStream);
		this.numRels    = (Integer)SyncCommand.deserialize(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, recursive);
		SyncCommand.serialize(outputStream, rootNodeId);
		SyncCommand.serialize(outputStream, key);
		SyncCommand.serialize(outputStream, numNodes);
		SyncCommand.serialize(outputStream, numRels);
	}
}
