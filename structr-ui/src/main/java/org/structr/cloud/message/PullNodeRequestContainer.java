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

import java.util.ArrayList;
import java.util.UUID;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportSet;
import org.structr.cloud.CloudContext;
import org.structr.common.SyncState;
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
public class PullNodeRequestContainer implements Message {

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
	public Message process(CloudConnection connection, final CloudContext context) {

		try {
			final App app = context.getApplicationContext();

			// try node first, then relationship
			Syncable syncable = (Syncable)app.nodeQuery().and(GraphObject.id, rootNodeId).includeDeletedAndHidden().getFirst();
			if (syncable == null) {

				syncable = (Syncable)app.relationshipQuery().and(GraphObject.id, rootNodeId).includeDeletedAndHidden().getFirst();
			}

			if (syncable != null) {

				final ExportSet exportSet = ExportSet.getInstance(syncable, SyncState.all(), recursive);

				// collect export set
				numNodes  = exportSet.getNodes().size();
				numRels   = exportSet.getRelationships().size();
				key       = UUID.randomUUID().toString();

				context.storeValue(key + "Nodes", new ArrayList<>(exportSet.getNodes()));
				context.storeValue(key + "Rels",  new ArrayList<>(exportSet.getRelationships()));

				// send this back
				return this;
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;
	}

	@Override
	public void postProcess(CloudConnection connection, CloudContext context) {
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
