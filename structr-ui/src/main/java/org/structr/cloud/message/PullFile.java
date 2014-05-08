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

import java.util.List;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudContext;
import org.structr.cloud.CloudService;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.File;

/**
 * Encapsulates a pull request for a node
 *
 *
 * @author Christian Morgner
 */
public class PullFile extends FileNodeDataContainer {

	private String key     = null;
	private int nodeIndex  = 0;

	public PullFile() throws FrameworkException {}

	public PullFile(final String key, final int nodeIndex) throws FrameworkException {

		this.key       = key;
		this.nodeIndex = nodeIndex;
	}

	@Override
	public Message process(CloudConnection connection, final CloudContext context) {

		final Object value = context.getValue(key + "Nodes");
		if (value instanceof List) {

			final List<NodeInterface> nodes = (List<NodeInterface>)value;
			final NodeInterface node        = nodes.get(nodeIndex);
			final File file                 = (File)node;
			this.sourceNodeId               = node.getUuid();
			this.type                       = node.getClass();

			collectProperties(node.getNode());

			context.storeValue(sourceNodeId, getChunks(file, CloudService.CHUNK_SIZE).iterator());

			// return file size
			this.setFileSize(file.getSize());
		}

		// send this back
		return this;
	}

	@Override
	public void postProcess(CloudConnection connection, CloudContext context) {
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
