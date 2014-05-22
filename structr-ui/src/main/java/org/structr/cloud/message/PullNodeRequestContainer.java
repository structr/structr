/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.cloud.ExportSet;
import org.structr.common.Syncable;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;

/**
 * Encapsulates a pull request for a node
 *
 *
 * @author Christian Morgner
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
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		try {
			final App app = serverConnection.getApplicationContext();

			// try node first, then relationship
			Syncable syncable = (Syncable)app.nodeQuery().and(GraphObject.id, rootNodeId).includeDeletedAndHidden().getFirst();
			if (syncable == null) {

				syncable = (Syncable)app.relationshipQuery().and(GraphObject.id, rootNodeId).includeDeletedAndHidden().getFirst();
			}

			if (syncable != null) {

				final ExportSet exportSet = ExportSet.getInstance(syncable, recursive);

				// collect export set
				numNodes  = exportSet.getNodes().size();
				numRels   = exportSet.getRelationships().size();
				key       = UUID.randomUUID().toString();

				serverConnection.storeValue(key + "Nodes", new ArrayList<>(exportSet.getNodes()));
				serverConnection.storeValue(key + "Rels",  new ArrayList<>(exportSet.getRelationships()));

				// send this back
				serverConnection.send(this);

				context.progress();
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {

		clientConnection.increaseTotal(numNodes + numRels);

		for (int i=0; i<numNodes; i++) {
			clientConnection.send(new PullNode(key, i));
		}

		for (int i=0; i<numRels; i++) {
			clientConnection.send(new PullRelationship(key, i));
		}

		clientConnection.send(new Finish());

		// important, signal progress AFTER increasing the total size of this
		// transaction, otherwise the thread just finishes because the goal
		// (current == total) is met.
		context.progress();
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	@Override
	public Object getPayload() {
		return null;
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
}
